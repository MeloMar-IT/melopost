package com.melomarit.melopost.service;

import com.melomarit.melopost.model.HoleUDT;
import com.melomarit.melopost.model.Postmortem;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.Deflater;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final Mustache.Compiler mustacheCompiler;
    private final ResourceLoader resourceLoader;
    private final ReportTemplateService templateService;
    private final PostmortemService postmortemService;

    private final ReportResourcePolicy resourcePolicy;

    public ReportService(Mustache.Compiler mustacheCompiler, ResourceLoader resourceLoader, ReportTemplateService templateService,
                         PostmortemService postmortemService, ReportResourcePolicy resourcePolicy) {
        this.resourcePolicy = resourcePolicy;
        this.mustacheCompiler = mustacheCompiler;
        this.resourceLoader = resourceLoader;
        this.templateService = templateService;
        this.postmortemService = postmortemService;
    }
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    public byte[] generatePostmortemPdf(Postmortem postmortem) throws IOException {
        log.debug("Starting PDF generation for postmortem: ID={}, Title='{}'", postmortem.getUuid(), postmortem.getTitle());
        String templateContent = null;
        var dbTemplate = templateService.findDefault();
        
        if (dbTemplate.isPresent() && dbTemplate.get().getContent() != null && !dbTemplate.get().getContent().isBlank()) {
            log.info("Using default report template from database: {}", dbTemplate.get().getName());
            log.debug("Database template content length: {}", dbTemplate.get().getContent().length());
            templateContent = dbTemplate.get().getContent();
        } else {
            log.info("Using default file-based report template");
            Resource resource = resourceLoader.getResource("classpath:templates/reports/postmortem-report.mustache");
            log.debug("Loading template from: {}", resource.getDescription());
            try (InputStream is = resource.getInputStream()) {
                byte[] bytes = is.readAllBytes();
                log.debug("File-based template content length: {}", bytes.length);
                templateContent = new String(bytes, StandardCharsets.UTF_8);
            }
        }

        if (templateContent == null || templateContent.isBlank()) {
            log.error("Report template content is empty");
            throw new IOException("Report template content is empty");
        }

        Template template = mustacheCompiler.compile(templateContent);

        Map<String, Object> context = buildReportContext(postmortem);

        log.debug("Executing mustache template. Context keys: {}", context.keySet());
        String html = template.execute(context);
        log.debug("Template execution complete. HTML length: {}", html.length());

        byte[] pdf = generatePdfFromHtml(html);
        log.debug("PDF generation complete. PDF size: {} bytes", pdf.length);
        return pdf;
    }

    Map<String, Object> buildReportContext(Postmortem postmortem) {
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
        context.put("note", postmortem.getNote() != null ? postmortem.getNote() : "");
        
        Map<String, List<Map<String, Object>>> aggregatedHoles = new HashMap<>();

        log.debug("Processing {} layers for holes", postmortem.getLayers().size());
        postmortem.getLayers().forEach(l -> {
            List<Map<String, Object>> holes = l.getHoles().stream().map(h -> mapHole(h, null)).toList();
            log.debug("Layer '{}' has {} holes", l.getName(), holes.size());
            aggregatedHoles.put(l.getName(), new ArrayList<>(holes));
        });

        // Resolve local postmortems
        log.debug("Resolving local postmortems: {}", postmortem.getLocalPostmortemUuids());
        List<Postmortem> localPms = new ArrayList<>();
        if (postmortem.getLocalPostmortemUuids() != null) {
            localPms = postmortemService.findVisibleByIds(postmortem.getLocalPostmortemUuids());
        }

        localPms.forEach(lp -> {
            log.debug("Aggregating holes from local postmortem: ID={}, Title='{}'", lp.getUuid(), lp.getTitle());
            lp.getLayers().forEach(l -> {
                List<Map<String, Object>> holesList = aggregatedHoles.computeIfAbsent(l.getName(), k -> new ArrayList<>());
                List<Map<String, Object>> localHoles = l.getHoles().stream().map(h -> mapHole(h, lp)).toList();
                log.debug("Adding {} holes from layer '{}' of local postmortem", localHoles.size(), l.getName());
                holesList.addAll(localHoles);
            });
        });

        context.put("layers", postmortem.getLayers().stream().map(l -> {
            Map<String, Object> layer = new HashMap<>();
            layer.put("name", l.getName());
            layer.put("description", l.getDescription());
            layer.put("holes", aggregatedHoles.getOrDefault(l.getName(), List.of()));
            return layer;
        }).toList());

        log.debug("Processing {} timeline events for chronological list", postmortem.getTimelineEvents().size());
        List<Map<String, Object>> sortedEvents = postmortem.getTimelineEvents().stream()
                .filter(e -> {
                    if (e.getEventTime() == null) {
                        log.warn("Timeline event has null eventTime: {}", e.getDescription());
                        return false;
                    }
                    return true;
                })
                .sorted((a, b) -> a.getEventTime().compareTo(b.getEventTime()))
                .map(e -> {
                    Map<String, Object> event = new HashMap<>();
                    event.put("eventTime", e.getEventTime().format(DATE_FORMATTER));
                    event.put("description", e.getDescription() != null ? e.getDescription() : "");
                    return event;
                }).toList();
        
        context.put("timelineEvents", sortedEvents);
        log.debug("Sorted {} valid timeline events", sortedEvents.size());

        log.debug("Grouping timeline events for graphical timeline for postmortem: {}", postmortem.getUuid());
        List<com.melomarit.melopost.model.TimelineEventUDT> events = postmortem.getTimelineEvents();
        log.debug("Found {} raw timeline events", events.size());

        List<Map<String, Object>> graphicalTimeline = events.stream()
                .filter(e -> {
                    if (e.getEventTime() == null) {
                        log.warn("Skipping timeline event with null eventTime in graphical timeline: {}", e.getDescription());
                        return false;
                    }
                    return true;
                })
                .sorted((a, b) -> a.getEventTime().compareTo(b.getEventTime()))
                .map(e -> {
                    Map<String, Object> event = new HashMap<>();
                    event.put("date", e.getEventTime().format(DateTimeFormatter.ofPattern("MMM dd")));
                    event.put("time", e.getEventTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                    event.put("description", e.getDescription() != null ? e.getDescription() : "");
                    return event;
                }).toList();

        context.put("graphicalTimeline", graphicalTimeline);
        context.put("hasGraphicalTimeline", !graphicalTimeline.isEmpty());

        // Generate Mermaid Timeline Source
        if (resourcePolicy.isKrokiEnabled() && !events.isEmpty()) {
            StringBuilder mermaidSource = new StringBuilder("timeline\n    title Incident Timeline\n");
            Map<String, List<String>> groups = new LinkedHashMap<>();
            DateTimeFormatter mermaidDateFormatter = DateTimeFormatter.ofPattern("MMM dd HH mm");

            events.stream()
                    .filter(e -> e.getEventTime() != null && e.getDescription() != null && !e.getDescription().isBlank())
                    .sorted((a, b) -> a.getEventTime().compareTo(b.getEventTime()))
                    .forEach(e -> {
                        String dateStr = e.getEventTime().format(mermaidDateFormatter);
                        String description = e.getDescription()
                                .replace("\"", "'")
                                .replace(":", " ")
                                .replaceAll("[()\\[\\]{}]", "")
                                .replace("#", "")
                                .replace(";", ",")
                                .replace("--", "-")
                                .replace("\n", " ")
                                .trim();
                        if (!description.isEmpty()) {
                            groups.computeIfAbsent(dateStr, k -> new ArrayList<>()).add("\"" + description + "\"");
                        }
                    });

            if (!groups.isEmpty()) {
                groups.forEach((date, descList) -> {
                    mermaidSource.append("    \"").append(date).append("\" : ").append(String.join(" : ", descList)).append("\n");
                });
                
                String source = mermaidSource.toString();
                try {
                    String encoded = encodeForKroki(source);
                    resourcePolicy.krokiTimelineUrl(encoded).ifPresent(url -> {
                        context.put("mermaidSource", encoded);
                        context.put("krokiTimelineUrl", url);
                    });
                    log.debug("Generated and encoded Mermaid source for PDF report");
                } catch (IOException e) {
                    log.error("Failed to encode Mermaid source for Kroki", e);
                }
            }
        }

        log.debug("Graphical timeline has {} events", graphicalTimeline.size());
        if (graphicalTimeline.isEmpty()) {
            log.warn("Graphical timeline is EMPTY for postmortem: {}", postmortem.getUuid());
        }

        context.put("questions", postmortem.getQuestions().stream().map(q -> {
            Map<String, Object> question = new HashMap<>();
            question.put("cheeseLayer", q.getCheeseLayer() != null ? q.getCheeseLayer() : "N/A");
            question.put("question", q.getQuestion() != null ? q.getQuestion() : "N/A");
            question.put("answer", q.getAnswer() != null ? q.getAnswer() : "");
            return question;
        }).toList());

        context.put("localPostmortems", localPms.stream().map(lp -> {
            Map<String, Object> pm = new HashMap<>();
            pm.put("uuid", lp.getUuid());
            pm.put("title", lp.getTitle());
            pm.put("incidentRef", lp.getIncidentRef() != null ? lp.getIncidentRef() : "N/A");
            pm.put("incidentDate", lp.getIncidentDate() != null ? lp.getIncidentDate().format(DATE_FORMATTER) : "N/A");
            
            pm.put("layers", lp.getLayers().stream().map(l -> {
                Map<String, Object> layer = new HashMap<>();
                layer.put("name", l.getName());
                layer.put("description", l.getDescription());
                layer.put("holes", l.getHoles().stream().map(h -> mapHole(h, null)).toList());
                return layer;
            }).toList());
            
            return pm;
        }).toList());

        context.put("majorPostmortems", postmortemService.findMajorPostmortems(postmortem.getUuid()).stream().map(mp -> {
            Map<String, Object> pm = new HashMap<>();
            pm.put("uuid", mp.getUuid());
            pm.put("title", mp.getTitle());
            pm.put("incidentRef", mp.getIncidentRef() != null ? mp.getIncidentRef() : "N/A");
            pm.put("incidentDate", mp.getIncidentDate() != null ? mp.getIncidentDate().format(DATE_FORMATTER) : "N/A");
            return pm;
        }).toList());

        return context;
    }

    private Map<String, Object> mapHole(HoleUDT h, Postmortem source) {
        Map<String, Object> hole = new HashMap<>();
        hole.put("description", toPdfRichText(h.getDescription()));
        hole.put("teamName", h.getTeamName());
        hole.put("remedialAction", toPdfRichText(h.getRemedialAction()));
        hole.put("actionStatus", h.getActionStatus());
        hole.put("tags", h.getTags());
        if (source != null) {
            hole.put("sourcePostmortem", source.getTitle() + " (" + source.getIncidentRef() + ")");
        }
        if (h.getStory() != null) {
            Map<String, String> story = new HashMap<>();
            story.put("storyNumber", h.getStory().getStoryNumber());
            story.put("status", h.getStory().getStatus());
            story.put("whatToFix", h.getStory().getWhatToFix());
            story.put("priority", h.getStory().getPriority());
            story.put("storyLink", h.getStory().getStoryLink());
            hole.put("story", story);
        }
        return hole;
    }

    private String encodeForKroki(String source) throws IOException {
        byte[] input = source.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try (java.util.zip.DeflaterOutputStream dos = new java.util.zip.DeflaterOutputStream(os, deflater)) {
            dos.write(input);
        }
        return Base64.getUrlEncoder().encodeToString(os.toByteArray());
    }

    private static final java.util.regex.Pattern NAMED_ENTITY = java.util.regex.Pattern.compile("&([a-zA-Z][a-zA-Z0-9]*);");
    private static final java.util.regex.Pattern VOID_TAG = java.util.regex.Pattern.compile("<(br|hr)\\s*>", java.util.regex.Pattern.CASE_INSENSITIVE);

    /** Sanitizes rich text and makes the result well-formed XML so it can be embedded in the XHTML report. */
    static String toPdfRichText(String html) {
        String clean = RichTextSanitizer.sanitize(html);
        clean = VOID_TAG.matcher(clean).replaceAll(m -> "<" + m.group(1).toLowerCase(Locale.ROOT) + "/>");
        return NAMED_ENTITY.matcher(clean).replaceAll(m -> {
            String name = m.group(1);
            return switch (name) {
                case "amp", "lt", "gt", "quot", "apos" -> m.group();
                default -> XHTML_ENTITY_REFS.getOrDefault(name, "&amp;" + name + ";");
            };
        });
    }

    /** openhtmltopdf's bundled XHTML character entity catalog (classpath resources, no network). */
    private static final String[] XHTML_ENTITY_FILES = {
            "resources/schema/openhtmltopdf/char-entities-xhtml-only.ent",
            "resources/schema/openhtmltopdf/char-entities-xhtml-mathml.ent"};
    private static final java.util.regex.Pattern ENTITY_DECL = java.util.regex.Pattern.compile(
            "<!ENTITY\\s+([A-Za-z][A-Za-z0-9]*)\\s+\"&#(x?)([0-9a-fA-F]+);\"");
    private static final String XHTML_ENTITY_DECLARATIONS = loadEntityFiles();
    /** Entity name to numeric character reference, e.g. copy to &amp;#169;. */
    private static final Map<String, String> XHTML_ENTITY_REFS = parseEntityRefs(XHTML_ENTITY_DECLARATIONS);

    private static String loadEntityFiles() {
        StringBuilder sb = new StringBuilder();
        for (String path : XHTML_ENTITY_FILES) {
            try (InputStream in = ReportService.class.getClassLoader().getResourceAsStream(path)) {
                if (in == null) {
                    throw new IllegalStateException("openhtmltopdf entity catalog missing: " + path);
                }
                sb.append(new String(in.readAllBytes(), StandardCharsets.UTF_8)).append('\n');
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        }
        return sb.toString();
    }

    private static Map<String, String> parseEntityRefs(String declarations) {
        Map<String, String> refs = new HashMap<>();
        java.util.regex.Matcher m = ENTITY_DECL.matcher(declarations);
        while (m.find()) {
            int codePoint = Integer.parseInt(m.group(3), m.group(2).isEmpty() ? 10 : 16);
            refs.putIfAbsent(m.group(1), "&#" + codePoint + ";");
        }
        return Map.copyOf(refs);
    }

    /** Parses report HTML with external entities and DTDs disabled; only openhtmltopdf's local entity catalog is consulted. */
    static org.w3c.dom.Document parseXhtml(String html) throws IOException {
        try {
            javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true); // the resolver below serves any DTD locally; nothing is fetched
            dbf.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbf.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(true);
            javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
            org.xml.sax.EntityResolver catalog = com.openhtmltopdf.resource.FSEntityResolver.instance();
            db.setEntityResolver((publicId, systemId) -> {
                boolean looksLikeDtd = publicId != null || (systemId != null && systemId.toLowerCase(Locale.ROOT).endsWith(".dtd"));
                if (looksLikeDtd) {
                    return new org.xml.sax.InputSource(new java.io.StringReader(XHTML_ENTITY_DECLARATIONS));
                }
                org.xml.sax.InputSource local = catalog.resolveEntity(publicId, systemId);
                return local != null ? local : new org.xml.sax.InputSource(new java.io.StringReader(""));
            });
            return db.parse(new org.xml.sax.InputSource(new java.io.StringReader(html)));
        } catch (org.xml.sax.SAXException | javax.xml.parsers.ParserConfigurationException e) {
            throw new IOException("Report HTML is not well-formed: " + e.getMessage(), e);
        }
    }

    private byte[] generatePdfFromHtml(String html) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useUriResolver(resourcePolicy::resolveUri);
            builder.useHttpStreamImplementation(new NoRedirectStreamFactory(resourcePolicy));
            builder.withW3cDocument(parseXhtml(html), "");
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        }
    }
}
