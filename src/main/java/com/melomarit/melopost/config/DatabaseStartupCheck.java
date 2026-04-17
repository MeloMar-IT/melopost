package com.melomarit.melopost.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DatabaseStartupCheck {
    private static final Logger log = LoggerFactory.getLogger(DatabaseStartupCheck.class);

    @Bean
    @Order(2)
    public CommandLineRunner databaseCheck() {
        return args -> {
            log.info("[DATABASE_CHECK] Cassandra is enabled, skipping H2 table check.");
        };
    }
}
