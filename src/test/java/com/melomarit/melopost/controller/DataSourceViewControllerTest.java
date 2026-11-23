package com.melomarit.melopost.controller;

import com.melomarit.melopost.model.DataSource;
import com.melomarit.melopost.service.DataSourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DataSourceViewController.class)
public class DataSourceViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataSourceService dataSourceService;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testShowCreateForm() throws Exception {
        when(dataSourceService.getTemplates()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/datasources/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("datasources/form"))
                .andExpect(model().attributeExists("datasource"))
                .andExpect(model().attributeExists("templates"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void editForm_shouldNotRenderStoredToken() throws Exception {
        DataSource ds = new DataSource();
        ds.setName("Jira");
        ds.setPassword("enc:v1:SECRET");
        when(dataSourceService.getDataSourceById(ds.getUuid())).thenReturn(Optional.of(ds));
        when(dataSourceService.getTemplates()).thenReturn(Collections.emptyList());

        String html = mockMvc.perform(get("/datasources/edit/" + ds.getUuid()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertFalse(html.contains("enc:v1:SECRET"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void save_shouldPassEmptyPasswordToKeepStoredToken() throws Exception {
        mockMvc.perform(post("/datasources/save").with(csrf()).param("name", "Jira").param("password", ""))
                .andExpect(status().is3xxRedirection());

        verify(dataSourceService).saveDataSource(any(DataSource.class), eq(""));
    }
}
