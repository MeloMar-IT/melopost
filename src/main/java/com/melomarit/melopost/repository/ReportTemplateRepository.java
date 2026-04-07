package com.melomarit.melopost.repository;

import com.melomarit.melopost.model.ReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, Long> {
    Optional<ReportTemplate> findByIsDefaultTrue();
}
