package com.melomarit.melopost.service;

import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.model.PostmortemDocument;
import com.melomarit.melopost.repository.PostmortemDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostmortemDocumentServiceTest {

    @Mock
    private PostmortemDocumentRepository repository;

    @Mock
    private PostmortemService postmortemService;

    @InjectMocks
    private PostmortemDocumentService service;

    @Test
    void findInPostmortem_otherPostmortemsDocumentIsNotFound() {
        UUID docId = UUID.randomUUID();
        PostmortemDocument doc = new PostmortemDocument();
        doc.setPostmortemUuid(UUID.randomUUID());
        when(repository.findById(docId)).thenReturn(Optional.of(doc));
        org.springframework.web.server.ResponseStatusException e = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.findInPostmortem(UUID.randomUUID(), docId));
        assertEquals(404, e.getStatusCode().value());
    }

    @Test
    void findInPostmortem_returnsOwnDocument() {
        UUID docId = UUID.randomUUID();
        UUID pmId = UUID.randomUUID();
        PostmortemDocument doc = new PostmortemDocument();
        doc.setPostmortemUuid(pmId);
        when(repository.findById(docId)).thenReturn(Optional.of(doc));
        assertSame(doc, service.findInPostmortem(pmId, docId));
    }

    @Test
    void deleteAccessible_deniedPostmortemKeepsDocument() {
        UUID docId = UUID.randomUUID();
        UUID pmId = UUID.randomUUID();
        PostmortemDocument doc = new PostmortemDocument();
        doc.setPostmortemUuid(pmId);
        when(repository.findById(docId)).thenReturn(Optional.of(doc));
        when(postmortemService.findAuthorized(pmId, PostmortemAccessPolicy.Action.UPDATE))
                .thenThrow(new PostmortemLockedException(pmId));
        assertThrows(PostmortemLockedException.class, () -> service.deleteAccessible(docId));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void findAccessible_orphanDocumentIsHiddenFromRestrictedUsers() {
        UUID docId = UUID.randomUUID();
        when(repository.findById(docId)).thenReturn(Optional.of(new PostmortemDocument()));
        when(postmortemService.currentUserIsUnrestricted()).thenReturn(false);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.findAccessible(docId, PostmortemAccessPolicy.Action.VIEW));
    }

    @Test
    void findAllVisible_restrictedUserOnlySeesDocumentsOfVisiblePostmortems() {
        Postmortem visiblePm = new Postmortem();
        PostmortemDocument mine = new PostmortemDocument();
        mine.setPostmortemUuid(visiblePm.getUuid());
        PostmortemDocument other = new PostmortemDocument();
        other.setPostmortemUuid(UUID.randomUUID());
        when(repository.findAll()).thenReturn(Arrays.asList(mine, other));
        when(postmortemService.currentUserIsUnrestricted()).thenReturn(false);
        when(postmortemService.findAll()).thenReturn(Arrays.asList(visiblePm));
        assertEquals(List.of(mine), service.findAllVisible());
    }

    @Test
    void findAll_shouldReturnAll() {
        when(repository.findAll()).thenReturn(Arrays.asList(new PostmortemDocument()));
        List<PostmortemDocument> result = service.findAll();
        assertEquals(1, result.size());
    }

    @Test
    void findById_shouldReturnDocument() {
        UUID id = UUID.randomUUID();
        PostmortemDocument doc = new PostmortemDocument();
        when(repository.findById(id)).thenReturn(Optional.of(doc));
        Optional<PostmortemDocument> result = service.findById(id);
        assertTrue(result.isPresent());
    }

    @Test
    void findByPostmortemUuid_shouldReturnList() {
        UUID pmId = UUID.randomUUID();
        when(repository.findByPostmortemUuid(pmId)).thenReturn(Arrays.asList(new PostmortemDocument()));
        List<PostmortemDocument> result = service.findByPostmortemUuid(pmId);
        assertEquals(1, result.size());
    }

    @Test
    void uploadDocument_shouldSaveDocument() throws IOException {
        Postmortem pm = new Postmortem();
        pm.setUuid(UUID.randomUUID());
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        
        service.uploadDocument(pm, file);
        
        verify(repository).save(any(PostmortemDocument.class));
        assertNotNull(pm.getDocuments());
        assertEquals(1, pm.getDocuments().size());
        assertEquals("test.txt", pm.getDocuments().get(0).getFileName());
    }

    @Test
    void processFiles_shouldHandleMultipleFiles() throws IOException {
        Postmortem pm = new Postmortem();
        pm.setUuid(UUID.randomUUID());
        MockMultipartFile file1 = new MockMultipartFile("file1", "test1.txt", "text/plain", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file2", "test2.txt", "text/plain", "content2".getBytes());
        
        service.processFiles(pm, new MockMultipartFile[]{file1, file2});
        
        verify(repository, times(2)).save(any(PostmortemDocument.class));
        assertEquals(2, pm.getDocuments().size());
    }

    @Test
    void uploadDocument_refusesDisallowedExtensionAndStoresNothing() {
        Postmortem pm = new Postmortem();
        pm.setUuid(UUID.randomUUID());
        MockMultipartFile file = new MockMultipartFile("file", "page.html", "text/html", "x".getBytes());
        UnsupportedDocumentTypeException e = assertThrows(UnsupportedDocumentTypeException.class,
                () -> service.uploadDocument(pm, file));
        assertEquals(400, e.getStatusCode().value());
        verify(repository, never()).save(any());
        assertTrue(pm.getDocuments() == null || pm.getDocuments().isEmpty());
    }

    @Test
    void uploadDocument_refusesAllowedExtensionWithMismatchingDeclaredType() {
        Postmortem pm = new Postmortem();
        MockMultipartFile file = new MockMultipartFile("file", "x.png", "text/html", "x".getBytes());
        assertThrows(UnsupportedDocumentTypeException.class, () -> service.uploadDocument(pm, file));
        verify(repository, never()).save(any());
    }

    @Test
    void uploadDocument_storesCanonicalTypeAndStripsPath() throws IOException {
        Postmortem pm = new Postmortem();
        pm.setUuid(UUID.randomUUID());
        MockMultipartFile file = new MockMultipartFile("file", "C:\\fakepath\\data.csv", "application/vnd.ms-excel", "a,b".getBytes());
        service.uploadDocument(pm, file);
        assertEquals("data.csv", pm.getDocuments().get(0).getFileName());
        assertEquals("text/csv", pm.getDocuments().get(0).getContentType());
    }

    @Test
    void processFiles_refusesWholeBatchWhenOneFileIsDisallowed() {
        Postmortem pm = new Postmortem();
        MockMultipartFile ok = new MockMultipartFile("f", "ok.txt", "text/plain", "x".getBytes());
        MockMultipartFile bad = new MockMultipartFile("f", "bad.exe", "application/octet-stream", "x".getBytes());
        assertThrows(UnsupportedDocumentTypeException.class,
                () -> service.processFiles(pm, new MockMultipartFile[]{ok, bad}));
        verify(repository, never()).save(any());
    }

    @Test
    void validateUploads_ignoresNullAndEmptyFiles() {
        assertDoesNotThrow(() -> service.validateUploads(null));
        assertDoesNotThrow(() -> service.validateUploads(new MockMultipartFile[]{null,
                new MockMultipartFile("f", "", "text/html", new byte[0])}));
    }

    @Test
    void deleteById_shouldCallRepository() {
        UUID id = UUID.randomUUID();
        service.deleteById(id);
        verify(repository).deleteById(id);
    }
}
