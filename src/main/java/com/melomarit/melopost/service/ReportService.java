package com.melomarit.melopost.service;

import com.melomarit.melopost.model.Postmortem;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    private final Mustache.Compiler mustacheCompiler;
    private final ResourceLoader resourceLoader;
    private final ReportTemplateService templateService;

    public ReportService(Mustache.Compiler mustacheCompiler, ResourceLoader resourceLoader, ReportTemplateService templateService) {
        this.mustacheCompiler = mustacheCompiler;
        this.resourceLoader = resourceLoader;
        this.templateService = templateService;
    }
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    public byte[] generatePostmortemPdf(Postmortem postmortem) throws IOException {
        String templateContent;
        var dbTemplate = templateService.findDefault();
        
        if (dbTemplate.isPresent()) {
            templateContent = dbTemplate.get().getContent();
        } else {
            Resource resource = resourceLoader.getResource("classpath:templates/reports/postmortem-report.mustache");
            try (InputStream is = resource.getInputStream()) {
                templateContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        Template template = mustacheCompiler.compile(templateContent);

        Map<String, Object> context = new HashMap<>();
        context.put("title", postmortem.getTitle());
        context.put("description", postmortem.getDescription());
        context.put("incidentRef", postmortem.getIncidentRef());
        context.put("incidentSource", postmortem.getIncidentSource());
        context.put("storyApplication", postmortem.getStoryApplication());
        context.put("startDate", postmortem.getStartDate() != null ? postmortem.getStartDate().format(DATE_FORMATTER) : "N/A");
        context.put("incidentDate", postmortem.getIncidentDate() != null ? postmortem.getIncidentDate().format(DATE_FORMATTER) : "N/A");
        context.put("postMortemMeetingDate", postmortem.getPostMortemMeetingDate() != null ? postmortem.getPostMortemMeetingDate().format(DATE_FORMATTER) : "N/A");
        context.put("dueDate", postmortem.getDueDate() != null ? postmortem.getDueDate().format(DATE_ONLY_FORMATTER) : "N/A");
        context.put("tags", postmortem.getTags());
        
        context.put("layers", postmortem.getLayers().stream().map(l -> {
            Map<String, Object> layer = new HashMap<>();
            layer.put("name", l.getName());
            layer.put("description", l.getDescription());
            layer.put("holes", l.getHoles().stream().map(h -> {
                Map<String, Object> hole = new HashMap<>();
                hole.put("description", h.getDescription());
                hole.put("teamName", h.getTeamName());
                hole.put("remedialAction", h.getRemedialAction());
                hole.put("actionStatus", h.getActionStatus());
                hole.put("tags", h.getTags());
                if (h.getStory() != null) {
                    Map<String, String> story = new HashMap<>();
                    story.put("storyNumber", h.getStory().getStoryNumber());
                    story.put("status", h.getStory().getStatus());
                    hole.put("story", story);
                }
                return hole;
            }).toList());
            return layer;
        }).toList());

        context.put("timelineEvents", postmortem.getTimelineEvents().stream().map(e -> {
            Map<String, String> event = new HashMap<>();
            event.put("eventTime", e.getEventTime() != null ? e.getEventTime().format(DATE_FORMATTER) : "N/A");
            event.put("description", e.getDescription());
            return event;
        }).toList());

        context.put("questions", postmortem.getQuestions().stream().map(q -> {
            Map<String, String> question = new HashMap<>();
            question.put("cheeseLayer", q.getCheeseLayer());
            question.put("question", q.getQuestion());
            question.put("answer", q.getAnswer());
            return question;
        }).toList());

        String html = template.execute(context);
        return generatePdfFromHtml(html);
    }

    private byte[] generatePdfFromHtml(String html) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, "");
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        }
    }
}
