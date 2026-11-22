package com.melomarit.melopost.controller;

import com.melomarit.melopost.config.SecurityConfig;
import com.melomarit.melopost.service.ReportTemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportTemplateViewController.class)
@Import(SecurityConfig.class)
class ReportTemplateViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportTemplateService templateService;

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "USER")
    void user_isForbiddenEverywhere() throws Exception {
        mockMvc.perform(post("/templates/save").with(csrf()).param("name", "x").param("content", "<html/>"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/templates")).andExpect(status().isForbidden());
        mockMvc.perform(get("/templates/new")).andExpect(status().isForbidden());
        mockMvc.perform(get("/templates/edit/" + UUID.randomUUID())).andExpect(status().isForbidden());
        mockMvc.perform(get("/templates/delete/" + UUID.randomUUID())).andExpect(status().isForbidden());
        verifyNoInteractions(templateService);
        verify(templateService, never()).save(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canSave() throws Exception {
        mockMvc.perform(post("/templates/save").with(csrf()).param("name", "x").param("content", "<html/>"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/templates"));
        verify(templateService, times(1)).save(any());
    }

    @Test
    void unauthenticated_isRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/templates"))
                .andExpect(status().is3xxRedirection());
        verifyNoInteractions(templateService);
    }
}
