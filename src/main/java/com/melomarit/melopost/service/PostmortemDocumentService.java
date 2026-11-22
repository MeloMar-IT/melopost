package com.melomarit.melopost.service;

import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.model.PostmortemDocument;
import com.melomarit.melopost.repository.PostmortemDocumentRepository;
import com.melomarit.melopost.service.PostmortemAccessPolicy.Action;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PostmortemDocumentService {
    private final PostmortemDocumentRepository repository;
    private final PostmortemService postmortemService;

    public PostmortemDocumentService(PostmortemDocumentRepository repository, PostmortemService postmortemService) {
        this.repository = repository;
        this.postmortemService = postmortemService;
    }

    /** All documents whose owning postmortem the current user may see. */
    public List<PostmortemDocument> findAllVisible() {
        List<PostmortemDocument> all = repository.findAll();
        if (postmortemService.currentUserIsUnrestricted()) {
            return all;
        }
        Set<UUID> visibleIds = postmortemService.findAll().stream()
                .map(Postmortem::getUuid)
                .collect(Collectors.toSet());
        return all.stream()
                .filter(d -> d.getPostmortemUuid() != null && visibleIds.contains(d.getPostmortemUuid()))
                .collect(Collectors.toList());
    }

    /** The document, after authorizing the action on its owning postmortem. 404 if missing or orphaned (non-admin). */
    public PostmortemDocument findAccessible(UUID docId, Action action) {
        PostmortemDocument doc = repository.findById(docId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        if (doc.getPostmortemUuid() == null) {
            if (!postmortemService.currentUserIsUnrestricted()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
            }
            return doc;
        }
        postmortemService.findAuthorized(doc.getPostmortemUuid(), action);
        return doc;
    }

    /** The document only if it belongs to the given postmortem; otherwise 404. */
    public PostmortemDocument findInPostmortem(UUID postmortemId, UUID docId) {
        return repository.findById(docId)
                .filter(d -> postmortemId.equals(d.getPostmortemUuid()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    public void deleteAccessible(UUID docId) {
        findAccessible(docId, Action.UPDATE);
        repository.deleteById(docId);
    }

    public List<PostmortemDocument> findAll() {
        return repository.findAll();
    }

    public Optional<PostmortemDocument> findById(UUID id) {
        return repository.findById(id);
    }

    public List<PostmortemDocument> findByPostmortemUuid(UUID postmortemUuid) {
        return repository.findByPostmortemUuid(postmortemUuid);
    }

    public PostmortemDocument save(PostmortemDocument document) {
        return repository.save(document);
    }

    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    public void uploadDocument(Postmortem postmortem, MultipartFile file) throws IOException {
        if (!file.isEmpty()) {
            PostmortemDocument doc = new PostmortemDocument();
            doc.setFileName(file.getOriginalFilename());
            doc.setContentType(file.getContentType());
            doc.setSize(file.getSize());
            doc.setData(file.getBytes());
            doc.setPostmortemUuid(postmortem.getUuid());
            doc.setUploadDate(java.time.LocalDateTime.now());
            pmAddDocument(postmortem, doc);
            repository.save(doc);
        }
    }

    public void processFiles(Postmortem postmortem, MultipartFile[] files) throws IOException {
        if (files != null) {
            for (MultipartFile file : files) {
                uploadDocument(postmortem, file);
            }
        }
    }

    private void pmAddDocument(Postmortem postmortem, PostmortemDocument doc) {
        if (postmortem.getDocuments() == null) {
            postmortem.setDocuments(new java.util.ArrayList<>());
        }
        postmortem.getDocuments().add(doc);
    }
}
