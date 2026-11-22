package com.melomarit.melopost.config;

import com.datastax.oss.driver.api.core.CqlSession;
import com.melomarit.melopost.MelopostApplication;
import com.melomarit.melopost.model.DataSource;
import com.melomarit.melopost.model.IncidentNote;
import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.model.User;
import com.melomarit.melopost.repository.DataSourceRepository;
import com.melomarit.melopost.repository.IncidentNoteRepository;
import com.melomarit.melopost.repository.PostmortemRepository;
import com.melomarit.melopost.repository.UserRepository;
import com.melomarit.melopost.support.AbstractCassandraIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Restarting with prod/acc must keep existing data and create no default users. */
class StartupDataSafetyIT extends AbstractCassandraIntegrationTest {

    /** Cassandra keyspace names are limited to 48 characters; keep the unique suffix short. */
    private static String keyspace(String label) {
        String name = "melopost_" + label + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        if (name.length() > 48) {
            throw new IllegalStateException("keyspace name too long: " + name);
        }
        return name;
    }

    private static final String KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    /** Starts with the test key; use the 5-argument form to choose the key. */
    private static ConfigurableApplicationContext start(String keyspace, String profile, String seedFlag, String adminPassword) {
        return start(keyspace, profile, seedFlag, adminPassword, KEY);
    }

    private static ConfigurableApplicationContext start(String keyspace, String profile, String seedFlag,
                                                        String adminPassword, String datasourceKey) {
        return new SpringApplication(MelopostApplication.class).run(
                "--melopost.datasource.key=" + datasourceKey,
                "--spring.profiles.active=" + profile,
                "--spring.cassandra.contact-points=" + cassandraHost(),
                "--spring.cassandra.port=" + cassandraPort(),
                "--spring.cassandra.local-datacenter=datacenter1",
                "--spring.cassandra.keyspace-name=" + keyspace,
                "--server.port=0",
                "--melopost.seed.enabled=" + seedFlag,
                "--melopost.admin.initial-password=" + adminPassword);
    }

    private static void dropKeyspace(String keyspace) {
        try (CqlSession session = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(cassandraHost(), cassandraPort()))
                .withLocalDatacenter("datacenter1").build()) {
            session.execute("DROP KEYSPACE IF EXISTS " + keyspace);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"prod", "acc"})
    void restartWithProdOrAccKeepsDataAndCreatesNoDefaultUsers(String profile) {
        String keyspace = keyspace("restart_" + profile);
        try {
            UUID pmId = UUID.randomUUID();
            UUID dsId = UUID.randomUUID();
            UUID noteId;
            try (ConfigurableApplicationContext first = start(keyspace, "test", "false", "")) {
                Postmortem pm = new Postmortem();
                pm.setUuid(pmId);
                pm.setTitle("Real outage");
                pm.setIncidentRef("INC-42");
                first.getBean(PostmortemRepository.class).save(pm);

                DataSource ds = new DataSource();
                ds.setUuid(dsId);
                ds.setName("Real source");
                ds.setUrl("https://example.invalid/source");
                first.getBean(DataSourceRepository.class).save(ds);

                IncidentNote note = new IncidentNote();
                note.onCreate();
                note.setIncidentRef("INC-42");
                note.setContent("Real note");
                first.getBean(IncidentNoteRepository.class).save(note);
                noteId = note.getUuid();

                assertEquals(0, first.getBean(UserRepository.class).count());
            }

            try (ConfigurableApplicationContext second = start(keyspace, profile, "true", "")) {
                Postmortem pm = second.getBean(PostmortemRepository.class).findById(pmId).orElseThrow();
                assertEquals("Real outage", pm.getTitle());
                assertEquals("INC-42", pm.getIncidentRef());
                DataSource ds = second.getBean(DataSourceRepository.class).findById(dsId).orElseThrow();
                assertEquals("Real source", ds.getName());
                assertEquals("https://example.invalid/source", ds.getUrl());
                IncidentNote note = second.getBean(IncidentNoteRepository.class).findById(noteId).orElseThrow();
                assertEquals("Real note", note.getContent());

                assertEquals(1, second.getBean(PostmortemRepository.class).count());
                assertEquals(1, second.getBean(DataSourceRepository.class).count());
                assertEquals(1, second.getBean(IncidentNoteRepository.class).count());

                UserRepository users = second.getBean(UserRepository.class);
                assertEquals(0, users.count());
                for (String name : List.of("admin", "user1", "user2")) {
                    assertTrue(users.findByUsername(name).isEmpty(), "unexpected user " + name);
                }
            }
        } finally {
            dropKeyspace(keyspace);
        }
    }

    @Test
    void concurrentBootstrapCreatesExactlyOneAdmin() throws Exception {
        String keyspace = keyspace("admin");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try (ConfigurableApplicationContext ctx = start(keyspace, "test", "false", "")) {
            UserRepository users = ctx.getBean(UserRepository.class);
            assertEquals(0, users.count());
            CassandraOperations operations = ctx.getBean(CassandraOperations.class);
            PasswordEncoder encoder = ctx.getBean(PasswordEncoder.class);
            AdminUserInitializer initializer = new AdminUserInitializer();

            List<String> passwords = List.of("first-password-123", "second-password-45");
            List<Callable<Void>> tasks = passwords.stream().<Callable<Void>>map(pw -> () -> {
                CommandLineRunner runner = initializer.adminUserBootstrap(
                        users, operations, encoder, pw);
                runner.run();
                return null;
            }).toList();
            for (Future<Void> f : pool.invokeAll(tasks, 60, TimeUnit.SECONDS)) {
                f.get();
            }

            assertEquals(1, users.count());
            User admin = users.findByUsername("admin").orElseThrow();
            assertEquals(AdminUserInitializer.BOOTSTRAP_ADMIN_UUID, admin.getUuid());
            assertTrue(passwords.stream().anyMatch(pw -> encoder.matches(pw, admin.getPassword())),
                    "stored password must belong to one of the attempts");
        } finally {
            pool.shutdownNow();
            dropKeyspace(keyspace);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"prod", "acc"})
    void prodOrAccWithoutDatasourceKeyFailsToStart(String profile) {
        String keyspace = keyspace("nokey_" + profile);
        try {
            ConfigurableApplicationContext ctx = null;
            Throwable failure = null;
            try {
                ctx = start(keyspace, profile, "false", "", "");
            } catch (Exception e) {
                failure = e;
            }
            if (ctx != null) {
                ctx.close();
                fail("the app must not start in " + profile + " without MELOPOST_DATASOURCE_KEY");
            }
            boolean found = false;
            for (Throwable t = failure; t != null; t = t.getCause()) {
                if (t.getMessage() != null && t.getMessage().contains("MELOPOST_DATASOURCE_KEY must be set")) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "startup failure must name the missing key");
        } finally {
            dropKeyspace(keyspace);
        }
    }

    @Test
    void existingPlaintextTokenIsEncryptedOnStart() {
        String keyspace = keyspace("enc");
        UUID dsId = UUID.randomUUID();
        try {
            try (ConfigurableApplicationContext first = start(keyspace, "test", "false", "", "")) {
                DataSource ds = new DataSource();
                ds.setUuid(dsId);
                ds.setName("Legacy");
                ds.setPassword("plain-token");
                first.getBean(DataSourceRepository.class).save(ds);
            }
            String afterSecond;
            try (ConfigurableApplicationContext second = start(keyspace, "test", "false", "", KEY)) {
                afterSecond = second.getBean(DataSourceRepository.class).findById(dsId).orElseThrow().getPassword();
                assertTrue(afterSecond.startsWith("enc:v1:"));
                assertEquals("plain-token", new com.melomarit.melopost.service.SecretCipher(KEY, new org.springframework.mock.env.MockEnvironment()).decrypt(afterSecond));
                assertEquals("Legacy", second.getBean(DataSourceRepository.class).findById(dsId).orElseThrow().getName());
            }
            try (ConfigurableApplicationContext third = start(keyspace, "test", "false", "", KEY)) {
                assertEquals(afterSecond, third.getBean(DataSourceRepository.class).findById(dsId).orElseThrow().getPassword());
            }
        } finally {
            dropKeyspace(keyspace);
        }
    }

    @Test
    void prefixedUndecryptableTokenIsLeftUntouchedOnStart() {
        String keyspace = keyspace("garb");
        UUID dsId = UUID.randomUUID();
        try {
            try (ConfigurableApplicationContext first = start(keyspace, "test", "false", "", "")) {
                DataSource ds = new DataSource();
                ds.setUuid(dsId);
                ds.setName("Odd");
                ds.setPassword("enc:v1:garbage");
                first.getBean(DataSourceRepository.class).save(ds);
            }
            try (ConfigurableApplicationContext second = start(keyspace, "test", "false", "", KEY)) {
                assertEquals("enc:v1:garbage", second.getBean(DataSourceRepository.class).findById(dsId).orElseThrow().getPassword());
            }
        } finally {
            dropKeyspace(keyspace);
        }
    }
}
