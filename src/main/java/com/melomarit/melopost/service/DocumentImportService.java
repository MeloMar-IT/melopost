package com.melomarit.melopost.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melomarit.melopost.model.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentImportService {

    private final PostmortemService postmortemService;
    private final ObjectMapper objectMapper;

    public DocumentImportService(PostmortemService postmortemService, ObjectMapper objectMapper) {
        this.postmortemService = postmortemService;
        this.objectMapper = objectMapper;
    }

    public Postmortem importFromDocument(MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.lastIndexOf(".") != -1) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        
        // Ensure we support .docx, .doc, and .pdf
        if (!extension.equals(".docx") && !extension.equals(".doc") && !extension.equals(".pdf")) {
            throw new IllegalArgumentException("Unsupported file type: " + extension);
        }

        Path tempFile = Files.createTempFile("postmortem_upload", extension);
        file.transferTo(tempFile.toFile());

        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "parse_postmortem.py", tempFile.toString());
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Python script failed with exit code " + exitCode);
            }

            JsonNode root = objectMapper.readTree(output.toString());
            if (root.has("error")) {
                throw new RuntimeException("Parser error: " + root.get("error").asText());
            }

            return mapJsonToPostmortem(root);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private Postmortem mapJsonToPostmortem(JsonNode root) {
        Postmortem pm = new Postmortem();
        
        String incidentRef = root.path("incidentRef").asText();
        pm.setIncidentRef(incidentRef);
        
        // The title in the database should be the Incident ID
        if (incidentRef != null && !incidentRef.isEmpty()) {
            pm.setTitle(incidentRef);
        } else {
            pm.setTitle(root.path("title").asText("Imported Postmortem"));
        }

        // Description is in the Database Description (from business impact section)
        String businessImpactDesc = root.path("businessImpact").path("description").asText();
        if (businessImpactDesc != null && !businessImpactDesc.isEmpty()) {
            pm.setDescription(businessImpactDesc);
        } else {
            pm.setDescription(root.path("description").asText());
        }

        pm.setType("Major Postmortem"); // Default for imported docs from this template
        
        // Mappings from requirement
        // Team Name is in the Database Department
        pm.setDepartment(root.path("teamName").asText());
        
        // Source should be datasource “Service NOW read”
        pm.setIncidentSource("Service NOW read");
        
        // Story Store should always be datasource “Azure DEV/OPS create new record”
        pm.setStoryStore("Azure DEV/OPS create new record");
        
        // Service is in the Database Application that failed
        String service = root.path("businessImpact").path("service").asText();
        if (service != null && !service.isEmpty()) {
            pm.setFailedApplication(service);
        } else {
            pm.setFailedApplication(root.path("failedApplication").asText());
        }

        // Timeline
        List<TimelineEventUDT> timelineEvents = new ArrayList<>();
        JsonNode timelineNode = root.path("timeline");
        if (timelineNode.isArray()) {
            for (JsonNode eventNode : timelineNode) {
                TimelineEventUDT event = new TimelineEventUDT();
                String timestamp = eventNode.path("timestamp").asText();
                event.setDescription(eventNode.path("description").asText());
                
                LocalDateTime dt = parseDateTime(timestamp);
                if (dt != null) {
                    event.setEventTime(dt);
                    if (pm.getIncidentDate() == null) {
                        pm.setIncidentDate(dt);
                    }
                }
                timelineEvents.add(event);
            }
        }
        pm.setTimelineEvents(timelineEvents);

        // Layers and Holes
        List<CheeseLayerUDT> layers = new ArrayList<>();
        JsonNode layersNode = root.path("layers");
        if (layersNode.isArray()) {
            for (JsonNode layerNode : layersNode) {
                CheeseLayerUDT layer = new CheeseLayerUDT();
                layer.setName(layerNode.path("name").asText());
                
                List<HoleUDT> holes = new ArrayList<>();
                JsonNode holesNode = layerNode.path("holes");
                if (holesNode.isArray()) {
                    for (JsonNode holeNode : holesNode) {
                        HoleUDT hole = new HoleUDT();
                        hole.setDescription(holeNode.path("action").asText());
                        hole.setTeamName(holeNode.path("tribe").asText());
                        hole.setActionStatus("PENDING");
                        
                        StoryUDT story = new StoryUDT();
                        story.setStoryNumber(holeNode.path("storyId").asText());
                        story.setWhatToFix(holeNode.path("action").asText());
                        story.setTeamName(holeNode.path("tribe").asText());
                        story.setManagerName(holeNode.path("productOwner").asText());
                        story.setPlatform("Azure DEV/OPS create new record"); // Based on template
                        story.setStatus("NEW");
                        
                        hole.setStory(story);
                        holes.add(hole);
                    }
                }
                layer.setHoles(holes);
                layers.add(layer);
            }
        }
        pm.setLayers(layers);

        return postmortemService.save(pm);
    }

    private LocalDateTime parseDateTime(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return null;
        
        // Try various formats
        String[] formats = {
            "dd-MM-yyyy HH:mm",
            "dd-MM-yyyy",
            "dd/MM/yyyy HH:mm",
            "dd/MM/yyyy",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd"
        };

        for (String format : formats) {
            try {
                if (format.contains("HH:mm")) {
                    return LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern(format));
                } else {
                    LocalDate date = LocalDate.parse(timestamp, DateTimeFormatter.ofPattern(format));
                    return date.atStartOfDay();
                }
            } catch (DateTimeParseException e) {
                // continue
            }
        }
        
        // Handle cases like "02-03 / 03-03" or other messy text by just returning current date or null
        return null;
    }
}
