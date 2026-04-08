package com.melomarit.melopost.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DatabaseAdminRestController.class)
public class DatabaseAdminRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetTables() throws Exception {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(Collections.singletonList("USERS"));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
                .thenReturn(10);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), anyString()))
                .thenReturn(Collections.singletonList("ID"));

        mockMvc.perform(get("/api/admin/database/tables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("USERS"))
                .andExpect(jsonPath("$[0].rowCount").value(10))
                .andExpect(jsonPath("$[0].columns[0]").value("ID"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testExecuteQuery() throws Exception {
        // Mock a SELECT result
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class))).thenAnswer(invocation -> {
            ResultSetExtractor<Object> extractor = invocation.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            ResultSetMetaData metaData = mock(ResultSetMetaData.class);
            when(rs.getMetaData()).thenReturn(metaData);
            when(metaData.getColumnCount()).thenReturn(1);
            when(metaData.getColumnName(1)).thenReturn("ID");
            
            // Simulating one row
            when(rs.next()).thenReturn(true).thenReturn(false);
            when(rs.getObject(1)).thenReturn(1L);
            
            return extractor.extractData(rs);
        });

        mockMvc.perform(post("/api/admin/database/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\": \"SELECT * FROM USERS\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns[0]").value("ID"))
                .andExpect(jsonPath("$.rows[0][0]").value(1));
    }
}
