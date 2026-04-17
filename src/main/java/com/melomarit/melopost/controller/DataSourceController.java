package com.melomarit.melopost.controller;

import com.melomarit.melopost.model.DataSource;
import com.melomarit.melopost.service.DataSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/datasources")
public class DataSourceController {

    @Autowired
    private DataSourceService dataSourceService;

    @GetMapping
    public List<DataSource> getAll() {
        return dataSourceService.getAllDataSources();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataSource> getById(@PathVariable UUID id) {
        return dataSourceService.getDataSourceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public DataSource create(@RequestBody DataSource dataSource) {
        return dataSourceService.saveDataSource(dataSource);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DataSource> update(@PathVariable UUID id, @RequestBody DataSource dataSource) {
        return dataSourceService.getDataSourceById(id)
                .map(existing -> {
                    if (dataSource.getName() != null) existing.setName(dataSource.getName());
                    if (dataSource.getType() != null) existing.setType(dataSource.getType());
                    if (dataSource.getOperation() != null) existing.setOperation(dataSource.getOperation());
                    if (dataSource.getUrl() != null) existing.setUrl(dataSource.getUrl());
                    if (dataSource.getUsername() != null) existing.setUsername(dataSource.getUsername());
                    if (dataSource.getPassword() != null) existing.setPassword(dataSource.getPassword());
                    if (dataSource.getDescription() != null) existing.setDescription(dataSource.getDescription());
                    return ResponseEntity.ok(dataSourceService.saveDataSource(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (dataSourceService.getDataSourceById(id).isPresent()) {
            dataSourceService.deleteDataSource(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/templates")
    public List<DataSource> getTemplates() {
        return dataSourceService.getTemplates();
    }
}
