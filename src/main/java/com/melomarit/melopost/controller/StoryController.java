package com.melomarit.melopost.controller;

import com.melomarit.melopost.model.Story;
import com.melomarit.melopost.service.StoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stories")
public class StoryController {
    @Autowired
    private StoryService service;

    @GetMapping
    public List<Story> getAll() { return service.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Story> getById(@PathVariable Long id) {
        return service.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Story create(@RequestBody Story story) { return service.save(story); }

    @PutMapping("/{id}")
    public ResponseEntity<Story> update(@PathVariable Long id, @RequestBody Story story) {
        return service.findById(id).map(existing -> {
            if (story.getStoryNumber() != null) existing.setStoryNumber(story.getStoryNumber());
            if (story.getTeamName() != null) existing.setTeamName(story.getTeamName());
            if (story.getBacklogName() != null) existing.setBacklogName(story.getBacklogName());
            if (story.getPlatform() != null) existing.setPlatform(story.getPlatform());
            if (story.getWhatToFix() != null) existing.setWhatToFix(story.getWhatToFix());
            if (story.getFoundByDepartment() != null) existing.setFoundByDepartment(story.getFoundByDepartment());
            if (story.getToSolveByDepartment() != null) existing.setToSolveByDepartment(story.getToSolveByDepartment());
            if (story.getPriority() != null) existing.setPriority(story.getPriority());
            if (story.getManagerName() != null) existing.setManagerName(story.getManagerName());
            if (story.getStoryLink() != null) existing.setStoryLink(story.getStoryLink());
            if (story.getStatus() != null) existing.setStatus(story.getStatus());
            if (story.getNotes() != null) existing.setNotes(story.getNotes());
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
