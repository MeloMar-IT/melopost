package com.melomarit.melopost.config;

import com.melomarit.melopost.model.User;
import com.melomarit.melopost.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

/**
 * Creates the first admin from {@code melopost.admin.initial-password} when no user exists.
 * The insert is conditional on a fixed uuid, so concurrent instances create at most one admin.
 */
@Configuration
public class AdminUserInitializer {
    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    static final UUID BOOTSTRAP_ADMIN_UUID =
            UUID.nameUUIDFromBytes("melopost-initial-admin".getBytes(StandardCharsets.UTF_8));
    static final int MIN_PASSWORD_LENGTH = 12;

    @Bean
    @Order(4)
    public CommandLineRunner adminUserBootstrap(
            UserRepository userRepository,
            CassandraOperations cassandraOperations,
            PasswordEncoder passwordEncoder,
            @Value("${melopost.admin.initial-password:}") String initialPassword) {
        return args -> {
            if (userRepository.count() > 0) {
                log.debug("[ADMIN_BOOTSTRAP] Users already exist; no admin created.");
                return;
            }
            if (initialPassword == null || initialPassword.isBlank()) {
                log.warn("[ADMIN_BOOTSTRAP] No users exist and MELOPOST_ADMIN_INITIAL_PASSWORD is not set: no admin user created.");
                return;
            }
            if (initialPassword.length() < MIN_PASSWORD_LENGTH) {
                log.error("[ADMIN_BOOTSTRAP] MELOPOST_ADMIN_INITIAL_PASSWORD is shorter than {} characters: no admin user created.",
                        MIN_PASSWORD_LENGTH);
                return;
            }
            User admin = new User();
            admin.setUuid(BOOTSTRAP_ADMIN_UUID);
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode(initialPassword));
            admin.setRoles(Set.of("ADMIN", "USER"));
            admin.setActive(true);
            var result = cassandraOperations.insert(admin, InsertOptions.builder().withIfNotExists().build());
            if (result.wasApplied()) {
                log.info("[ADMIN_BOOTSTRAP] Created initial admin user 'admin'.");
            } else {
                log.info("[ADMIN_BOOTSTRAP] Initial admin was already created by another instance.");
            }
        };
    }
}
