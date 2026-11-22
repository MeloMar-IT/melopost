package com.melomarit.melopost.config;

import com.melomarit.melopost.model.ReportTemplate;
import com.melomarit.melopost.repository.ReportTemplateRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReportTemplateInitializerTest {

    private static final String DEFAULT = "Standard Postmortem Report";
    private static final String TEST = "Test Mustache Template";

    private final ReportTemplateRepository repo = mock(ReportTemplateRepository.class);
    private final ResourceLoader loader = new DefaultResourceLoader();

    private String load(String file) throws Exception {
        return new String(loader.getResource("classpath:templates/reports/" + file).getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
    }

    private ReportTemplate template(String name, String content) {
        ReportTemplate t = new ReportTemplate();
        t.setName(name);
        t.setContent(content);
        return t;
    }

    private void run() throws Exception {
        new ReportTemplateInitializer().initReportTemplates(repo, loader).run();
    }

    @Test
    void createsBothTemplatesWhenMissing() throws Exception {
        when(repo.findByName(any())).thenReturn(Optional.empty());

        run();

        ArgumentCaptor<ReportTemplate> captor = ArgumentCaptor.forClass(ReportTemplate.class);
        verify(repo, times(2)).save(captor.capture());
        assertEquals(DEFAULT, captor.getAllValues().get(0).getName());
        assertTrue(captor.getAllValues().get(0).isDefault());
        assertEquals(TEST, captor.getAllValues().get(1).getName());
        assertFalse(captor.getAllValues().get(1).isDefault());
    }

    @Test
    void savesNothingWhenContentIsCurrent() throws Exception {
        when(repo.findByName(DEFAULT)).thenReturn(Optional.of(template(DEFAULT, load("postmortem-report.mustache"))));
        when(repo.findByName(TEST)).thenReturn(Optional.of(template(TEST, load("test.mustache"))));

        run();

        verify(repo, never()).save(any());
    }

    @Test
    void updatesStaleDefaultTemplateOnly() throws Exception {
        when(repo.findByName(DEFAULT)).thenReturn(Optional.of(template(DEFAULT, "stale")));
        when(repo.findByName(TEST)).thenReturn(Optional.of(template(TEST, load("test.mustache"))));

        run();

        verify(repo, times(1)).save(any(ReportTemplate.class));
    }
}
