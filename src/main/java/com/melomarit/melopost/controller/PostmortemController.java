package com.melomarit.melopost.controller;

import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.service.PostmortemService;
import com.melomarit.melopost.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/postmortems")
public class PostmortemController {
    private final PostmortemService service;
    private final ReportService reportService;

    public PostmortemController(PostmortemService service, ReportService reportService) {
        this.service = service;
        this.reportService = reportService;
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
    public ResponseEntity<Postmortem> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public Postmortem create(@RequestBody Postmortem postmortem) {
        return service.save(postmortem);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Postmortem> update(@PathVariable Long id, @RequestBody Postmortem postmortem) {
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
                existing.getTags().clear();
                existing.getTags().addAll(postmortem.getTags());
            }

            if (postmortem.getLayers() != null) {
                existing.getLayers().clear();
                existing.getLayers().addAll(postmortem.getLayers());
            }
            if (postmortem.getTimelineEvents() != null) {
                existing.getTimelineEvents().clear();
                existing.getTimelineEvents().addAll(postmortem.getTimelineEvents());
            }

            if (postmortem.getQuestions() != null) {
                existing.getQuestions().clear();
                existing.getQuestions().addAll(postmortem.getQuestions());
                for (var q : existing.getQuestions()) {
                    q.setPostmortem(existing);
                }
            }
            
            return ResponseEntity.ok(service.save(existing));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public List<Postmortem> search(@RequestParam String keyword) {
        return service.search(keyword);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<byte[]> downloadReport(@PathVariable Long id) {
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
