package com.melomarit.melopost.controller;

import com.melomarit.melopost.dto.HoleDTO;
import com.melomarit.melopost.dto.StoryDTO;
import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.model.StoryUDT;
import com.melomarit.melopost.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.mock.web.MockMultipartFile;
import java.util.Collections;
import java.util.UUID;

@WebMvcTest(PostmortemViewController.class)
public class PostmortemViewControllerTest {

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

    @MockitoBean
    private IncidentNoteService incidentNoteService;

    @Test
    @WithMockUser
    public void testViewDetails() throws Exception {
        UUID id = UUID.randomUUID();
        Postmortem pm = new Postmortem();
        pm.setUuid(id);
        pm.setTitle("Test Postmortem");
        
        when(postmortemService.findById(any(UUID.class))).thenReturn(pm);

        mockMvc.perform(get("/postmortems/details/" + id))
                .andExpect(status().isOk())
                .andExpect(view().name("postmortems/details"))
                .andExpect(model().attributeExists("postmortem"))
                .andExpect(model().attributeExists("allPostmortems"));
    }

    @Test
    @WithMockUser
    public void testLinkPostmortem() throws Exception {
        UUID id1 = UUID.randomUUID();
        Postmortem pm1 = new Postmortem();
        pm1.setUuid(id1);
        UUID id2 = UUID.randomUUID();
        Postmortem pm2 = new Postmortem();
        pm2.setUuid(id2);

        when(postmortemService.findById(id1)).thenReturn(pm1);
        when(postmortemService.findById(id2)).thenReturn(pm2);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/postmortems/" + id1 + "/link")
                .param("linkedId", id2.toString())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/postmortems/details/" + id1));
    }

    @Test
    @WithMockUser
    public void testUnlinkPostmortem() throws Exception {
        UUID id1 = UUID.randomUUID();
        Postmortem pm1 = new Postmortem();
        pm1.setUuid(id1);
        UUID id2 = UUID.randomUUID();
        Postmortem pm2 = new Postmortem();
        pm2.setUuid(id2);
        pm1.getLocalPostmortemUuids().add(id2);

        when(postmortemService.findById(id1)).thenReturn(pm1);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/postmortems/" + id1 + "/unlink")
                .param("linkedId", id2.toString())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/postmortems/details/" + id1));
    }

    @Test
    @WithMockUser
    public void testShowNote() throws Exception {
        UUID id = UUID.randomUUID();
        Postmortem pm = new Postmortem();
        pm.setUuid(id);
        pm.setTitle("Test Postmortem");
        
        when(postmortemService.findById(any(UUID.class))).thenReturn(pm);

        mockMvc.perform(get("/postmortems/" + id + "/note"))
                .andExpect(status().isOk())
                .andExpect(view().name("postmortems/note"))
                .andExpect(model().attributeExists("postmortem"));
    }

    @Test
    @WithMockUser
    public void testSaveNote() throws Exception {
        UUID id = UUID.randomUUID();
        Postmortem pm = new Postmortem();
        pm.setUuid(id);
        
        when(postmortemService.findById(any(UUID.class))).thenReturn(pm);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/postmortems/" + id + "/note")
                .param("note", "New note content")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/postmortems/details/" + id));
        
        verify(postmortemService).save(argThat(savedPm -> "New note content".equals(savedPm.getNote())));
    }

    @Test
    @WithMockUser
    public void testSaveNoteTruncation() throws Exception {
        UUID id = UUID.randomUUID();
        Postmortem pm = new Postmortem();
        pm.setUuid(id);
        
        when(postmortemService.findById(any(UUID.class))).thenReturn(pm);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000010; i++) {
            sb.append("a");
        }
        String longNote = sb.toString();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/postmortems/" + id + "/note")
                .param("note", longNote)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/postmortems/details/" + id));
        
        verify(postmortemService).save(argThat(savedPm -> savedPm.getNote().length() == 1000000));
    }

    @Test
    @WithMockUser
    public void testUploadDocument() throws Exception {
        UUID id = UUID.randomUUID();
        Postmortem pm = new Postmortem();
        pm.setUuid(id);
        
        when(postmortemService.findById(any(UUID.class))).thenReturn(pm);
        
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test data".getBytes());

        mockMvc.perform(multipart("/postmortems/" + id + "/documents/upload")
                .file(file)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/postmortems/details/" + id));
        
        verify(documentService, times(1)).uploadDocument(any(Postmortem.class), any());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    public void testSaveLockedPostmortemFailsForNonAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        Postmortem pm = new Postmortem();
        pm.setUuid(id);
        pm.setStatus("Published");
        
        when(postmortemService.findById(id)).thenReturn(pm);
        when(postmortemService.isUserAdmin(any())).thenReturn(false);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/postmortems/save")
                .flashAttr("postmortem", pm)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/postmortems/details/" + id + "?error=locked"));
    }

    @Test
    @WithMockUser
    public void testListHoles() throws Exception {
        HoleDTO hole = new HoleDTO();
        hole.setDescription("Test Hole");
        hole.setTeamName("Test Team");
        hole.setActionStatus("OPEN");
        
        when(postmortemService.findAllHoles(any(), any(), any())).thenReturn(java.util.List.of(hole));

        mockMvc.perform(get("/postmortems/holes"))
                .andExpect(status().isOk())
                .andExpect(view().name("holes/list"))
                .andExpect(model().attributeExists("holes"))
                .andExpect(model().attributeExists("teamNames"))
                .andExpect(model().attributeExists("statuses"));
    }

    @Test
    @WithMockUser
    public void testListStories() throws Exception {
        StoryUDT storyUdt = new StoryUDT();
        storyUdt.setStoryNumber("STORY-123");
        storyUdt.setTeamName("Test Team");
        storyUdt.setStatus("OPEN");

        StoryDTO story = new StoryDTO(storyUdt, new Postmortem(), "Test Hole");
        
        when(postmortemService.findAllStories(any(), any(), any())).thenReturn(java.util.List.of(story));

        mockMvc.perform(get("/postmortems/stories"))
                .andExpect(status().isOk())
                .andExpect(view().name("stories/list"))
                .andExpect(model().attributeExists("stories"))
                .andExpect(model().attributeExists("teamNames"))
                .andExpect(model().attributeExists("statuses"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testSaveLockedPostmortemSucceedsForAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        Postmortem pm = new Postmortem();
        pm.setUuid(id);
        pm.setStatus("Published");
        
        when(postmortemService.findById(id)).thenReturn(pm);
        when(postmortemService.isUserAdmin(any())).thenReturn(true);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/postmortems/save")
                .flashAttr("postmortem", pm)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/postmortems"));
        
        verify(postmortemService, times(1)).save(any(Postmortem.class));
    }
}
