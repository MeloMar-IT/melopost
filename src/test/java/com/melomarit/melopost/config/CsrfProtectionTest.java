package com.melomarit.melopost.config;

import com.melomarit.melopost.controller.DataSourceViewController;
import com.melomarit.melopost.controller.IncidentNoteViewController;
import com.melomarit.melopost.controller.PostmortemController;
import com.melomarit.melopost.controller.ReportTemplateViewController;
import com.melomarit.melopost.controller.UserViewController;
import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.service.DataSourceService;
import com.melomarit.melopost.service.DocumentImportService;
import com.melomarit.melopost.service.IncidentNoteService;
import com.melomarit.melopost.service.PostmortemService;
import com.melomarit.melopost.service.ReportService;
import com.melomarit.melopost.service.ReportTemplateService;
import com.melomarit.melopost.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UserViewController.class, IncidentNoteViewController.class,
        ReportTemplateViewController.class, DataSourceViewController.class, PostmortemController.class})
@Import({SecurityConfig.class, CsrfProtectionTest.TestUsers.class})
class CsrfProtectionTest {

    private static final String BASIC_USER = "api";
    private static final String BASIC_PASSWORD = "api-password-123";

    @TestConfiguration
    static class TestUsers {
        @Bean
        InMemoryUserDetailsManager testUserDetails(PasswordEncoder encoder) {
            return new InMemoryUserDetailsManager(
                    User.withUsername(BASIC_USER).password(encoder.encode(BASIC_PASSWORD)).roles("USER").build());
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private IncidentNoteService incidentNoteService;
    @MockitoBean
    private PostmortemService postmortemService;
    @MockitoBean
    private ReportTemplateService reportTemplateService;
    @MockitoBean
    private DataSourceService dataSourceService;
    @MockitoBean
    private ReportService reportService;
    @MockitoBean
    private DocumentImportService importService;

    private MockMultipartFile file() {
        return new MockMultipartFile("file", "x.docx", "application/octet-stream", "x".getBytes(StandardCharsets.UTF_8));
    }

    // ---- API: CSRF applies to /api/** ----

    @Test
    void importWithSessionAndNoToken_isForbidden() throws Exception {
        mockMvc.perform(multipart("/api/postmortems/import").file(file())
                        .with(user("u").roles("USER"))
                        .header("Origin", "https://evil.example"))
                .andExpect(status().isForbidden());

        verify(importService, never()).importFromDocument(any());
    }

    @Test
    void importWithSessionAndHeaderToken_succeeds() throws Exception {
        when(importService.importFromDocument(any())).thenReturn(new Postmortem());

        mockMvc.perform(multipart("/api/postmortems/import").file(file())
                        .with(user("u").roles("USER"))
                        .with(csrf().asHeader()))
                .andExpect(status().isOk());

        verify(importService).importFromDocument(any());
    }

    @Test
    void importWithSessionAndMatchingBasicUserWrongPassword_noToken_isForbidden() throws Exception {
        String wrong = Base64.getEncoder().encodeToString((BASIC_USER + ":wrong-password").getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/postmortems/import").file(file())
                        .with(user(BASIC_USER).roles("USER"))
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + wrong))
                .andExpect(status().isForbidden());

        verify(importService, never()).importFromDocument(any());
    }

    @Test
    void importWithBasicOnlyAndNoToken_isForbidden() throws Exception {
        mockMvc.perform(multipart("/api/postmortems/import").file(file())
                        .with(httpBasic(BASIC_USER, BASIC_PASSWORD)))
                .andExpect(status().isForbidden());

        verify(importService, never()).importFromDocument(any());
    }

    @Test
    void getWithBasicAuth_isAuthenticatedWithoutToken() throws Exception {
        when(postmortemService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/postmortems").with(httpBasic(BASIC_USER, BASIC_PASSWORD)))
                .andExpect(status().isOk());
    }

    // ---- Delete routes: POST only, CSRF protected ----

    @Test
    void deleteUser_getDoesNotDelete() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/users/delete/" + id).with(user("admin").roles("ADMIN")))
                .andExpect(status().isMethodNotAllowed());
        verify(userService, never()).deleteById(any());
    }

    @Test
    void deleteUser_postWithToken_deletes() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/users/delete/" + id).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"));
        verify(userService).deleteById(id);
    }

    @Test
    void deleteUser_postWithoutToken_isForbidden() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/users/delete/" + id).with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        verify(userService, never()).deleteById(any());
    }

    @Test
    void deleteUser_asNonAdmin_isForbidden() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/users/delete/" + id).with(user("u").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
        verify(userService, never()).deleteById(any());
    }

    @Test
    void deleteIncidentNote_getDoesNotDelete() throws Exception {
        mockMvc.perform(get("/incident-notes/delete/" + UUID.randomUUID()).with(user("u").roles("USER")))
                .andExpect(status().isMethodNotAllowed());
        verify(incidentNoteService, never()).deleteById(any());
    }

    @Test
    void deleteIncidentNote_postWithToken_deletes() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/incident-notes/delete/" + id).with(user("u").roles("USER")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incident-notes"));
        verify(incidentNoteService).deleteById(id);
    }

    @Test
    void deleteTemplate_getDoesNotDelete() throws Exception {
        mockMvc.perform(get("/templates/delete/" + UUID.randomUUID()).with(user("u").roles("USER")))
                .andExpect(status().isMethodNotAllowed());
        verify(reportTemplateService, never()).deleteById(any());
    }

    @Test
    void deleteTemplate_postWithToken_deletes() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/templates/delete/" + id).with(user("u").roles("USER")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/templates"));
        verify(reportTemplateService).deleteById(id);
    }

    @Test
    void deleteDataSource_getDoesNotDelete() throws Exception {
        mockMvc.perform(get("/datasources/delete/" + UUID.randomUUID()).with(user("u").roles("ADMIN")))
                .andExpect(status().isMethodNotAllowed());
        verify(dataSourceService, never()).deleteDataSource(any());
    }

    @Test
    void deleteDataSource_postWithToken_deletes() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/datasources/delete/" + id).with(user("u").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/datasources"));
        verify(dataSourceService).deleteDataSource(id);
    }
}
