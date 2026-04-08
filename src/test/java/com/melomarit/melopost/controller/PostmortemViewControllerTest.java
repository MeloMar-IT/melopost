package com.melomarit.melopost.controller;

import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.service.DataSourceService;
import com.melomarit.melopost.service.PostmortemDocumentService;
import com.melomarit.melopost.service.PostmortemService;
import com.melomarit.melopost.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostmortemViewController.class)
public class PostmortemViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostmortemService postmortemService;

    @MockBean
    private ReportService reportService;

    @MockBean
    private PostmortemDocumentService documentService;

    @MockBean
    private DataSourceService dataSourceService;

    @Test
    @WithMockUser
    public void testViewDetails() throws Exception {
        Postmortem pm = new Postmortem();
        pm.setId(1L);
        pm.setTitle("Test Postmortem");
        
        when(postmortemService.findById(anyLong())).thenReturn(pm);

        mockMvc.perform(get("/postmortems/details/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("postmortems/details"))
                .andExpect(model().attributeExists("postmortem"));
    }
}
