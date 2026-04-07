package com.melo.melopost.controller;

import com.melo.melopost.model.CheeseLayer;
import com.melo.melopost.service.CheeseLayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cheese-layers")
public class CheeseLayerController {
    @Autowired
    private CheeseLayerService service;

    @GetMapping
    public List<CheeseLayer> getAll() { return service.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<CheeseLayer> getById(@PathVariable Long id) {
        return service.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public CheeseLayer create(@RequestBody CheeseLayer layer) { return service.save(layer); }

    @PutMapping("/{id}")
    public ResponseEntity<CheeseLayer> update(@PathVariable Long id, @RequestBody CheeseLayer layer) {
        return service.findById(id).map(existing -> {
            if (layer.getName() != null) existing.setName(layer.getName());
            if (layer.getDescription() != null) existing.setDescription(layer.getDescription());
            if (layer.getHoles() != null) {
                existing.getHoles().clear();
                existing.getHoles().addAll(layer.getHoles());
            }
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
