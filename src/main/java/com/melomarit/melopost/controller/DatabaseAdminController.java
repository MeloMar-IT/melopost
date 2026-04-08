package com.melomarit.melopost.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/database")
@PreAuthorize("hasRole('ADMIN')")
public class DatabaseAdminController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping
    public String getDatabaseInfo(Model model) {
        List<Map<String, Object>> tables = new ArrayList<>();
        
        try {
            // Query to get tables in H2
            List<String> tableNames = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND (TABLE_TYPE = 'TABLE' OR TABLE_TYPE = 'BASE TABLE') ORDER BY TABLE_NAME",
                String.class
            );

            for (String tableName : tableNames) {
                Map<String, Object> tableInfo = new HashMap<>();
                tableInfo.put("name", tableName);
                
                // Get row count
                try {
                    Integer rowCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM \"" + tableName + "\"", Integer.class);
                    tableInfo.put("rowCount", rowCount);
                } catch (Exception e) {
                    tableInfo.put("rowCount", 0);
                }
                
                // Get columns
                try {
                    List<String> columns = jdbcTemplate.queryForList(
                        "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND TABLE_SCHEMA = 'PUBLIC' ORDER BY ORDINAL_POSITION",
                        String.class,
                        tableName
                    );
                    tableInfo.put("columns", columns);
                } catch (Exception e) {
                    tableInfo.put("columns", new ArrayList<>());
                }
                
                tables.add(tableInfo);
            }
        } catch (Exception e) {
            model.addAttribute("queryError", "Error listing tables: " + e.getMessage());
        }

        model.addAttribute("tables", tables);
        return "admin/database";
    }

    @PostMapping("/query")
    public String executeQuery(@RequestParam("sql") String sql, Model model) {
        // Re-use the main view logic to show the list of tables too
        getDatabaseInfo(model);
        model.addAttribute("query", sql);
        
        try {
            if (sql.trim().toUpperCase().startsWith("SELECT") || sql.trim().toUpperCase().startsWith("SHOW")) {
                // Execute and get results for SELECT/SHOW
                List<Map<String, Object>> rows = jdbcTemplate.query(sql, (rs, rowNum) -> {
                    Map<String, Object> row = new HashMap<>();
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        String colName = metaData.getColumnName(i);
                        row.put(colName, rs.getObject(i));
                    }
                    return row;
                });

                if (!rows.isEmpty()) {
                    // Extract column names from the first row's keys to maintain order as per query
                    // Use a more reliable way to get column names in order
                    List<String> queryColumns = new ArrayList<>();
                    jdbcTemplate.query(sql, rs -> {
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        for (int i = 1; i <= columnCount; i++) {
                            queryColumns.add(metaData.getColumnName(i));
                        }
                        return null; // Just need the header
                    });

                    // To avoid double execution for headers, let's refine this
                    // For now, let's use the keys from the first row if available
                    List<String> cols = new ArrayList<>(rows.get(0).keySet());
                    
                    // Convert rows to ordered values
                    List<List<Object>> queryData = new ArrayList<>();
                    for (Map<String, Object> row : rows) {
                        List<Object> rowValues = new ArrayList<>();
                        for (String col : cols) {
                            rowValues.add(row.get(col));
                        }
                        queryData.add(rowValues);
                    }
                    
                    model.addAttribute("queryColumns", cols);
                    model.addAttribute("queryRows", queryData);
                } else {
                    model.addAttribute("queryMessage", "Query executed successfully, but returned no rows.");
                }
            } else {
                // Execute as update for non-SELECT
                int affectedRows = jdbcTemplate.update(sql);
                model.addAttribute("queryMessage", "Update successful. " + affectedRows + " rows affected.");
            }
        } catch (Exception e) {
            model.addAttribute("queryError", "Error executing query: " + e.getMessage());
        }
        
        return "admin/database";
    }

    @GetMapping("/table/{name}")
    public String getTableInfo(@PathVariable("name") String tableName, Model model) {
        // Re-use the main view logic to show the list of tables too
        getDatabaseInfo(model);
        
        try {
            // Fetch data for the selected table
            List<String> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND TABLE_SCHEMA = 'PUBLIC' ORDER BY ORDINAL_POSITION",
                String.class,
                tableName
            );
            
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM \"" + tableName + "\" LIMIT 100");
            
            // Fetch table configuration (DDL-like using INFORMATION_SCHEMA)
            List<Map<String, Object>> config = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME, DATA_TYPE AS TYPE_NAME, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE, COLUMN_DEFAULT " +
                "FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND TABLE_SCHEMA = 'PUBLIC' ORDER BY ORDINAL_POSITION",
                tableName
            );

            // Get primary keys
            List<String> primaryKeys = new ArrayList<>();
            try {
                // H2 Database specific query for primary keys
                primaryKeys = jdbcTemplate.queryForList(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.INDEX_COLUMNS " +
                    "WHERE TABLE_NAME = ? AND TABLE_SCHEMA = 'PUBLIC' AND PRIMARY_KEY = TRUE",
                    String.class,
                    tableName
                );
            } catch (Exception pkEx) {
                // Fallback or ignore if INFORMATION_SCHEMA structure is different
            }

            // Convert Map to ordered List of values based on column names
            List<List<Object>> tableData = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                List<Object> rowValues = new ArrayList<>();
                for (String col : columns) {
                    rowValues.add(row.get(col));
                }
                tableData.add(rowValues);
            }
            
            model.addAttribute("selectedTable", tableName);
            model.addAttribute("columns", columns);
            model.addAttribute("rows", tableData);
            model.addAttribute("tableConfig", config);
            model.addAttribute("primaryKeys", primaryKeys);
        } catch (Exception e) {
            model.addAttribute("queryError", "Error fetching table info: " + e.getMessage());
        }
        
        return "admin/database";
    }
}
