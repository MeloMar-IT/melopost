package com.melomarit.melopost.config;

import com.melomarit.melopost.service.DataSourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/** Encrypts data-source tokens that are still stored in plain text. Safe to run on every start. */
@Configuration
public class DataSourceSecretMigration {
    private static final Logger log = LoggerFactory.getLogger(DataSourceSecretMigration.class);

    @Bean
    @Order(3)
    public CommandLineRunner dataSourceSecretMigrationRunner(DataSourceService dataSourceService) {
        return args -> {
            int count = dataSourceService.encryptPlaintextPasswords();
            if (count > 0) {
                log.info("[DATASOURCE_KEY] Encrypted {} stored data-source token(s).", count);
            }
        };
    }
}
