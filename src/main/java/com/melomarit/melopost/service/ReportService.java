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
        context.put("title", postmortem.getTitle() != null ? postmortem.getTitle() : "N/A");
        context.put("description", postmortem.getDescription() != null ? postmortem.getDescription() : "N/A");
        context.put("incidentRef", postmortem.getIncidentRef() != null ? postmortem.getIncidentRef() : "N/A");
        context.put("incidentSource", postmortem.getIncidentSource() != null ? postmortem.getIncidentSource() : "N/A");
        context.put("storyStore", postmortem.getStoryStore() != null ? postmortem.getStoryStore() : "N/A");
        context.put("department", postmortem.getDepartment() != null ? postmortem.getDepartment() : "N/A");
        context.put("failedApplication", postmortem.getFailedApplication() != null ? postmortem.getFailedApplication() : "N/A");
        context.put("type", postmortem.getType() != null ? postmortem.getType() : "N/A");
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
            Map<String, Object> event = new HashMap<>();
            event.put("eventTime", e.getEventTime() != null ? e.getEventTime().format(DATE_FORMATTER) : "N/A");
            event.put("description", e.getDescription() != null ? e.getDescription() : "");
            return event;
        }).toList());

        context.put("questions", postmortem.getQuestions().stream().map(q -> {
            Map<String, Object> question = new HashMap<>();
            question.put("cheeseLayer", q.getCheeseLayer() != null ? q.getCheeseLayer() : "N/A");
            question.put("question", q.getQuestion() != null ? q.getQuestion() : "N/A");
            question.put("answer", q.getAnswer() != null ? q.getAnswer() : "");
            return question;
        }).toList());

        context.put("localPostmortems", postmortem.getLocalPostmortems().stream().map(lp -> {
            Map<String, Object> pm = new HashMap<>();
            pm.put("title", lp.getTitle());
            pm.put("incidentRef", lp.getIncidentRef() != null ? lp.getIncidentRef() : "N/A");
            pm.put("incidentDate", lp.getIncidentDate() != null ? lp.getIncidentDate().format(DATE_FORMATTER) : "N/A");
            return pm;
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
