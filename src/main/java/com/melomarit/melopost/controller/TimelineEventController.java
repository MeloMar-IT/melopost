package com.melomarit.melopost.controller;

import com.melomarit.melopost.model.TimelineEvent;
import com.melomarit.melopost.service.TimelineEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/timeline-events")
public class TimelineEventController {
    @Autowired
    private TimelineEventService service;

    @GetMapping
    public List<TimelineEvent> getAll() { return service.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<TimelineEvent> getById(@PathVariable Long id) {
        return service.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public TimelineEvent create(@RequestBody TimelineEvent event) { return service.save(event); }

    @PutMapping("/{id}")
    public ResponseEntity<TimelineEvent> update(@PathVariable Long id, @RequestBody TimelineEvent event) {
        return service.findById(id).map(existing -> {
            if (event.getEventTime() != null) existing.setEventTime(event.getEventTime());
            if (event.getDescription() != null) existing.setDescription(event.getDescription());
            return ResponseEntity.ok(service.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (service.findById(id).isPresent()) {
            service.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
