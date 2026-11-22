package com.melomarit.melopost.controller;

import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.model.PostmortemDocument;
import com.melomarit.melopost.service.PostmortemAccessPolicy.Action;
import com.melomarit.melopost.service.PostmortemDocumentService;
import com.melomarit.melopost.service.PostmortemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/postmortem-documents")
public class PostmortemDocumentController {
    private final PostmortemDocumentService service;
    private final PostmortemService postmortemService;

    public PostmortemDocumentController(PostmortemDocumentService service, PostmortemService postmortemService) {
        this.service = service;
        this.postmortemService = postmortemService;
    }

    @GetMapping
    public List<PostmortemDocument> getAll() {
        return service.findAllVisible();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostmortemDocument> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findAccessible(id, Action.VIEW));
    }

    @GetMapping("/postmortem/{id}")
    public List<PostmortemDocument> getByPostmortem(@PathVariable UUID id) {
        postmortemService.findById(id);
        return service.findByPostmortemUuid(id);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        return DocumentDownloadResponse.of(service.findAccessible(id, Action.VIEW));
    }

    @PostMapping("/upload/{postmortemId}")
    public ResponseEntity<PostmortemDocument> upload(@PathVariable UUID postmortemId, @RequestParam("file") MultipartFile file) throws IOException {
        Postmortem pm = postmortemService.findAuthorized(postmortemId, Action.UPDATE);
        service.uploadDocument(pm, file);
        postmortemService.save(pm);
        
        // Return the last added document
        List<PostmortemDocument> docs = pm.getDocuments();
        if (docs != null && !docs.isEmpty()) {
            return ResponseEntity.ok(docs.get(docs.size() - 1));
        }
        return ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteAccessible(id);
        return ResponseEntity.noContent().build();
    }
}
