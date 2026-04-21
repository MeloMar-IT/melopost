package com.melomarit.melopost.controller;

import com.melomarit.melopost.dto.HoleDTO;
import com.melomarit.melopost.dto.PostmortemSearchResultDTO;
import com.melomarit.melopost.dto.StoryDTO;
import com.melomarit.melopost.model.*;
import com.melomarit.melopost.service.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/postmortems")
public class PostmortemViewController {
    private final PostmortemService service;
    private final ReportService reportService;
    private final PostmortemDocumentService documentService;
    private final DataSourceService dataSourceService;
    private final IncidentNoteService incidentNoteService;

    public PostmortemViewController(PostmortemService service, 
                                 ReportService reportService, 
                                 PostmortemDocumentService documentService, 
                                 DataSourceService dataSourceService,
                                 IncidentNoteService incidentNoteService) {
        this.service = service;
        this.reportService = reportService;
        this.documentService = documentService;
        this.dataSourceService = dataSourceService;
        this.incidentNoteService = incidentNoteService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String type,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String incidentDate,
                       @RequestParam(required = false) String incidentDateOp,
                       @RequestParam(required = false) String incidentRef,
                       @RequestParam(required = false) String dueDate,
                       @RequestParam(required = false) String dueDateOp,
                       Model model) {
        
        List<PostmortemSearchResultDTO> results = service.filter(keyword, type, status, incidentDate, incidentDateOp, incidentRef, dueDate, dueDateOp);
        
        if ((keyword != null && !keyword.trim().isEmpty()) || 
            (type != null && !type.trim().isEmpty()) ||
            (status != null && !status.trim().isEmpty()) ||
            (incidentDate != null && !incidentDate.trim().isEmpty()) ||
            (incidentRef != null && !incidentRef.trim().isEmpty()) ||
            (dueDate != null && !dueDate.trim().isEmpty())) {
            
            model.addAttribute("searchResults", results);
            model.addAttribute("keyword", keyword);
            model.addAttribute("type", type);
            model.addAttribute("status", status);
            model.addAttribute("incidentDate", incidentDate);
            model.addAttribute("incidentDateOp", incidentDateOp);
            model.addAttribute("incidentRef", incidentRef);
            model.addAttribute("dueDate", dueDate);
            model.addAttribute("dueDateOp", dueDateOp);
        } else {
            model.addAttribute("postmortems", service.findAll());
        }
        
        model.addAttribute("postmortemTypes", Postmortem.POSTMORTEM_TYPES);
        model.addAttribute("postmortemStates", Postmortem.POSTMORTEM_STATES);
        return "postmortems/list";
    }

    @GetMapping("/holes")
    public String listHoles(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String teamName,
                            @RequestParam(required = false) String actionStatus,
                            Model model) {
        List<HoleDTO> holes = service.findAllHoles(keyword, teamName, actionStatus);
        
        // Collect unique team names and statuses for filters
        Set<String> teamNames = holes.stream()
                .map(HoleDTO::getTeamName)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
                
        Set<String> statuses = holes.stream()
                .map(HoleDTO::getActionStatus)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        model.addAttribute("holes", holes);
        model.addAttribute("teamNames", teamNames);
        model.addAttribute("statuses", statuses);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedTeam", teamName);
        model.addAttribute("selectedStatus", actionStatus);
        
        return "holes/list";
    }

    @GetMapping("/stories")
    public String listStories(@RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String teamName,
                             @RequestParam(required = false) String status,
                             Model model) {
        List<StoryDTO> stories = service.findAllStories(keyword, teamName, status);
        
        // Collect unique team names and statuses for filters
        Set<String> teamNames = stories.stream()
                .map(s -> s.getStory().getTeamName())
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
                
        Set<String> statuses = stories.stream()
                .map(s -> s.getStory().getStatus())
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        model.addAttribute("stories", stories);
        model.addAttribute("teamNames", teamNames);
        model.addAttribute("statuses", statuses);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedTeam", teamName);
        model.addAttribute("selectedStatus", status);

        return "stories/list";
    }

    @GetMapping("/new")
    public String showForm(@RequestParam(required = false) String incidentRef,
                           @RequestParam(required = false) String note,
                           Model model) {
        Postmortem pm = new Postmortem();
        if (incidentRef != null) {
            pm.setIncidentRef(incidentRef);
        }
        if (note != null) {
            pm.setNote(note);
        }
        pm.addLayer(createLayer("Define"));
        pm.addLayer(createLayer("Design"));
        pm.addLayer(createLayer("Build"));
        pm.addLayer(createLayer("Test"));
        pm.addLayer(createLayer("Release"));
        pm.addLayer(createLayer("Run"));
        pm.addLayer(createLayer("Resilience"));
        pm.addLayer(createLayer("Observability"));
        pm.addLayer(createLayer("Incident Handling"));
        pm.addLayer(createLayer("Human"));
        model.addAttribute("postmortem", pm);
        model.addAttribute("readDataSources", dataSourceService.getDataSourcesByOperation("Read"));
        model.addAttribute("writeDataSources", dataSourceService.getDataSourcesByOperation("Create"));
        model.addAttribute("localPostmortems", service.findByType("Local postmortem"));
        model.addAttribute("postmortemStates", Postmortem.POSTMORTEM_STATES);
        return "postmortems/form";
    }

    private CheeseLayerUDT createLayer(String name) {
        CheeseLayerUDT layer = new CheeseLayerUDT();
        layer.setName(name);
        return layer;
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Postmortem postmortem, @RequestParam(value = "files", required = false) MultipartFile[] files, Model model) throws IOException {
        if (postmortem.getUuid() != null) {
            try {
                Postmortem existing = service.findById(postmortem.getUuid());
                if (existing != null && "Published".equals(existing.getStatus())) {
                    // If the postmortem is already published, check if the current user is an admin
                    User currentUser = service.getCurrentUser();
                    if (!service.isUserAdmin(currentUser)) {
                        return "redirect:/postmortems/details/" + postmortem.getUuid() + "?error=locked";
                    }
                }
            } catch (RuntimeException e) {
                // If not found, it might be a manual UUID assignment (unlikely but possible)
            }
        }
        
        if (files != null) {
            documentService.processFiles(postmortem, files);
        }
        try {
            service.save(postmortem);
        } catch (RuntimeException e) {
            model.addAttribute("postmortem", postmortem);
            model.addAttribute("readDataSources", dataSourceService.getDataSourcesByOperation("Read"));
            model.addAttribute("writeDataSources", dataSourceService.getDataSourcesByOperation("Create"));
            model.addAttribute("allPostmortems", service.findAll());
            model.addAttribute("postmortemStates", Postmortem.POSTMORTEM_STATES);
            model.addAttribute("error", e.getMessage());
            return "postmortems/form";
        }
        return "redirect:/postmortems";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable UUID id, Model model) {
        Postmortem pm = service.findById(id);
        pm.setDocuments(documentService.findByPostmortemUuid(id));
        model.addAttribute("postmortem", pm);
        model.addAttribute("readDataSources", dataSourceService.getDataSourcesByOperation("Read"));
        model.addAttribute("writeDataSources", dataSourceService.getDataSourcesByOperation("Create"));
        model.addAttribute("allPostmortems", service.findAll());
        model.addAttribute("postmortemStates", Postmortem.POSTMORTEM_STATES);
        return "postmortems/form";
    }

    @GetMapping("/details/{id}")
    public String view(@PathVariable UUID id, Model model) {
        Postmortem pm = service.findById(id);
        pm.setDocuments(documentService.findByPostmortemUuid(id));
        
        // Populate local postmortem objects
        if (pm.getLocalPostmortemUuids() != null && !pm.getLocalPostmortemUuids().isEmpty()) {
            List<Postmortem> localPms = pm.getLocalPostmortemUuids().stream()
                    .map(lUuid -> {
                        try {
                            return service.findById(lUuid);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            pm.setLocalPostmortems(localPms);
        }

        model.addAttribute("postmortem", pm);
        model.addAttribute("documents", pm.getDocuments());
        model.addAttribute("linkedPostmortems", pm.getLocalPostmortems());
        model.addAttribute("allPostmortems", service.findAll());
        model.addAttribute("postmortemStates", Postmortem.POSTMORTEM_STATES);
        
        // Find matching incident notes
        if (pm.getIncidentRef() != null && !pm.getIncidentRef().isEmpty()) {
            model.addAttribute("incidentNotes", incidentNoteService.findByIncidentRef(pm.getIncidentRef()));
        } else {
            model.addAttribute("incidentNotes", Collections.emptyList());
        }
        
        return "postmortems/details";
    }

    @PostMapping("/{id}/link")
    public String linkPostmortem(@PathVariable UUID id, @RequestParam UUID linkedId) {
        Postmortem pm = service.findById(id);
        if ("Published".equals(pm.getStatus()) && !service.isUserAdmin(service.getCurrentUser())) {
            return "redirect:/postmortems/details/" + id + "?error=locked";
        }
        if (pm.getLocalPostmortemUuids() == null) {
            pm.setLocalPostmortemUuids(new ArrayList<>());
        }
        if (!id.equals(linkedId) && !pm.getLocalPostmortemUuids().contains(linkedId)) {
            pm.getLocalPostmortemUuids().add(linkedId);
            service.save(pm);
        }
        return "redirect:/postmortems/details/" + id;
    }

    @PostMapping("/{id}/unlink")
    public String unlinkPostmortem(@PathVariable UUID id, @RequestParam UUID linkedId) {
        Postmortem pm = service.findById(id);
        if ("Published".equals(pm.getStatus()) && !service.isUserAdmin(service.getCurrentUser())) {
            return "redirect:/postmortems/details/" + id + "?error=locked";
        }
        if (pm.getLocalPostmortemUuids() != null) {
            pm.getLocalPostmortemUuids().remove(linkedId);
            service.save(pm);
        }
        return "redirect:/postmortems/details/" + id;
    }

    @PostMapping("/{id}/tags/add")
    public String addTag(@PathVariable UUID id, @RequestParam String tag) {
        Postmortem pm = service.findById(id);
        if ("Published".equals(pm.getStatus()) && !service.isUserAdmin(service.getCurrentUser())) {
            return "redirect:/postmortems/details/" + id + "?error=locked";
        }
        if (tag != null && !tag.trim().isEmpty()) {
            String trimmedTag = tag.trim();
            if (!pm.getTags().contains(trimmedTag)) {
                pm.getTags().add(trimmedTag);
                service.save(pm);
            }
        }
        return "redirect:/postmortems/details/" + id;
    }

    @PostMapping("/{id}/tags/remove")
    public String removeTag(@PathVariable UUID id, @RequestParam String tag) {
        Postmortem pm = service.findById(id);
        if ("Published".equals(pm.getStatus()) && !service.isUserAdmin(service.getCurrentUser())) {
            return "redirect:/postmortems/details/" + id + "?error=locked";
        }
        pm.getTags().remove(tag);
        service.save(pm);
        return "redirect:/postmortems/details/" + id;
    }

    @PostMapping("/{id}/questions/add")
    public String addQuestion(@PathVariable UUID id, 
                             @RequestParam String cheeseLayer, 
                             @RequestParam String question, 
                             @RequestParam(required = false) String answer) {
        Postmortem pm = service.findById(id);
        if ("Published".equals(pm.getStatus()) && !service.isUserAdmin(service.getCurrentUser())) {
            return "redirect:/postmortems/details/" + id + "?error=locked";
        }
        PostmortemQuestionUDT q = new PostmortemQuestionUDT();
        q.setCheeseLayer(cheeseLayer);
        q.setQuestion(question);
        q.setAnswer(answer);
        pm.getQuestions().add(q);
        service.save(pm);
        return "redirect:/postmortems/details/" + id;
    }

    @PostMapping("/{id}/questions/{qUuid}/remove")
    public String removeQuestion(@PathVariable UUID id, @PathVariable UUID qUuid) {
        Postmortem pm = service.findById(id);
        if ("Published".equals(pm.getStatus()) && !service.isUserAdmin(service.getCurrentUser())) {
            return "redirect:/postmortems/details/" + id + "?error=locked";
        }
        pm.getQuestions().removeIf(q -> q.getUuid().equals(qUuid));
        service.save(pm);
        return "redirect:/postmortems/details/" + id;
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<byte[]> downloadReport(@PathVariable UUID id) {
        try {
            Postmortem postmortem = service.findById(id);
            byte[] pdfBytes = reportService.generatePostmortemPdf(postmortem);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=postmortem-report-" + id + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/documents/upload")
    public String uploadDocument(@PathVariable UUID id, @RequestParam("file") MultipartFile file) throws IOException {
        Postmortem pm = service.findById(id);
        if ("Published".equals(pm.getStatus()) && !service.isUserAdmin(service.getCurrentUser())) {
            return "redirect:/postmortems/details/" + id + "?error=locked";
        }
        documentService.uploadDocument(pm, file);
        service.save(pm);
        return "redirect:/postmortems/details/" + id;
    }

    @GetMapping("/{id}/documents/{docId}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable UUID id, @PathVariable UUID docId) {
        return documentService.findById(docId)
                .map(doc -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                        .contentType(MediaType.parseMediaType(doc.getContentType()))
                        .body(doc.getData()))
                .orElse(ResponseEntity.status(404).body(null));
    }

    @PostMapping("/{id}/documents/{docId}/delete")
    public String deleteDocument(@PathVariable UUID id, @PathVariable UUID docId) {
        Postmortem pm = service.findById(id);
        if ("Published".equals(pm.getStatus()) && !service.isUserAdmin(service.getCurrentUser())) {
            return "redirect:/postmortems/details/" + id + "?error=locked";
        }
        documentService.deleteById(docId);
        return "redirect:/postmortems/details/" + id;
    }

    @GetMapping("/{id}/note")
    public String showNote(@PathVariable UUID id, Model model) {
        model.addAttribute("postmortem", service.findById(id));
        return "postmortems/note";
    }

    @PostMapping("/{id}/note")
    public String saveNote(@PathVariable UUID id, @RequestParam String note) {
        Postmortem pm = service.findById(id);
        if ("Published".equals(pm.getStatus()) && !service.isUserAdmin(service.getCurrentUser())) {
            return "redirect:/postmortems/details/" + id + "?error=locked";
        }
        if (note != null && note.length() > 1000000) {
            note = note.substring(0, 1000000);
        }
        pm.setNote(note);
        service.save(pm);
        return "redirect:/postmortems/details/" + id;
    }

    @PostMapping("/{id}/incident-notes/import")
    public String importIncidentNote(@PathVariable UUID id, @RequestParam UUID noteId) {
        Postmortem pm = service.findById(id);
        if ("Published".equals(pm.getStatus()) && !service.isUserAdmin(service.getCurrentUser())) {
            return "redirect:/postmortems/details/" + id + "?error=locked";
        }
        IncidentNote note = incidentNoteService.findById(noteId);
        
        String currentNote = pm.getNote() != null ? pm.getNote() : "";
        String separator = currentNote.isEmpty() ? "" : "\n\n--- Imported from Incident Note (" + note.getIncidentRef() + ") ---\n\n";
        String combinedNote = currentNote + separator + note.getContent();
        if (combinedNote.length() > 1000000) {
            combinedNote = combinedNote.substring(0, 1000000);
        }
        pm.setNote(combinedNote);
        
        service.save(pm);
        return "redirect:/postmortems/details/" + id;
    }
}
