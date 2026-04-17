package com.melomarit.melopost.repository;

import com.melomarit.melopost.model.ReportTemplate;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.AllowFiltering;
import java.util.Optional;
import java.util.UUID;

public interface ReportTemplateRepository extends CassandraRepository<ReportTemplate, UUID> {
    @AllowFiltering
    Optional<ReportTemplate> findByIsDefaultTrue();
    
    @AllowFiltering
    Optional<ReportTemplate> findByName(String name);
}
