package com.melomarit.melopost.controller;

import com.melomarit.melopost.model.Hole;
import com.melomarit.melopost.service.HoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/holes")
public class HoleController {
    @Autowired
    private HoleService service;

    @GetMapping
    public List<Hole> getAll() { return service.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Hole> getById(@PathVariable Long id) {
        return service.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Hole create(@RequestBody Hole hole) { return service.save(hole); }

    @PutMapping("/{id}")
    public ResponseEntity<Hole> update(@PathVariable Long id, @RequestBody Hole hole) {
        return service.findById(id).map(existing -> {
            if (hole.getDescription() != null) existing.setDescription(hole.getDescription());
            if (hole.getStory() != null) existing.setStory(hole.getStory());
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
