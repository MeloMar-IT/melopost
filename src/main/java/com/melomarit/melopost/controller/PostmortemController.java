package com.melomarit.melopost.controller;

import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.service.PostmortemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/postmortems")
public class PostmortemController {
    private final PostmortemService service;

    public PostmortemController(PostmortemService service) {
        this.service = service;
    }

    @GetMapping
    public List<Postmortem> getAll() {
        return service.findAll();
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
            if (postmortem.getStoryApplication() != null) existing.setStoryApplication(postmortem.getStoryApplication());
            if (postmortem.getDepartment() != null) existing.setDepartment(postmortem.getDepartment());
            if (postmortem.getFailedApplication() != null) existing.setFailedApplication(postmortem.getFailedApplication());
            
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
}
