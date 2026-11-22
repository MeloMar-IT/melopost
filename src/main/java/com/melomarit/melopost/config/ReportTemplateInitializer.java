package com.melomarit.melopost.config;

import com.melomarit.melopost.model.ReportTemplate;
import com.melomarit.melopost.repository.ReportTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Creates or updates the built-in report templates on every start, in every profile. */
@Configuration
public class ReportTemplateInitializer {
    private static final Logger log = LoggerFactory.getLogger(ReportTemplateInitializer.class);

    @Bean
    @Order(3)
    public CommandLineRunner initReportTemplates(ReportTemplateRepository repository, ResourceLoader resourceLoader) {
        return args -> {
            try {
                upsert(repository, resourceLoader, "Standard Postmortem Report",
                        "classpath:templates/reports/postmortem-report.mustache", true);
                upsert(repository, resourceLoader, "Test Mustache Template",
                        "classpath:templates/reports/test.mustache", false);
            } catch (Exception e) {
                log.error("[REPORT_TEMPLATES] Error initializing/updating report templates: {}", e.getMessage());
            }
        };
    }

    private void upsert(ReportTemplateRepository repository, ResourceLoader resourceLoader,
                        String name, String location, boolean isDefault) throws java.io.IOException {
        var existing = repository.findByName(name);
        String content = new String(resourceLoader.getResource(location).getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        if (existing.isPresent()) {
            ReportTemplate t = existing.get();
            if (!content.equals(t.getContent())) {
                t.setContent(content);
                repository.save(t);
                log.info("[REPORT_TEMPLATES] Updated report template '{}' with latest content.", name);
            }
        } else {
            ReportTemplate t = new ReportTemplate();
            t.setUuid(UUID.randomUUID());
            t.setName(name);
            t.setContent(content);
            t.setDefault(isDefault);
            repository.save(t);
            log.info("[REPORT_TEMPLATES] Initialized report template '{}'.", name);
        }
    }
}
