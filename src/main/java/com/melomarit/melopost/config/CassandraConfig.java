package com.melomarit.melopost.config;

import com.datastax.oss.driver.api.core.CqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CassandraConfig {

    private static final Logger log = LoggerFactory.getLogger(CassandraConfig.class);

    @Value("${spring.cassandra.keyspace-name:melopost}")
    private String keyspaceName;

    @Value("${spring.cassandra.contact-points:127.0.0.1}")
    private String contactPoints;

    @Value("${spring.cassandra.port:9042}")
    private int port;

    @Value("${spring.cassandra.local-datacenter:datacenter1}")
    private String localDatacenter;

    @Bean
    public CqlSessionBuilderCustomizer sessionBuilderCustomizer() {
        return builder -> {
            log.info("Checking/Creating Cassandra keyspace: {}", keyspaceName);
            try (CqlSession tempSession = CqlSession.builder()
                    .addContactPoint(new java.net.InetSocketAddress(contactPoints.split(",")[0].trim(), port))
                    .withLocalDatacenter(localDatacenter)
                    .build()) {
                tempSession.execute(String.format(
                        "CREATE KEYSPACE IF NOT EXISTS %s WITH replication = {'class':'SimpleStrategy', 'replication_factor':1};",
                        keyspaceName));
                log.info("Keyspace {} created or already exists.", keyspaceName);
            } catch (Exception e) {
                log.error("Failed to create keyspace {}: {}", keyspaceName, e.getMessage());
            }
            // Explicitly don't set keyspace on builder here if we want Spring Boot to handle it via properties,
            // but we must ensure it's not trying to USE it before it's created.
            // Actually, if we set it here, we are SURE it's set.
            builder.withKeyspace(keyspaceName);
        };
    }
}
