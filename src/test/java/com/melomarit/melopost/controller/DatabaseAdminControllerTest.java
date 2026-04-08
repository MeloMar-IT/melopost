package com.melomarit.melopost.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DatabaseAdminController.class)
public class DatabaseAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetDatabaseInfo() throws Exception {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(Collections.singletonList("USERS"));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
                .thenReturn(10);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), anyString()))
                .thenReturn(Collections.singletonList("ID"));

        mockMvc.perform(get("/admin/database"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/database"))
                .andExpect(model().attributeExists("tables"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetTableInfo() throws Exception {
        // Mock for getDatabaseInfo call inside getTableInfo
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(Collections.singletonList("USERS"));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
                .thenReturn(10);
        
        // Mock for getTableInfo specific queries
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq("USERS")))
                .thenReturn(Collections.singletonList("ID"));
        
        when(jdbcTemplate.queryForList("SELECT * FROM USERS LIMIT 100"))
                .thenReturn(new ArrayList<>());

        Map<String, Object> configMap = new HashMap<>();
        configMap.put("COLUMN_NAME", "ID");
        configMap.put("TYPE_NAME", "BIGINT");
        configMap.put("CHARACTER_MAXIMUM_LENGTH", null);
        configMap.put("IS_NULLABLE", "NO");
        configMap.put("COLUMN_DEFAULT", "NEXT VALUE FOR PUBLIC.SYSTEM_SEQUENCE_...");

        when(jdbcTemplate.queryForList(contains("INFORMATION_SCHEMA.COLUMNS"), eq("USERS")))
                .thenReturn(Collections.singletonList(configMap));

        when(jdbcTemplate.queryForList(contains("INFORMATION_SCHEMA.INDEX_COLUMNS"), eq(String.class), eq("USERS")))
                .thenReturn(Collections.singletonList("ID"));

        mockMvc.perform(get("/admin/database/table/USERS"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/database"))
                .andExpect(model().attributeExists("selectedTable"))
                .andExpect(model().attributeExists("columns"))
                .andExpect(model().attributeExists("rows"))
                .andExpect(model().attributeExists("tableConfig"))
                .andExpect(model().attributeExists("primaryKeys"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testExecuteQuery() throws Exception {
        // Mock for getDatabaseInfo call
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(Collections.emptyList());

        // Mock for SELECT query
        String sql = "SELECT * FROM USERS";
        mockMvc.perform(post("/admin/database/query")
                        .param("sql", sql)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/database"))
                .andExpect(model().attribute("query", sql))
                .andExpect(model().attributeExists("tables"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testExecuteUpdateQuery() throws Exception {
        // Mock for getDatabaseInfo call
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(Collections.emptyList());

        // Mock for UPDATE query
        String sql = "UPDATE USERS SET NAME = 'Test' WHERE ID = 1";
        when(jdbcTemplate.update(sql)).thenReturn(1);

        mockMvc.perform(post("/admin/database/query")
                        .param("sql", sql)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/database"))
                .andExpect(model().attribute("query", sql))
                .andExpect(model().attributeExists("queryMessage"))
                .andExpect(model().attribute("queryMessage", "Update successful. 1 rows affected."));
    }
}
