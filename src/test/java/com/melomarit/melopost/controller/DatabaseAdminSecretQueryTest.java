package com.melomarit.melopost.controller;

import com.datastax.oss.driver.api.core.CqlSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Console queries must not return a password value under an alias or inside SELECT JSON. */
@WebMvcTest(DatabaseAdminRestController.class)
class DatabaseAdminSecretQueryTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CqlSession session;

    private void postQuery(String sql) throws Exception {
        mockMvc.perform(post("/api/admin/database/query")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"" + sql + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("not allowed")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aliasedPasswordColumnIsRejectedAndNotExecuted() throws Exception {
        postQuery("SELECT password AS secret FROM users");
        verify(session, never()).execute(anyString());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void selectJsonIsRejectedAndNotExecuted() throws Exception {
        postQuery("SELECT JSON * FROM users");
        verify(session, never()).execute(anyString());
    }

    @Test
    void checkAllowsOrdinaryAndNonProjectionMatches() {
        assertNull(DatabaseAdminRestController.secretProjectionError("SELECT * FROM users"));
        assertNull(DatabaseAdminRestController.secretProjectionError("SELECT username, email FROM users WHERE username = 'password'"));
        assertNull(DatabaseAdminRestController.secretProjectionError("SELECT * FROM data_source"));
    }

    @Test
    void checkRejectsPasswordInAnyProjectionForm() {
        assertNotNull(DatabaseAdminRestController.secretProjectionError("SELECT username, password FROM users"));
        assertNotNull(DatabaseAdminRestController.secretProjectionError("select * , \"password\" as p from users"));
        assertNotNull(DatabaseAdminRestController.secretProjectionError("SELECT toJson(password) FROM users"));
        assertNotNull(DatabaseAdminRestController.secretProjectionError("select json password from users"));
    }
}
