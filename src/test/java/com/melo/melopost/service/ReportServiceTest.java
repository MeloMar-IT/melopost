package com.melo.melopost.service;

import com.melo.melopost.model.Postmortem;
import com.melo.melopost.model.ReportTemplate;
import com.samskivert.mustache.Mustache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
public class ReportServiceTest {

    @Autowired
    private ReportService reportService;

    @MockBean
    private ReportTemplateService templateService;

    @Autowired
    private ResourceLoader resourceLoader;

    @Test
    public void testGeneratePdfWithTestTemplate() throws IOException {
        // Arrange
        Postmortem pm = new Postmortem();
        pm.setTitle("Test Postmortem");
        pm.setDescription("This is a test description");
        pm.getTags().add("test");
        pm.getTags().add("mustache");

        // Mock the default template from DB
        ReportTemplate template = new ReportTemplate();
        template.setName("Test Template");
        var resource = resourceLoader.getResource("classpath:templates/reports/test.mustache");
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        template.setContent(content);
        template.setDefault(true);

        when(templateService.findDefault()).thenReturn(Optional.of(template));

        // Act
        byte[] pdfBytes = reportService.generatePostmortemPdf(pm);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    public void testGeneratePdfWithDefaultTemplate() throws IOException {
        // Arrange
        Postmortem pm = new Postmortem();
        pm.setTitle("Default Template Test");
        pm.setDescription("Testing the default classpath template");
        pm.setIncidentRef("INC123");
        pm.setIncidentSource("ServiceNow");
        pm.setStoryApplication("Jira");

        when(templateService.findDefault()).thenReturn(Optional.empty());

        // Act
        byte[] pdfBytes = reportService.generatePostmortemPdf(pm);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }
}
