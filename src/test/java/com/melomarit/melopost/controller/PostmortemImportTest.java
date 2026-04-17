package com.melomarit.melopost.controller;

import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.service.DocumentImportService;
import com.melomarit.melopost.service.PostmortemService;
import com.melomarit.melopost.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import java.util.UUID;

@WebMvcTest(PostmortemController.class)
public class PostmortemImportTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostmortemService postmortemService;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private DocumentImportService importService;

    @Test
    @WithMockUser
    public void testImportFromDocument() throws Exception {
        UUID id = UUID.randomUUID();
        Postmortem pm = new Postmortem();
        pm.setUuid(id);
        pm.setTitle("Imported PM");

        when(importService.importFromDocument(any())).thenReturn(pm);

        MockMultipartFile file = new MockMultipartFile("file", "test.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "test content".getBytes());

        mockMvc.perform(multipart("/api/postmortems/import")
                .file(file)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(id.toString()))
                .andExpect(jsonPath("$.title").value("Imported PM"));
    }
}
