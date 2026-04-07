package com.melomarit.melopost.service;

import com.melomarit.melopost.model.ReportTemplate;
import com.melomarit.melopost.repository.ReportTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ReportTemplateService {

    private final ReportTemplateRepository repository;

    public ReportTemplateService(ReportTemplateRepository repository) {
        this.repository = repository;
    }

    public List<ReportTemplate> findAll() {
        return repository.findAll();
    }

    public Optional<ReportTemplate> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<ReportTemplate> findDefault() {
        return repository.findByIsDefaultTrue();
    }

    @Transactional
    public ReportTemplate save(ReportTemplate template) {
        if (template.isDefault()) {
            repository.findByIsDefaultTrue().ifPresent(t -> {
                if (!t.getId().equals(template.getId())) {
                    t.setDefault(false);
                    repository.save(t);
                }
            });
        }
        return repository.save(template);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
