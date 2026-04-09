package com.melomarit.melopost.controller;

import com.melomarit.melopost.model.CheeseLayer;
import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.model.PostmortemDocument;
import com.melomarit.melopost.model.PostmortemQuestion;
import com.melomarit.melopost.service.DataSourceService;
import com.melomarit.melopost.service.PostmortemDocumentService;
import com.melomarit.melopost.service.PostmortemService;
import com.melomarit.melopost.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequestMapping("/postmortems")
public class PostmortemViewController {
    private final PostmortemService service;
    private final ReportService reportService;
    private final PostmortemDocumentService documentService;
    private final DataSourceService dataSourceService;

    public PostmortemViewController(PostmortemService service, ReportService reportService, PostmortemDocumentService documentService, DataSourceService dataSourceService) {
        this.service = service;
        this.reportService = reportService;
        this.documentService = documentService;
        this.dataSourceService = dataSourceService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword, Model model) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            model.addAttribute("postmortems", service.search(keyword));
            model.addAttribute("keyword", keyword);
        } else {
            model.addAttribute("postmortems", service.findAll());
        }
        return "postmortems/list";
    }

    @GetMapping("/new")
    public String showForm(Model model) {
        Postmortem pm = new Postmortem();
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
        return "postmortems/form";
    }

    private CheeseLayer createLayer(String name) {
        CheeseLayer layer = new CheeseLayer();
        layer.setName(name);
        return layer;
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Postmortem postmortem, @RequestParam(value = "files", required = false) MultipartFile[] files) throws IOException {
        if (postmortem.getId() != null) {
            Postmortem existing = service.findById(postmortem.getId());
            postmortem.setDocuments(existing.getDocuments());
            postmortem.setQuestions(existing.getQuestions());
            postmortem.setCreatedAt(existing.getCreatedAt());
            // Re-attach items to the new model instance to maintain bidirectional integrity
            if (postmortem.getDocuments() != null) {
                for (PostmortemDocument doc : postmortem.getDocuments()) {
                    doc.setPostmortem(postmortem);
                }
            }
            if (postmortem.getQuestions() != null) {
                for (PostmortemQuestion q : postmortem.getQuestions()) {
                    q.setPostmortem(postmortem);
                }
            }
        }
        if (files != null) {
            documentService.processFiles(postmortem, files);
        }
        service.save(postmortem);
        return "redirect:/postmortems";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("postmortem", service.findById(id));
        model.addAttribute("readDataSources", dataSourceService.getDataSourcesByOperation("Read"));
        model.addAttribute("writeDataSources", dataSourceService.getDataSourcesByOperation("Create"));
        model.addAttribute("localPostmortems", service.findByType("Local postmortem"));
        return "postmortems/form";
    }

    @GetMapping("/details/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("postmortem", service.findById(id));
        return "postmortems/details";
    }

    @PostMapping("/{id}/tags/add")
    public String addTag(@PathVariable Long id, @RequestParam String tag) {
        Postmortem pm = service.findById(id);
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
    public String removeTag(@PathVariable Long id, @RequestParam String tag) {
        Postmortem pm = service.findById(id);
        pm.getTags().remove(tag);
        service.save(pm);
        return "redirect:/postmortems/details/" + id;
    }

    @PostMapping("/{id}/questions/add")
    public String addQuestion(@PathVariable Long id, 
                             @RequestParam String cheeseLayer, 
                             @RequestParam String question, 
                             @RequestParam(required = false) String answer) {
        Postmortem pm = service.findById(id);
        PostmortemQuestion q = new PostmortemQuestion();
        q.setCheeseLayer(cheeseLayer);
        q.setQuestion(question);
        q.setAnswer(answer);
        q.setPostmortem(pm);
        pm.getQuestions().add(q);
        service.save(pm);
        return "redirect:/postmortems/details/" + id;
    }

    @PostMapping("/{id}/questions/{qId}/remove")
    public String removeQuestion(@PathVariable Long id, @PathVariable Long qId) {
        Postmortem pm = service.findById(id);
        pm.getQuestions().removeIf(q -> q.getId().equals(qId));
        service.save(pm);
        return "redirect:/postmortems/details/" + id;
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<byte[]> downloadReport(@PathVariable Long id) {
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
    public String uploadDocument(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        Postmortem pm = service.findById(id);
        documentService.uploadDocument(pm, file);
        service.save(pm);
        return "redirect:/postmortems/details/" + id;
    }

    @GetMapping("/{id}/documents/{docId}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id, @PathVariable Long docId) {
        return documentService.findById(docId)
                .map(doc -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                        .contentType(MediaType.parseMediaType(doc.getContentType()))
                        .body(doc.getData()))
                .orElse(ResponseEntity.status(404).body(null));
    }

    @PostMapping("/{id}/documents/{docId}/delete")
    public String deleteDocument(@PathVariable Long id, @PathVariable Long docId) {
        documentService.deleteById(docId);
        return "redirect:/postmortems/details/" + id;
    }

    @GetMapping("/{id}/note")
    public String showNote(@PathVariable Long id, Model model) {
        model.addAttribute("postmortem", service.findById(id));
        return "postmortems/note";
    }

    @PostMapping("/{id}/note")
    public String saveNote(@PathVariable Long id, @RequestParam String note) {
        Postmortem pm = service.findById(id);
        pm.setNote(note);
        service.save(pm);
        return "redirect:/postmortems/details/" + id;
    }
}
