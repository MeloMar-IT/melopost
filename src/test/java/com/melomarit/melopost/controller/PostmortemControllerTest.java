package com.melomarit.melopost.controller;

import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.service.DocumentImportService;
import com.melomarit.melopost.service.PostmortemAccessDeniedException;
import com.melomarit.melopost.service.PostmortemAccessPolicy.Action;
import com.melomarit.melopost.service.PostmortemLockedException;
import com.melomarit.melopost.service.PostmortemService;
import com.melomarit.melopost.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostmortemControllerTest {

    @Mock
    private PostmortemService service;

    @Mock
    private ReportService reportService;

    @Mock
    private DocumentImportService importService;

    @InjectMocks
    private PostmortemController controller;

    private UUID pmId;
    private Postmortem postmortem;

    @BeforeEach
    void setUp() {
        pmId = UUID.randomUUID();
        postmortem = new Postmortem();
        postmortem.setUuid(pmId);
        postmortem.setTitle("Test Title");
    }

    @Test
    void getAll_shouldReturnList() {
        when(service.findAll()).thenReturn(Arrays.asList(postmortem));
        List<Postmortem> result = controller.getAll();
        assertEquals(1, result.size());
    }

    @Test
    void getById_shouldReturnPostmortem() {
        when(service.findById(pmId)).thenReturn(postmortem);
        ResponseEntity<Postmortem> result = controller.getById(pmId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(postmortem, result.getBody());
    }

    @Test
    void getById_accessDeniedIsNotSwallowed() {
        when(service.findById(pmId)).thenThrow(new PostmortemAccessDeniedException("denied"));
        assertThrows(PostmortemAccessDeniedException.class, () -> controller.getById(pmId));
    }

    @Test
    void update_lockedIsNotSwallowed() {
        when(service.findAuthorized(pmId, Action.UPDATE)).thenThrow(new PostmortemLockedException(pmId));
        assertThrows(PostmortemLockedException.class, () -> controller.update(pmId, new Postmortem()));
        verify(service, never()).save(any(Postmortem.class));
    }

    @Test
    void downloadReport_accessDeniedIsNotTurnedInto500() {
        when(service.findById(pmId)).thenThrow(new PostmortemAccessDeniedException("denied"));
        assertThrows(PostmortemAccessDeniedException.class, () -> controller.downloadReport(pmId));
    }

    @Test
    void importFromDocument_accessDeniedIsNotTurnedInto500() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.docx", "application/octet-stream", "data".getBytes());
        when(importService.importFromDocument(file)).thenThrow(new PostmortemAccessDeniedException("denied"));
        assertThrows(PostmortemAccessDeniedException.class, () -> controller.importFromDocument(file));
    }

    @Test
    void create_shouldCreatePostmortem() {
        when(service.create(any(Postmortem.class))).thenReturn(postmortem);
        Postmortem result = controller.create(new Postmortem());
        assertNotNull(result);
        verify(service).create(any(Postmortem.class));
        verify(service, never()).save(any(Postmortem.class));
    }

    @Test
    void update_shouldUpdateExisting() {
        when(service.findAuthorized(pmId, Action.UPDATE)).thenReturn(postmortem);
        when(service.save(any(Postmortem.class))).thenReturn(postmortem);
        
        Postmortem updateData = new Postmortem();
        updateData.setTitle("New Title");
        
        ResponseEntity<Postmortem> result = controller.update(pmId, updateData);
        
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("New Title", postmortem.getTitle());
    }

    @Test
    void delete_shouldCallService() {
        ResponseEntity<Void> result = controller.delete(pmId);
        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(service).deleteById(pmId);
    }

    @Test
    void importFromDocument_shouldCallImportService() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "data".getBytes());
        when(importService.importFromDocument(file)).thenReturn(postmortem);
        
        ResponseEntity<Postmortem> result = controller.importFromDocument(file);
        
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(importService).importFromDocument(file);
    }
}
