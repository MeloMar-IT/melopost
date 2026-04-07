package com.melo.melopost.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
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
    public CommandLineRunner databaseCheck(DataSource dataSource) {
        return args -> {
            log.info("[DATABASE_CHECK] Startup: Checking database tables and schema status...");
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                String[] types = {"TABLE"};
                List<String> foundTables = new ArrayList<>();
                try (ResultSet rs = metaData.getTables(null, null, "%", types)) {
                    while (rs.next()) {
                        String tableSchema = rs.getString("TABLE_SCHEM");
                        String tableName = rs.getString("TABLE_NAME");
                        // Only include tables from the default schema (usually PUBLIC in H2)
                        // and exclude internal H2/System tables
                        if ("PUBLIC".equalsIgnoreCase(tableSchema)) {
                            foundTables.add(tableName);
                        }
                    }
                }
                
                log.info("[DATABASE_CHECK] Found tables: {}", foundTables);
                
                if (foundTables.isEmpty()) {
                    log.info("[DATABASE_CHECK] No application tables found. Hibernate's ddl-auto=update will create them if entities are present.");
                } else {
                    log.info("[DATABASE_CHECK] Application tables are present. Hibernate's ddl-auto=update will ensure they are up to date.");
                }
            } catch (Exception e) {
                log.error("[DATABASE_CHECK] Failed to check database schema at startup", e);
            }
        };
    }
}
