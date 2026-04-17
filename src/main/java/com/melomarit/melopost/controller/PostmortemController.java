package com.melomarit.melopost.controller;

import com.melomarit.melopost.dto.PostmortemSearchResultDTO;
import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.service.DocumentImportService;
import com.melomarit.melopost.service.PostmortemService;
import com.melomarit.melopost.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/postmortems")
public class PostmortemController {
    private final PostmortemService service;
    private final ReportService reportService;
    private final DocumentImportService importService;

    public PostmortemController(PostmortemService service, ReportService reportService, DocumentImportService importService) {
        this.service = service;
        this.reportService = reportService;
        this.importService = importService;
    }

    @PostMapping("/import")
    public ResponseEntity<Postmortem> importFromDocument(@RequestParam("file") MultipartFile file) {
        try {
            Postmortem pm = importService.importFromDocument(file);
            return ResponseEntity.ok(pm);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public List<Postmortem> getAll() {
        return service.findAll();
    }

    @GetMapping("/recent")
    public List<Postmortem> getRecent() {
        return service.findRecent();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Postmortem> getById(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(service.findById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public Postmortem create(@RequestBody Postmortem postmortem) {
        return service.save(postmortem);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Postmortem> update(@PathVariable UUID id, @RequestBody Postmortem postmortem) {
        try {
            Postmortem existing = service.findById(id);
            if (postmortem.getTitle() != null) existing.setTitle(postmortem.getTitle());
            if (postmortem.getDescription() != null) existing.setDescription(postmortem.getDescription());
            if (postmortem.getStartDate() != null) existing.setStartDate(postmortem.getStartDate());
            if (postmortem.getIncidentDate() != null) existing.setIncidentDate(postmortem.getIncidentDate());
            if (postmortem.getPostMortemMeetingDate() != null) existing.setPostMortemMeetingDate(postmortem.getPostMortemMeetingDate());
            if (postmortem.getDueDate() != null) existing.setDueDate(postmortem.getDueDate());
            if (postmortem.getIncidentRef() != null) existing.setIncidentRef(postmortem.getIncidentRef());
            if (postmortem.getIncidentSource() != null) existing.setIncidentSource(postmortem.getIncidentSource());
            if (postmortem.getStoryStore() != null) existing.setStoryStore(postmortem.getStoryStore());
            if (postmortem.getDepartment() != null) existing.setDepartment(postmortem.getDepartment());
            if (postmortem.getFailedApplication() != null) existing.setFailedApplication(postmortem.getFailedApplication());
            if (postmortem.getType() != null) existing.setType(postmortem.getType());
            if (postmortem.getNote() != null) existing.setNote(postmortem.getNote());
            
            if (postmortem.getTags() != null) {
                if (existing.getTags() == null) {
                    existing.setTags(new java.util.ArrayList<>());
                }
                existing.getTags().clear();
                existing.getTags().addAll(postmortem.getTags());
            }

            if (postmortem.getLayers() != null) {
                if (existing.getLayers() == null) {
                    existing.setLayers(new java.util.ArrayList<>());
                }
                existing.getLayers().clear();
                existing.getLayers().addAll(postmortem.getLayers());
            }
            if (postmortem.getTimelineEvents() != null) {
                if (existing.getTimelineEvents() == null) {
                    existing.setTimelineEvents(new java.util.ArrayList<>());
                }
                existing.getTimelineEvents().clear();
                existing.getTimelineEvents().addAll(postmortem.getTimelineEvents());
            }

            if (postmortem.getQuestions() != null) {
                if (existing.getQuestions() == null) {
                    existing.setQuestions(new java.util.ArrayList<>());
                }
                existing.getQuestions().clear();
                existing.getQuestions().addAll(postmortem.getQuestions());
            }
            
            return ResponseEntity.ok(service.save(existing));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public List<PostmortemSearchResultDTO> search(@RequestParam String keyword) {
        return service.search(keyword);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<byte[]> downloadReport(@PathVariable UUID id) {
        try {
            Postmortem postmortem = service.findById(id);
            byte[] pdfBytes = reportService.generatePostmortemPdf(postmortem);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=postmortem-report-" + id + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
