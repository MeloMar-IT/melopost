package com.melomarit.melopost.controller;

import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.model.PostmortemDocument;
import com.melomarit.melopost.service.PostmortemDocumentService;
import com.melomarit.melopost.service.PostmortemService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostmortemDocument> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/postmortem/{id}")
    public List<PostmortemDocument> getByPostmortem(@PathVariable Long id) {
        return service.findByPostmortemId(id);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        return service.findById(id)
                .map(doc -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                        .contentType(MediaType.parseMediaType(doc.getContentType()))
                        .body(doc.getData()))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/upload/{postmortemId}")
    public ResponseEntity<PostmortemDocument> upload(@PathVariable Long postmortemId, @RequestParam("file") MultipartFile file) throws IOException {
        Postmortem pm = postmortemService.findById(postmortemId);
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
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
