package com.melomarit.melopost.config;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import com.datastax.oss.driver.api.core.metadata.schema.TableMetadata;
import com.datastax.oss.driver.api.core.type.UserDefinedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Configuration
@ConditionalOnProperty(name = "spring.cassandra.enabled", havingValue = "true", matchIfMissing = true)
public class CassandraStartupCheck {
    private static final Logger log = LoggerFactory.getLogger(CassandraStartupCheck.class);

    @Bean
    @Order(1)
    public CommandLineRunner cassandraSchemaBootstrap(CqlSession session) {
        return args -> {
            log.info("[CASSANDRA_BOOTSTRAP] Startup: Bootstrapping Cassandra schema...");

            String keyspaceName = session.getKeyspace().orElseThrow().asInternal();
            KeyspaceMetadata keyspaceMetadata = session.getMetadata().getKeyspace(keyspaceName).orElseThrow();

            // 1. Create UDTs
            createUdtIfNotExists(session, keyspaceMetadata, "story",
                "CREATE TYPE story (" +
                "uuid uuid," +
                "storynumber text," +
                "teamname text," +
                "backlogname text," +
                "platform text," +
                "whattofix text," +
                "foundbydepartment text," +
                "tosolvebydepartment text," +
                "priority text," +
                "managername text," +
                "storylink text," +
                "status text," +
                "notes text," +
                "tags list<text>" +
                ");");

            createUdtIfNotExists(session, keyspaceMetadata, "hole",
                "CREATE TYPE hole (" +
                "uuid uuid," +
                "description text," +
                "teamname text," +
                "remedialaction text," +
                "actionstatus text," +
                "tags list<text>," +
                "story frozen<story>" +
                ");");

            createUdtIfNotExists(session, keyspaceMetadata, "cheese_layer",
                "CREATE TYPE cheese_layer (" +
                "uuid uuid," +
                "name text," +
                "description text," +
                "holes list<frozen<hole>>" +
                ");");

            createUdtIfNotExists(session, keyspaceMetadata, "timeline_event",
                "CREATE TYPE timeline_event (" +
                "uuid uuid," +
                "eventtime timestamp," +
                "description text" +
                ");");

            createUdtIfNotExists(session, keyspaceMetadata, "postmortem_question",
                "CREATE TYPE postmortem_question (" +
                "uuid uuid," +
                "question text," +
                "answer text," +
                "cheeselayer text" +
                ");");

            createUdtIfNotExists(session, keyspaceMetadata, "service_impact",
                "CREATE TYPE service_impact (" +
                "uuid uuid," +
                "service text," +
                "country text," +
                "start_time timestamp," +
                "end_time timestamp," +
                "duration text," +
                "impact_description text" +
                ");");

            createUdtIfNotExists(session, keyspaceMetadata, "cbs_impact",
                "CREATE TYPE cbs_impact (" +
                "uuid uuid," +
                "cbs_name text," +
                "it_services text," +
                "start_time timestamp," +
                "end_time timestamp," +
                "duration text," +
                "tolerance_level_exceeded text" +
                ");");

            // 2. Create Tables
            createTableIfNotExists(session, keyspaceMetadata, "postmortem",
                "CREATE TABLE postmortem (" +
                "uuid uuid PRIMARY KEY," +
                "title text," +
                "description text," +
                "start_date timestamp," +
                "incident_date timestamp," +
                "post_mortem_meeting_date timestamp," +
                "due_date timestamp," +
                "incident_ref text," +
                "incident_source text," +
                "story_store text," +
                "department text," +
                "failed_application text," +
                "type text," +
                "note text," +
                "status text," +
                "tags list<text>," +
                "layers list<frozen<cheese_layer>>," +
                "timeline_events list<frozen<timeline_event>>," +
                "service_impacts list<frozen<service_impact>>," +
                "cbs_impacts list<frozen<cbs_impact>>," +
                "questions list<frozen<postmortem_question>>," +
                "document_uuids list<uuid>," +
                "local_postmortem_uuids list<uuid>," +
                "created_at timestamp," +
                "updated_at timestamp" +
                ");");

            createTableIfNotExists(session, keyspaceMetadata, "users",
                "CREATE TABLE users (" +
                "uuid uuid PRIMARY KEY," +
                "username text," +
                "password text," +
                "email text," +
                "first_name text," +
                "last_name text," +
                "active boolean," +
                "roles set<text>," +
                "allowed_departments set<text>" +
                ");");

            createTableIfNotExists(session, keyspaceMetadata, "data_source",
                "CREATE TABLE data_source (" +
                "uuid uuid PRIMARY KEY," +
                "name text," +
                "type text," +
                "url text," +
                "username text," +
                "password text," +
                "operation text," +
                "description text," +
                "created_at timestamp," +
                "updated_at timestamp" +
                ");");

            createTableIfNotExists(session, keyspaceMetadata, "report_template",
                "CREATE TABLE report_template (" +
                "uuid uuid PRIMARY KEY," +
                "name text," +
                "content text," +
                "is_default boolean," +
                "created_at timestamp," +
                "updated_at timestamp" +
                ");");

            createTableIfNotExists(session, keyspaceMetadata, "postmortem_document",
                "CREATE TABLE postmortem_document (" +
                "uuid uuid PRIMARY KEY," +
                "postmortem_uuid uuid," +
                "filename text," +
                "content_type text," +
                "content blob," +
                "created_at timestamp" +
                ");");

            // 3. Create Indexes
            createIndexIfNotExists(session, "users_username_idx", "users(username)");
            createIndexIfNotExists(session, "postmortem_doc_pm_uuid_idx", "postmortem_document(postmortem_uuid)");
        };
    }

    private void createUdtIfNotExists(CqlSession session, KeyspaceMetadata keyspaceMetadata, String typeName, String cql) {
        Optional<UserDefinedType> udt = keyspaceMetadata.getUserDefinedType(typeName);
        if (udt.isEmpty()) {
            log.info("[CASSANDRA_BOOTSTRAP] Creating UDT '{}'...", typeName);
            session.execute(cql);
        } else {
            log.info("[CASSANDRA_BOOTSTRAP] UDT '{}' already exists. Dropping and recreating to ensure correct schema...", typeName);
            // Drop dependent tables if necessary, or just hope ALTER is enough.
            // Recreating UDTs in Cassandra can be tricky if they are used in tables.
            // Dropping and recreating is safer for schema consistency if no data is present.
            try {
                session.execute("DROP TYPE " + typeName);
                session.execute(cql);
            } catch (Exception e) {
                log.warn("[CASSANDRA_BOOTSTRAP] Could not drop/recreate UDT '{}', attempting ALTER instead: {}", typeName, e.getMessage());
                UserDefinedType currentUdt = udt.get();
                if (typeName.equals("story")) {
                    ensureUdtFieldExists(session, currentUdt, "teamname", "text");
                    ensureUdtFieldExists(session, currentUdt, "storynumber", "text");
                    ensureUdtFieldExists(session, currentUdt, "backlogname", "text");
                    ensureUdtFieldExists(session, currentUdt, "managername", "text");
                    ensureUdtFieldExists(session, currentUdt, "foundbydepartment", "text");
                    ensureUdtFieldExists(session, currentUdt, "tosolvebydepartment", "text");
                    ensureUdtFieldExists(session, currentUdt, "storylink", "text");
                    ensureUdtFieldExists(session, currentUdt, "whattofix", "text");
                } else if (typeName.equals("hole")) {
                    ensureUdtFieldExists(session, currentUdt, "teamname", "text");
                    ensureUdtFieldExists(session, currentUdt, "remedialaction", "text");
                    ensureUdtFieldExists(session, currentUdt, "actionstatus", "text");
                } else if (typeName.equals("timeline_event")) {
                    ensureUdtFieldExists(session, currentUdt, "eventtime", "timestamp");
                } else if (typeName.equals("postmortem_question")) {
                    ensureUdtFieldExists(session, currentUdt, "cheeselayer", "text");
                }
            }
        }
    }

    private void ensureUdtFieldExists(CqlSession session, UserDefinedType udt, String fieldName, String fieldType) {
        if (!udt.getFieldNames().contains(CqlIdentifier.fromInternal(fieldName))) {
            log.info("[CASSANDRA_BOOTSTRAP] Adding missing field '{}' to UDT '{}'...", fieldName, udt.getName().asInternal());
            try {
                session.execute("ALTER TYPE " + udt.getName().asInternal() + " ADD " + fieldName + " " + fieldType);
            } catch (Exception e) {
                log.error("[CASSANDRA_BOOTSTRAP] Failed to add field '{}' to UDT '{}': {}", 
                        fieldName, udt.getName().asInternal(), e.getMessage());
            }
        }
    }

    private void createTableIfNotExists(CqlSession session, KeyspaceMetadata keyspaceMetadata, String tableName, String cql) {
        if (keyspaceMetadata.getTable(tableName).isEmpty()) {
            log.info("[CASSANDRA_BOOTSTRAP] Creating table '{}'...", tableName);
            session.execute(cql);
        } else {
            log.info("[CASSANDRA_BOOTSTRAP] Table '{}' already exists. Ensuring schema is up to date...", tableName);
            
            TableMetadata tableMetadata = keyspaceMetadata.getTable(tableName).get();
            
            if (tableName.equals("postmortem")) {
                ensureColumnExists(session, tableMetadata, "title", "text");
                ensureColumnExists(session, tableMetadata, "description", "text");
                ensureColumnExists(session, tableMetadata, "start_date", "timestamp");
                ensureColumnExists(session, tableMetadata, "incident_date", "timestamp");
                ensureColumnExists(session, tableMetadata, "post_mortem_meeting_date", "timestamp");
                ensureColumnExists(session, tableMetadata, "due_date", "timestamp");
                ensureColumnExists(session, tableMetadata, "incident_ref", "text");
                ensureColumnExists(session, tableMetadata, "incident_source", "text");
                ensureColumnExists(session, tableMetadata, "story_store", "text");
                ensureColumnExists(session, tableMetadata, "department", "text");
                ensureColumnExists(session, tableMetadata, "failed_application", "text");
                ensureColumnExists(session, tableMetadata, "type", "text");
                ensureColumnExists(session, tableMetadata, "note", "text");
                ensureColumnExists(session, tableMetadata, "status", "text");
                ensureColumnExists(session, tableMetadata, "tags", "list<text>");
                ensureColumnExists(session, tableMetadata, "layers", "list<frozen<cheese_layer>>");
                ensureColumnExists(session, tableMetadata, "timeline_events", "list<frozen<timeline_event>>");
                ensureColumnExists(session, tableMetadata, "service_impacts", "list<frozen<service_impact>>");
                ensureColumnExists(session, tableMetadata, "cbs_impacts", "list<frozen<cbs_impact>>");
                ensureColumnExists(session, tableMetadata, "questions", "list<frozen<postmortem_question>>");
                ensureColumnExists(session, tableMetadata, "document_uuids", "list<uuid>");
                ensureColumnExists(session, tableMetadata, "local_postmortem_uuids", "list<uuid>");
                ensureColumnExists(session, tableMetadata, "created_at", "timestamp");
                ensureColumnExists(session, tableMetadata, "updated_at", "timestamp");

                // Handle legacy 'id' column
                if (tableMetadata.getColumn("id").isPresent()) {
                    log.info("[CASSANDRA_BOOTSTRAP] Legacy 'id' column found in 'postmortem' table. Checking partition key...");
                    boolean isIdPk = tableMetadata.getPartitionKey().stream()
                            .anyMatch(c -> c.getName().asInternal().equals("id"));

                    if (isIdPk) {
                        log.warn("[CASSANDRA_BOOTSTRAP] 'id' is a partition key in 'postmortem'. Dropping and recreating table to use 'uuid' instead.");
                        session.execute("DROP TABLE postmortem");
                        session.execute(cql);
                        return; // Exit this table's processing as it's recreated
                    }
                }
            }
            
            if (tableName.equals("users")) {
                ensureColumnExists(session, tableMetadata, "roles", "set<text>");
                ensureColumnExists(session, tableMetadata, "allowed_departments", "set<text>");
                ensureColumnExists(session, tableMetadata, "first_name", "text");
                ensureColumnExists(session, tableMetadata, "last_name", "text");
                ensureColumnExists(session, tableMetadata, "active", "boolean");
                ensureColumnExists(session, tableMetadata, "uuid", "uuid");
                
                // Handle legacy 'id' column
                if (tableMetadata.getColumn("id").isPresent()) {
                    log.info("[CASSANDRA_BOOTSTRAP] Legacy 'id' column found in 'users' table. Checking partition key...");
                    boolean isIdPk = tableMetadata.getPartitionKey().stream()
                            .anyMatch(c -> c.getName().asInternal().equals("id"));
                    
                    if (isIdPk) {
                        log.warn("[CASSANDRA_BOOTSTRAP] 'id' is a partition key in 'users'. Dropping and recreating table to use 'uuid' instead.");
                        session.execute("DROP TABLE users");
                        session.execute(cql);
                        return; // Exit this table's processing as it's recreated
                    } else {
                        log.info("[CASSANDRA_BOOTSTRAP] Legacy 'id' column is not a PK. It can be ignored or dropped later.");
                    }
                }
            }
            
            if (tableName.equals("data_source")) {
                ensureColumnExists(session, tableMetadata, "name", "text");
                ensureColumnExists(session, tableMetadata, "type", "text");
                ensureColumnExists(session, tableMetadata, "url", "text");
                ensureColumnExists(session, tableMetadata, "username", "text");
                ensureColumnExists(session, tableMetadata, "password", "text");
                ensureColumnExists(session, tableMetadata, "operation", "text");
                ensureColumnExists(session, tableMetadata, "description", "text");
                ensureColumnExists(session, tableMetadata, "created_at", "timestamp");
                ensureColumnExists(session, tableMetadata, "updated_at", "timestamp");
                ensureColumnExists(session, tableMetadata, "uuid", "uuid");

                // Handle legacy 'id' column
                if (tableMetadata.getColumn("id").isPresent()) {
                    log.info("[CASSANDRA_BOOTSTRAP] Legacy 'id' column found in 'data_source' table. Checking partition key...");
                    boolean isIdPk = tableMetadata.getPartitionKey().stream()
                            .anyMatch(c -> c.getName().asInternal().equals("id"));

                    if (isIdPk) {
                        log.warn("[CASSANDRA_BOOTSTRAP] 'id' is a partition key in 'data_source'. Dropping and recreating table to use 'uuid' instead.");
                        session.execute("DROP TABLE data_source");
                        session.execute(cql);
                        return; // Exit this table's processing as it's recreated
                    }
                }
            }

            if (tableName.equals("report_template")) {
                ensureColumnExists(session, tableMetadata, "name", "text");
                ensureColumnExists(session, tableMetadata, "content", "text");
                ensureColumnExists(session, tableMetadata, "is_default", "boolean");
                ensureColumnExists(session, tableMetadata, "created_at", "timestamp");
                ensureColumnExists(session, tableMetadata, "updated_at", "timestamp");
            }

            if (tableName.equals("postmortem_document")) {
                ensureColumnExists(session, tableMetadata, "postmortem_uuid", "uuid");
                ensureColumnExists(session, tableMetadata, "filename", "text");
                ensureColumnExists(session, tableMetadata, "content_type", "text");
                ensureColumnExists(session, tableMetadata, "content", "blob");
                ensureColumnExists(session, tableMetadata, "created_at", "timestamp");
            }
        }
    }

    private void ensureColumnExists(CqlSession session, TableMetadata tableMetadata, String columnName, String columnType) {
        if (tableMetadata.getColumn(columnName).isEmpty()) {
            log.info("[CASSANDRA_BOOTSTRAP] Adding missing column '{}' to table '{}'...", columnName, tableMetadata.getName().asInternal());
            try {
                session.execute("ALTER TABLE " + tableMetadata.getName().asInternal() + " ADD " + columnName + " " + columnType);
            } catch (Exception e) {
                log.error("[CASSANDRA_BOOTSTRAP] Failed to add column '{}' to table '{}': {}", 
                        columnName, tableMetadata.getName().asInternal(), e.getMessage());
            }
        }
    }

    private void createIndexIfNotExists(CqlSession session, String indexName, String target) {
        String cql = String.format("CREATE INDEX IF NOT EXISTS %s ON %s", indexName, target);
        log.info("[CASSANDRA_BOOTSTRAP] Ensuring index '{}' exists on '{}'...", indexName, target);
        session.execute(cql);
    }
}
