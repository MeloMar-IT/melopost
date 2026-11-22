package com.melomarit.melopost.controller;

import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.model.PostmortemDocument;
import com.melomarit.melopost.service.PostmortemAccessDeniedException;
import com.melomarit.melopost.service.PostmortemAccessPolicy.Action;
import com.melomarit.melopost.service.PostmortemLockedException;
import com.melomarit.melopost.service.PostmortemDocumentService;
import com.melomarit.melopost.service.PostmortemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostmortemDocumentControllerTest {

    @Mock
    private PostmortemDocumentService service;

    @Mock
    private PostmortemService postmortemService;

    @InjectMocks
    private PostmortemDocumentController controller;

    private UUID docId;
    private PostmortemDocument doc;

    @BeforeEach
    void setUp() {
        docId = UUID.randomUUID();
        doc = new PostmortemDocument();
        doc.setUuid(docId);
        doc.setFileName("test.txt");
        doc.setContentType("text/plain");
        doc.setData("content".getBytes());
    }

    @Test
    void getAll_shouldReturnList() {
        when(service.findAllVisible()).thenReturn(Arrays.asList(doc));
        List<PostmortemDocument> result = controller.getAll();
        assertEquals(1, result.size());
    }

    @Test
    void getById_shouldReturnDocument() {
        when(service.findAccessible(docId, Action.VIEW)).thenReturn(doc);
        ResponseEntity<PostmortemDocument> result = controller.getById(docId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(doc, result.getBody());
    }

    @Test
    void getById_accessDeniedIsNotSwallowed() {
        when(service.findAccessible(docId, Action.VIEW)).thenThrow(new PostmortemAccessDeniedException("denied"));
        assertThrows(PostmortemAccessDeniedException.class, () -> controller.getById(docId));
    }

    @Test
    void getByPostmortem_checksOwningPostmortem() {
        UUID pmId = UUID.randomUUID();
        when(postmortemService.findById(pmId)).thenThrow(new PostmortemAccessDeniedException("denied"));
        assertThrows(PostmortemAccessDeniedException.class, () -> controller.getByPostmortem(pmId));
        verify(service, never()).findByPostmortemUuid(any());
    }

    @Test
    void download_shouldReturnData() {
        when(service.findAccessible(docId, Action.VIEW)).thenReturn(doc);
        ResponseEntity<byte[]> result = controller.download(docId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertArrayEquals(doc.getData(), result.getBody());
    }

    @Test
    void upload_lockedPostmortemIsRejectedBeforeStoringAnything() throws IOException {
        UUID pmId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        when(postmortemService.findAuthorized(pmId, Action.UPDATE)).thenThrow(new PostmortemLockedException(pmId));
        assertThrows(PostmortemLockedException.class, () -> controller.upload(pmId, file));
        verify(service, never()).uploadDocument(any(), any());
    }

    @Test
    void upload_shouldCallServices() throws IOException {
        UUID pmId = UUID.randomUUID();
        Postmortem pm = new Postmortem();
        pm.setUuid(pmId);
        pm.setDocuments(new ArrayList<>());
        pm.getDocuments().add(doc);
        
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        when(postmortemService.findAuthorized(pmId, Action.UPDATE)).thenReturn(pm);

        ResponseEntity<PostmortemDocument> result = controller.upload(pmId, file);
        
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(service).uploadDocument(pm, file);
        verify(postmortemService).save(pm);
    }

    @Test
    void delete_shouldCallService() {
        ResponseEntity<Void> result = controller.delete(docId);
        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(service).deleteAccessible(docId);
    }
}
