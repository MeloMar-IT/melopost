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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DataSourceViewController.class)
public class DataSourceViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataSourceService dataSourceService;

    @Test
    @WithMockUser
    public void testShowCreateForm() throws Exception {
        when(dataSourceService.getTemplates()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/datasources/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("datasources/form"))
                .andExpect(model().attributeExists("datasource"))
                .andExpect(model().attributeExists("templates"));
    }
}
