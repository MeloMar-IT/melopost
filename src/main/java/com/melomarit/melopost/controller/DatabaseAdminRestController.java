package com.melomarit.melopost.controller;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.metadata.schema.ColumnMetadata;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import com.datastax.oss.driver.api.core.metadata.schema.TableMetadata;
import com.datastax.oss.driver.api.core.type.UserDefinedType;
import com.datastax.oss.driver.api.core.data.UdtValue;
import com.datastax.oss.driver.api.core.data.TupleValue;
import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.melomarit.melopost.dto.DatabaseTableDTO;
import com.melomarit.melopost.dto.QueryResultDTO;
import com.melomarit.melopost.dto.TableInfoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/database")
@PreAuthorize("hasRole('ADMIN')")
public class DatabaseAdminRestController {

    @Autowired
    private CqlSession session;

    @GetMapping("/tables")
    public List<DatabaseTableDTO> getTables(@RequestParam(value = "includeCounts", defaultValue = "true") boolean includeCounts) {
        return getTablesInternal(includeCounts);
    }

    public List<DatabaseTableDTO> getTablesInternal(boolean includeCounts) {
        String keyspaceName = session.getKeyspace().orElseThrow().asInternal();
        KeyspaceMetadata keyspaceMetadata = session.getMetadata().getKeyspace(keyspaceName).orElseThrow();
        
        List<DatabaseTableDTO> items = new ArrayList<>();
        
        // Add Tables
        for (TableMetadata tableMetadata : keyspaceMetadata.getTables().values()) {
            DatabaseTableDTO dto = new DatabaseTableDTO();
            dto.setName(tableMetadata.getName().asInternal());
            dto.setType("TABLE");
            
            if (includeCounts) {
                try {
                    ResultSet rs = session.execute("SELECT COUNT(*) FROM \"" + dto.getName() + "\"");
                    Row row = rs.one();
                    dto.setRowCount(row != null ? (int) row.getLong(0) : null);
                } catch (Exception e) {
                    dto.setRowCount(null);
                }
            } else {
                dto.setRowCount(null);
            }

            dto.setColumns(tableMetadata.getColumns().values().stream()
                .map(c -> c.getName().asInternal())
                .collect(Collectors.toList()));
            
            items.add(dto);
        }
        
        // Add User Defined Types (UDTs)
        for (UserDefinedType udt : keyspaceMetadata.getUserDefinedTypes().values()) {
            DatabaseTableDTO dto = new DatabaseTableDTO();
            String udtName = udt.getName().asInternal();
            dto.setName(udtName);
            dto.setType("TYPE");
            
            if (includeCounts) {
                try {
                    // Count UDT instances by using the same logic as getTableInfo
                    // For now, this is restricted to known UDTs and the postmortem table
                    ResultSet rs = session.execute("SELECT * FROM postmortem");
                    int count = 0;
                    for (Row row : rs) {
                        if ("cheese_layer".equals(udtName)) {
                            List<UdtValue> layers = row.getList("layers", UdtValue.class);
                            if (layers != null) count += layers.size();
                        } else if ("hole".equals(udtName)) {
                            List<UdtValue> layers = row.getList("layers", UdtValue.class);
                            if (layers != null) {
                                for (UdtValue layer : layers) {
                                    List<UdtValue> holes = layer.getList("holes", UdtValue.class);
                                    if (holes != null) count += holes.size();
                                }
                            }
                        } else if ("story".equals(udtName)) {
                            List<UdtValue> layers = row.getList("layers", UdtValue.class);
                            if (layers != null) {
                                for (UdtValue layer : layers) {
                                    List<UdtValue> holes = layer.getList("holes", UdtValue.class);
                                    if (holes != null) {
                                        for (UdtValue hole : holes) {
                                            if (hole.getUdtValue("story") != null) count++;
                                        }
                                    }
                                }
                            }
                        } else if ("timeline_event".equals(udtName)) {
                            List<UdtValue> events = row.getList("timeline_events", UdtValue.class);
                            if (events != null) count += events.size();
                        } else if ("postmortem_question".equals(udtName)) {
                            List<UdtValue> questions = row.getList("questions", UdtValue.class);
                            if (questions != null) count += questions.size();
                        }
                    }
                    dto.setRowCount(count);
                } catch (Exception e) {
                    dto.setRowCount(0);
                }
            } else {
                dto.setRowCount(null);
            }
            
            List<String> columns = new ArrayList<>();
            for (CqlIdentifier fieldName : udt.getFieldNames()) {
                columns.add(fieldName.asInternal());
            }
            dto.setColumns(columns);
            
            items.add(dto);
        }
        
        return items;
    }

    @PostMapping("/query")
    public QueryResultDTO executeQuery(@RequestBody Map<String, String> request) {
        String cql = request.get("sql");
        if (cql != null && cql.trim().toUpperCase().startsWith("SELECT") && !cql.toUpperCase().contains("LIMIT")) {
            cql = cql.trim();
            if (cql.endsWith(";")) {
                cql = cql.substring(0, cql.length() - 1);
            }
            // If it contains ALLOW FILTERING, we should insert LIMIT before it, but Cassandra usually allows LIMIT after ALLOW FILTERING too
            // Actually, the error shows: ...ALLOW FILTERING [LIMIT]... 
            // In CQL, LIMIT comes BEFORE ALLOW FILTERING.
            if (cql.toUpperCase().contains("ALLOW FILTERING")) {
                int index = cql.toUpperCase().lastIndexOf("ALLOW FILTERING");
                cql = cql.substring(0, index) + " LIMIT 1000 " + cql.substring(index);
            } else {
                cql += " LIMIT 1000";
            }
        }

        QueryResultDTO result = new QueryResultDTO();
        result.setSql(cql);
        
        try {
            ResultSet rs = session.execute(cql);
            result.setIsSelect(true); // Most CQL admin queries will be SELECT or DESCRIBE-like
            
            List<String> columns = new ArrayList<>();
            rs.getColumnDefinitions().forEach(cd -> columns.add(cd.getName().asInternal()));
            if (columns.isEmpty()) {
                for (int i = 0; i < rs.getColumnDefinitions().size(); i++) {
                    columns.add(rs.getColumnDefinitions().get(i).getName().asInternal());
                }
            }
            result.setColumns(columns);

            List<List<Object>> rows = new ArrayList<>();
            int count = 0;
            for (Row row : rs) {
                if (count++ >= 2000) break; // Hard safety limit to prevent OOM
                List<Object> rowData = new ArrayList<>();
                for (int i = 0; i < columns.size(); i++) {
                    try {
                        rowData.add(convertValue(row.getObject(i)));
                    } catch (Exception e) {
                        rowData.add("[Error: " + e.getMessage() + "]");
                    }
                }
                rows.add(rowData);
            }
            result.setRows(rows);
            
            if (rows.isEmpty() && rs.getExecutionInfo().getWarnings().isEmpty()) {
                result.setMessage("Query executed successfully, but returned no rows.");
            } else if (!rs.getExecutionInfo().getWarnings().isEmpty()) {
                String warnings = String.join("; ", rs.getExecutionInfo().getWarnings());
                result.setMessage("Query executed with warnings: " + warnings + (rows.isEmpty() ? ". No rows returned." : ""));
            }
        } catch (Exception e) {
            result.setError(e.getMessage());
        }
        
        return result;
    }

    private Object convertValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UdtValue) {
            UdtValue udtValue = (UdtValue) value;
            Map<String, Object> map = new HashMap<>();
            for (CqlIdentifier id : udtValue.getType().getFieldNames()) {
                map.put(id.asInternal(), convertValue(udtValue.getObject(id)));
            }
            return map;
        }
        if (value instanceof TupleValue) {
            TupleValue tupleValue = (TupleValue) value;
            List<Object> list = new ArrayList<>();
            for (int i = 0; i < tupleValue.size(); i++) {
                list.add(convertValue(tupleValue.getObject(i)));
            }
            return list;
        }
        if (value instanceof List) {
            return ((List<?>) value).stream().map(this::convertValue).collect(Collectors.toList());
        }
        if (value instanceof Set) {
            return ((Set<?>) value).stream().map(this::convertValue).collect(Collectors.toSet());
        }
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            Map<Object, Object> convertedMap = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                convertedMap.put(convertValue(entry.getKey()), convertValue(entry.getValue()));
            }
            return convertedMap;
        }
        if (value instanceof CqlIdentifier) {
            return ((CqlIdentifier) value).asInternal();
        }
        // Basic types like String, Integer, UUID, etc. are already serializable by Jackson
        // and Cassandra driver returns them as such.
        // If it's something else we don't know, toString() is a safe fallback for admin view
        String className = value.getClass().getName();
        if (className.startsWith("java.lang.") || className.startsWith("java.util.") || 
            value instanceof java.math.BigDecimal || value instanceof java.math.BigInteger ||
            value instanceof java.time.temporal.Temporal) {
            return value;
        }
        
        return value.toString();
    }

    @GetMapping("/table/{name}")
    public TableInfoDTO getTableInfo(@PathVariable("name") String tableName) {
        Optional<CqlIdentifier> keyspace = session.getKeyspace();
        if (keyspace.isEmpty()) {
            throw new IllegalStateException("No keyspace selected in session");
        }
        String keyspaceName = keyspace.get().asInternal();
        KeyspaceMetadata keyspaceMetadata = session.getMetadata().getKeyspace(keyspaceName)
            .orElseThrow(() -> new IllegalArgumentException("Keyspace not found: " + keyspaceName));
        
        TableInfoDTO info = new TableInfoDTO();
        info.setName(tableName);

        if (keyspaceMetadata.getTable(tableName).isPresent()) {
            TableMetadata tableMetadata = keyspaceMetadata.getTable(tableName).get();
            info.setType("TABLE");
            
            // Schema
            List<Map<String, Object>> schema = new ArrayList<>();
            for (ColumnMetadata column : tableMetadata.getColumns().values()) {
                Map<String, Object> colInfo = new HashMap<>();
                colInfo.put("COLUMN_NAME", column.getName().asInternal());
                colInfo.put("DATA_TYPE", column.getType().toString());
                schema.add(colInfo);
            }
            info.setSchema(schema);
            
            // PKs
            info.setPrimaryKeys(tableMetadata.getPrimaryKey().stream()
                .map(c -> c.getName().asInternal())
                .collect(Collectors.toList()));
            
            // Preview data
            QueryResultDTO data = executeQuery(Map.of("sql", "SELECT * FROM " + tableName + " LIMIT 50"));
            info.setData(data);
        } else if (keyspaceMetadata.getUserDefinedType(tableName).isPresent()) {
            UserDefinedType udt = keyspaceMetadata.getUserDefinedType(tableName).get();
            info.setType("TYPE");
            
            // Schema
            List<Map<String, Object>> schema = new ArrayList<>();
            List<CqlIdentifier> fieldNames = udt.getFieldNames();
            for (int i = 0; i < fieldNames.size(); i++) {
                Map<String, Object> fieldInfo = new HashMap<>();
                fieldInfo.put("COLUMN_NAME", fieldNames.get(i).asInternal());
                fieldInfo.put("DATA_TYPE", udt.getFieldTypes().get(i).toString());
                schema.add(fieldInfo);
            }
            info.setSchema(schema);
            info.setPrimaryKeys(new ArrayList<>());
            
            QueryResultDTO data = new QueryResultDTO();
            data.setColumns(fieldNames.stream().map(CqlIdentifier::asInternal).collect(Collectors.toList()));
            
            // Special handling for UDTs to show all instances
            try {
                String udtQuery = "SELECT * FROM postmortem";
                ResultSet rs = session.execute(udtQuery);
                data.setSql(udtQuery);
                List<List<Object>> allRows = new ArrayList<>();
                Map<String, List<Object>> uniqueItems = new HashMap<>();
                
                for (Row row : rs) {
                    if ("cheese_layer".equals(tableName)) {
                        List<UdtValue> layers = row.getList("layers", UdtValue.class);
                        if (layers != null) {
                            for (UdtValue layer : layers) {
                                String name = layer.getString("name");
                                if (name != null && !uniqueItems.containsKey(name)) {
                                    List<Object> rowData = new ArrayList<>();
                                    for (CqlIdentifier fieldId : fieldNames) {
                                        rowData.add(convertValue(layer.getObject(fieldId)));
                                    }
                                    uniqueItems.put(name, rowData);
                                }
                            }
                        }
                    } else if ("hole".equals(tableName)) {
                        List<UdtValue> layers = row.getList("layers", UdtValue.class);
                        if (layers != null) {
                            for (UdtValue layer : layers) {
                                List<UdtValue> holes = layer.getList("holes", UdtValue.class);
                                if (holes != null) {
                                    for (UdtValue hole : holes) {
                                        String description = hole.getString("description");
                                        if (description != null && !uniqueItems.containsKey(description)) {
                                            List<Object> rowData = new ArrayList<>();
                                            for (CqlIdentifier fieldId : fieldNames) {
                                                rowData.add(convertValue(hole.getObject(fieldId)));
                                            }
                                            uniqueItems.put(description, rowData);
                                        }
                                    }
                                }
                            }
                        }
                    } else if ("story".equals(tableName)) {
                        List<UdtValue> layers = row.getList("layers", UdtValue.class);
                        if (layers != null) {
                            for (UdtValue layer : layers) {
                                List<UdtValue> holes = layer.getList("holes", UdtValue.class);
                                if (holes != null) {
                                    for (UdtValue hole : holes) {
                                        UdtValue story = hole.getUdtValue("story");
                                        if (story != null) {
                                            String storyNumber = story.getString("storynumber");
                                            if (storyNumber != null && !uniqueItems.containsKey(storyNumber)) {
                                                List<Object> rowData = new ArrayList<>();
                                                for (CqlIdentifier fieldId : fieldNames) {
                                                    rowData.add(convertValue(story.getObject(fieldId)));
                                                }
                                                uniqueItems.put(storyNumber, rowData);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if ("timeline_event".equals(tableName)) {
                        List<UdtValue> events = row.getList("timeline_events", UdtValue.class);
                        if (events != null) {
                            for (UdtValue event : events) {
                                String description = event.getString("description");
                                if (description != null && !uniqueItems.containsKey(description)) {
                                    List<Object> rowData = new ArrayList<>();
                                    for (CqlIdentifier fieldId : fieldNames) {
                                        rowData.add(convertValue(event.getObject(fieldId)));
                                    }
                                    uniqueItems.put(description, rowData);
                                }
                            }
                        }
                    } else if ("postmortem_question".equals(tableName)) {
                        List<UdtValue> questions = row.getList("questions", UdtValue.class);
                        if (questions != null) {
                            for (UdtValue question : questions) {
                                String qText = question.getString("question");
                                if (qText != null && !uniqueItems.containsKey(qText)) {
                                    List<Object> rowData = new ArrayList<>();
                                    for (CqlIdentifier fieldId : fieldNames) {
                                        rowData.add(convertValue(question.getObject(fieldId)));
                                    }
                                    uniqueItems.put(qText, rowData);
                                }
                            }
                        }
                    }
                }
                allRows.addAll(uniqueItems.values());
                data.setRows(allRows);
                data.setIsSelect(true);
                data.setMessage("Found " + allRows.size() + " unique instances across all postmortems.");
            } catch (Exception e) {
                data.setMessage("Could not fetch " + tableName + " instances: " + e.getMessage());
                data.setRows(new ArrayList<>());
            }
            info.setData(data);
        } else {
            throw new IllegalArgumentException("Table or Type not found: " + tableName);
        }
        
        return info;
    }
}
