package com.melomarit.melopost.controller;

import com.melomarit.melopost.dto.HoleDTO;
import com.melomarit.melopost.dto.StoryDTO;
import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.model.StoryUDT;
import com.melomarit.melopost.service.*;
import com.melomarit.melopost.service.PostmortemAccessPolicy.Action;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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

        when(postmortemService.findAuthorized(id1, Action.UPDATE)).thenReturn(pm1);
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

        when(postmortemService.findAuthorized(id1, Action.UPDATE)).thenReturn(pm1);

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

        when(postmortemService.findAuthorized(any(UUID.class), eq(Action.UPDATE))).thenReturn(pm);

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

        when(postmortemService.findAuthorized(any(UUID.class), eq(Action.UPDATE))).thenReturn(pm);

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

        when(postmortemService.findAuthorized(any(UUID.class), eq(Action.UPDATE))).thenReturn(pm);

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
        
        when(postmortemService.findAuthorized(id, Action.UPDATE)).thenThrow(new PostmortemLockedException(id));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/postmortems/save")
                .flashAttr("postmortem", pm)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/postmortems/details/" + id + "?error=locked"));
        verify(postmortemService, never()).save(any(Postmortem.class));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    public void testSaveOtherDepartmentIsForbiddenAndStoresNoFiles() throws Exception {
        UUID id = UUID.randomUUID();
        Postmortem pm = new Postmortem();
        pm.setUuid(id);
        when(postmortemService.findAuthorized(id, Action.UPDATE)).thenThrow(new PostmortemAccessDeniedException("denied"));

        mockMvc.perform(multipart("/postmortems/save")
                .file(new MockMultipartFile("files", "a.txt", "text/plain", "x".getBytes()))
                .flashAttr("postmortem", pm)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());
        verify(postmortemService, never()).save(any(Postmortem.class));
        verify(documentService, never()).processFiles(any(), any());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    public void testSaveWithUnknownUuidIsNotFoundAndNotRecreated() throws Exception {
        UUID id = UUID.randomUUID();
        Postmortem pm = new Postmortem();
        pm.setUuid(id);
        when(postmortemService.findAuthorized(id, Action.UPDATE))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/postmortems/save")
                .flashAttr("postmortem", pm)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound());
        verify(postmortemService, never()).save(any(Postmortem.class));
        verify(postmortemService, never()).create(any(Postmortem.class));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    public void testDuplicateCreateThenCorrectedRetrySucceeds() throws Exception {
        // First attempt: a new postmortem (no uuid) with a duplicate incident ref.
        Postmortem first = new Postmortem();
        first.setUuid(null); // the new-postmortem form posts an empty uuid
        first.setIncidentRef("INC-1");
        UUID generated = UUID.randomUUID();
        when(postmortemService.create(any(Postmortem.class)))
                .thenAnswer(inv -> {
                    Postmortem p = inv.getArgument(0);
                    p.setUuid(generated); // create() assigns the uuid before the save is rejected
                    throw new DuplicateIncidentRefException("INC-1");
                })
                .thenAnswer(inv -> inv.getArgument(0));

        MvcResult rejected = mockMvc.perform(post("/postmortems/save")
                        .flashAttr("postmortem", first)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("postmortems/form"))
                .andReturn();
        Postmortem redisplayed = (Postmortem) rejected.getModelAndView().getModel().get("postmortem");
        assertNull(redisplayed.getUuid(), "the form must not carry a uuid for a create that was not stored");

        // Corrected resubmission: the form posts the (now empty) uuid, so it is still a create.
        Postmortem corrected = new Postmortem();
        corrected.setUuid(null);
        corrected.setIncidentRef("INC-2");
        mockMvc.perform(post("/postmortems/save")
                        .flashAttr("postmortem", corrected)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/postmortems"));
        verify(postmortemService, times(2)).create(any(Postmortem.class));
        verify(postmortemService, never()).findAuthorized(any(), any());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    public void testSaveWithoutUuidCreatesNewPostmortem() throws Exception {
        Postmortem pm = new Postmortem();
        pm.setUuid(null);
        when(postmortemService.create(any(Postmortem.class))).thenReturn(pm);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/postmortems/save")
                .flashAttr("postmortem", pm)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/postmortems"));
        verify(postmortemService).create(any(Postmortem.class));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    public void testDetailsOtherDepartmentIsForbidden() throws Exception {
        UUID id = UUID.randomUUID();
        when(postmortemService.findById(id)).thenThrow(new PostmortemAccessDeniedException("denied"));
        mockMvc.perform(get("/postmortems/details/" + id)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    public void testReportOtherDepartmentIsForbiddenNotServerError() throws Exception {
        UUID id = UUID.randomUUID();
        when(postmortemService.findById(id)).thenThrow(new PostmortemAccessDeniedException("denied"));
        mockMvc.perform(get("/postmortems/" + id + "/report")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    public void testNoteOnPublishedRedirectsLockedAndSavesNothing() throws Exception {
        UUID id = UUID.randomUUID();
        when(postmortemService.findAuthorized(id, Action.UPDATE)).thenThrow(new PostmortemLockedException(id));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/postmortems/" + id + "/note")
                .param("note", "x")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/postmortems/details/" + id + "?error=locked"));
        verify(postmortemService, never()).save(any(Postmortem.class));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    public void testDocumentOfOtherPostmortemIsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        when(documentService.findInPostmortem(id, docId))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/postmortems/" + id + "/documents/" + docId + "/download")).andExpect(status().isNotFound());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/postmortems/" + id + "/documents/" + docId + "/delete")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound());
        verify(documentService, never()).deleteById(any());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    public void testEditPublishedRedirectsLocked() throws Exception {
        UUID id = UUID.randomUUID();
        when(postmortemService.findAuthorized(id, Action.UPDATE)).thenThrow(new PostmortemLockedException(id));
        mockMvc.perform(get("/postmortems/edit/" + id))
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
        
        when(postmortemService.findAuthorized(id, Action.UPDATE)).thenReturn(pm);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/postmortems/save")
                .flashAttr("postmortem", pm)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/postmortems"));
        
        verify(postmortemService, times(1)).save(any(Postmortem.class));
    }
}
