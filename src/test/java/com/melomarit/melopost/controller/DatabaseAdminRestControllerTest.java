package com.melomarit.melopost.controller;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ColumnDefinition;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.metadata.Metadata;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import com.datastax.oss.driver.api.core.metadata.schema.TableMetadata;
import com.datastax.oss.driver.api.core.type.UserDefinedType;
import com.datastax.oss.driver.api.core.cql.ExecutionInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    @MockitoBean
    private CqlSession session;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetTables() throws Exception {
        CqlIdentifier keyspace = CqlIdentifier.fromInternal("melopost");
        when(session.getKeyspace()).thenReturn(Optional.of(keyspace));
        
        Metadata metadata = mock(Metadata.class);
        when(session.getMetadata()).thenReturn(metadata);
        
        KeyspaceMetadata keyspaceMetadata = mock(KeyspaceMetadata.class);
        when(metadata.getKeyspace(keyspace)).thenReturn(Optional.of(keyspaceMetadata));
        
        TableMetadata tableMetadata = mock(TableMetadata.class);
        CqlIdentifier tableName = CqlIdentifier.fromInternal("USERS");
        when(tableMetadata.getName()).thenReturn(tableName);
        when(keyspaceMetadata.getTables()).thenReturn(Collections.singletonMap(tableName, tableMetadata));
        
        // Mock UDTs
        UserDefinedType udt = mock(UserDefinedType.class);
        CqlIdentifier udtName = CqlIdentifier.fromInternal("cheese_layer");
        when(udt.getName()).thenReturn(udtName);
        when(udt.getFieldNames()).thenReturn(Collections.emptyList());
        when(keyspaceMetadata.getUserDefinedTypes()).thenReturn(Collections.singletonMap(udtName, udt));
        when(keyspaceMetadata.getUserDefinedType(anyString())).thenReturn(Optional.of(udt));
        when(keyspaceMetadata.getUserDefinedType(any(CqlIdentifier.class))).thenReturn(Optional.of(udt));

        when(keyspaceMetadata.getTable(anyString())).thenReturn(Optional.of(tableMetadata));
        when(keyspaceMetadata.getTable(any(CqlIdentifier.class))).thenReturn(Optional.of(tableMetadata));
        when(metadata.getKeyspace(any(CqlIdentifier.class))).thenReturn(Optional.of(keyspaceMetadata));
        when(metadata.getKeyspace(anyString())).thenReturn(Optional.of(keyspaceMetadata));
        
        ResultSet rs = mock(ResultSet.class);
        when(session.execute(anyString())).thenReturn(rs);
        Row row = mock(Row.class);
        when(rs.one()).thenReturn(row);
        when(row.getLong(0)).thenReturn(10L);

        // Test without counts (explicitly false)
        mockMvc.perform(get("/api/admin/database/tables?includeCounts=false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("USERS"))
                .andExpect(jsonPath("$[0].rowCount").value(org.hamcrest.Matchers.nullValue()));

        // Test with counts (default is now true)
        mockMvc.perform(get("/api/admin/database/tables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("USERS"))
                .andExpect(jsonPath("$[0].rowCount").value(10));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetTableInfoForCheeseLayer() throws Exception {
        CqlIdentifier keyspace = CqlIdentifier.fromInternal("melopost");
        when(session.getKeyspace()).thenReturn(Optional.of(keyspace));
        
        Metadata metadata = mock(Metadata.class);
        when(session.getMetadata()).thenReturn(metadata);
        
        KeyspaceMetadata keyspaceMetadata = mock(KeyspaceMetadata.class);
        when(metadata.getKeyspace(keyspace)).thenReturn(Optional.of(keyspaceMetadata));
        when(metadata.getKeyspace(anyString())).thenReturn(Optional.of(keyspaceMetadata));
        when(metadata.getKeyspace(any(CqlIdentifier.class))).thenReturn(Optional.of(keyspaceMetadata));
        when(keyspaceMetadata.getTable(anyString())).thenReturn(Optional.empty());
        when(keyspaceMetadata.getTable(any(CqlIdentifier.class))).thenReturn(Optional.empty());
        
        UserDefinedType udt = mock(UserDefinedType.class);
        CqlIdentifier udtName = CqlIdentifier.fromInternal("cheese_layer");
        when(udt.getFieldNames()).thenReturn(Collections.singletonList(CqlIdentifier.fromInternal("name")));
        when(udt.getFieldTypes()).thenReturn(Collections.singletonList(com.datastax.oss.driver.api.core.type.DataTypes.TEXT));
        when(keyspaceMetadata.getUserDefinedType("cheese_layer")).thenReturn(Optional.of(udt));
        when(keyspaceMetadata.getUserDefinedType(CqlIdentifier.fromInternal("cheese_layer"))).thenReturn(Optional.of(udt));

        ResultSet rs = mock(ResultSet.class);
        when(session.execute(anyString())).thenReturn(rs);
        
        Row row = mock(Row.class);
        com.datastax.oss.driver.api.core.data.UdtValue udtValue = mock(com.datastax.oss.driver.api.core.data.UdtValue.class);
        when(udtValue.getType()).thenReturn(udt);
        when(udtValue.getString("name")).thenReturn("Test Layer");
        when(udtValue.getObject(any(CqlIdentifier.class))).thenReturn("Test Layer");
        
        when(row.getList("layers", com.datastax.oss.driver.api.core.data.UdtValue.class)).thenReturn(Collections.singletonList(udtValue));
        when(rs.iterator()).thenReturn(Collections.singletonList(row).iterator());

        mockMvc.perform(get("/api/admin/database/table/cheese_layer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("cheese_layer"))
                .andExpect(jsonPath("$.type").value("TYPE"))
                .andExpect(jsonPath("$.data.rows[0][0]").value("Test Layer"))
                .andExpect(jsonPath("$.data.message").value(org.hamcrest.Matchers.containsString("Found 1 unique instances")));
    }
}
