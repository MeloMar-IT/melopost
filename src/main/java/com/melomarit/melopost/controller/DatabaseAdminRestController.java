package com.melomarit.melopost.controller;

import com.melomarit.melopost.dto.DatabaseTableDTO;
import com.melomarit.melopost.dto.QueryResultDTO;
import com.melomarit.melopost.dto.TableInfoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/database")
@PreAuthorize("hasRole('ADMIN')")
public class DatabaseAdminRestController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/tables")
    public List<DatabaseTableDTO> getTables() {
        List<DatabaseTableDTO> tables = new ArrayList<>();
        
        List<String> tableNames = jdbcTemplate.queryForList(
            "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE (TABLE_SCHEMA = 'PUBLIC' OR TABLE_SCHEMA = 'public') AND (TABLE_TYPE = 'TABLE' OR TABLE_TYPE = 'BASE TABLE') ORDER BY TABLE_NAME",
            String.class
        );

        for (String tableName : tableNames) {
            Integer rowCount;
            try {
                rowCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM \"" + tableName + "\"", Integer.class);
            } catch (Exception e) {
                rowCount = 0;
            }
            
            List<String> columns;
            try {
                columns = jdbcTemplate.queryForList(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND (TABLE_SCHEMA = 'PUBLIC' OR TABLE_SCHEMA = 'public') ORDER BY ORDINAL_POSITION",
                    String.class,
                    tableName
                );
            } catch (Exception e) {
                columns = new ArrayList<>();
            }
            
            DatabaseTableDTO dto = new DatabaseTableDTO();
            dto.setName(tableName);
            dto.setRowCount(rowCount);
            dto.setColumns(columns);
            tables.add(dto);
        }
        return tables;
    }

    @PostMapping("/query")
    public QueryResultDTO executeQuery(@RequestBody Map<String, String> request) {
        String sql = request.get("sql");
        QueryResultDTO result = new QueryResultDTO();
        result.setSql(sql);
        
        try {
            String trimmedSql = sql.trim().toUpperCase();
            if (trimmedSql.startsWith("SELECT") || trimmedSql.startsWith("SHOW") || trimmedSql.startsWith("DESCRIBE") || trimmedSql.startsWith("CALL")) {
                result.setIsSelect(true);
                
                List<String> columns = new ArrayList<>();
                List<List<Object>> rows = jdbcTemplate.query(sql, rs -> {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        columns.add(metaData.getColumnName(i));
                    }
                    
                    List<List<Object>> data = new ArrayList<>();
                    while (rs.next()) {
                        List<Object> row = new ArrayList<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.add(rs.getObject(i));
                        }
                        data.add(row);
                    }
                    return data;
                });
                
                result.setColumns(columns);
                result.setRows(rows);
                if (rows.isEmpty()) {
                    result.setMessage("Query executed successfully, but returned no rows.");
                }
            } else {
                result.setIsSelect(false);
                int affectedRows = jdbcTemplate.update(sql);
                result.setAffectedRows(affectedRows);
                result.setMessage("Update successful. " + affectedRows + " rows affected.");
            }
        } catch (Exception e) {
            result.setError(e.getMessage());
        }
        
        return result;
    }

    @GetMapping("/table/{name}")
    public TableInfoDTO getTableInfo(@PathVariable("name") String tableName) {
        TableInfoDTO info = new TableInfoDTO();
        info.setName(tableName);
        
        // Schema
        List<Map<String, Object>> schema = jdbcTemplate.queryForList(
            "SELECT COLUMN_NAME, DATA_TYPE as TYPE_NAME, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE, COLUMN_DEFAULT " +
            "FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND (TABLE_SCHEMA = 'PUBLIC' OR TABLE_SCHEMA = 'public') ORDER BY ORDINAL_POSITION",
            tableName
        );
        info.setSchema(schema);
        
        // PKs
        List<String> primaryKeys;
        try {
            primaryKeys = jdbcTemplate.queryForList(
                "SELECT k.COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE k " +
                "JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS t ON k.CONSTRAINT_NAME = t.CONSTRAINT_NAME " +
                "AND k.TABLE_NAME = t.TABLE_NAME AND k.TABLE_SCHEMA = t.TABLE_SCHEMA " +
                "WHERE t.CONSTRAINT_TYPE = 'PRIMARY KEY' AND t.TABLE_NAME = ? AND t.TABLE_SCHEMA = 'PUBLIC'",
                String.class,
                tableName
            );
        } catch (Exception e) {
            primaryKeys = new ArrayList<>();
        }
        info.setPrimaryKeys(primaryKeys);
        
        // Preview data (first 50 rows)
        QueryResultDTO data = executeQuery(Map.of("sql", "SELECT * FROM \"" + tableName + "\" LIMIT 50"));
        info.setData(data);
        
        return info;
    }
}
