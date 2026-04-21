package com.melomarit.melopost.controller;

import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.service.DataSourceService;
import com.melomarit.melopost.service.PostmortemDocumentService;
import com.melomarit.melopost.service.PostmortemService;
import com.melomarit.melopost.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostmortemViewController.class)
public class PostmortemNullSafetyTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostmortemService postmortemService;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private PostmortemDocumentService documentService;

    @MockitoBean
    private DataSourceService dataSourceService;

    @Test
    @WithMockUser
    public void testViewDetailsWithNullCollections() throws Exception {
        UUID id = UUID.randomUUID();
        Postmortem pm = new Postmortem();
        pm.setUuid(id);
        pm.setTitle("Null Safety Test");
        
        // Use reflection to force collections to null, bypassing the defensive getters
        // (though in practice they are null before the getter is called)
        try {
            forceNull(pm, "layers");
            forceNull(pm, "questions");
            forceNull(pm, "tags");
            forceNull(pm, "timelineEvents");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        when(postmortemService.findById(any(UUID.class))).thenReturn(pm);

        // This should not throw SpelEvaluationException because of our fixes
        mockMvc.perform(get("/postmortems/details/" + id))
                .andExpect(status().isOk())
                .andExpect(view().name("postmortems/details"))
                .andExpect(model().attributeExists("postmortem"));
    }
    
    private void forceNull(Object obj, String fieldName) throws Exception {
        Field field = Postmortem.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, null);
    }
}
