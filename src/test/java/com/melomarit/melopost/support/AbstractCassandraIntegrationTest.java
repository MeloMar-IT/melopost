package com.melomarit.melopost.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for Spring tests that need Cassandra.
 *
 * <p>The container is a lazily started JVM-wide singleton (a per-class container would be restarted
 * per test class while Spring's context cache keeps the old mapped port). Ryuk removes it at JVM end.
 *
 * <p>Properties are overridden with {@code @DynamicPropertySource} rather than {@code @ServiceConnection},
 * because {@code CassandraConfig} reads {@code spring.cassandra.*} itself to create the keyspace.
 * Without Docker the tests are skipped.
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractCassandraIntegrationTest {

    private static final CassandraContainer CASSANDRA =
            new CassandraContainer(DockerImageName.parse("cassandra:4.1"));

    private static synchronized void startIfNeeded() {
        if (!CASSANDRA.isRunning()) {
            CASSANDRA.start();
        }
    }

    @DynamicPropertySource
    static void cassandraProperties(DynamicPropertyRegistry registry) {
        startIfNeeded();
        registry.add("spring.cassandra.contact-points", CASSANDRA::getHost);
        registry.add("spring.cassandra.port", () -> CASSANDRA.getMappedPort(9042));
        registry.add("spring.cassandra.local-datacenter", () -> "datacenter1");
        registry.add("spring.cassandra.keyspace-name", () -> "melopost");
    }
}
