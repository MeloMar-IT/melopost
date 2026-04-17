package com.melomarit.melopost.controller;

import com.melomarit.melopost.model.IncidentNote;
import com.melomarit.melopost.dto.PostmortemSearchResultDTO;
import com.melomarit.melopost.service.IncidentNoteService;
import com.melomarit.melopost.service.PostmortemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/incident-notes")
public class IncidentNoteViewController {
    private final IncidentNoteService service;
    private final PostmortemService postmortemService;

    public IncidentNoteViewController(IncidentNoteService service, PostmortemService postmortemService) {
        this.service = service;
        this.postmortemService = postmortemService;
    }

    @GetMapping
    public String list(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        model.addAttribute("notes", service.search(keyword));
        model.addAttribute("keyword", keyword);
        return "incident-notes/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("note", new IncidentNote());
        return "incident-notes/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable UUID id, Model model) {
        model.addAttribute("note", service.findById(id));
        return "incident-notes/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute IncidentNote note) {
        service.save(note);
        return "redirect:/incident-notes";
    }

    @GetMapping("/migrate/{id}")
    public String migrate(@PathVariable UUID id) {
        IncidentNote note = service.findById(id);
        String content = note.getContent();
        if (content != null && content.length() > 1000000) {
            content = content.substring(0, 1000000);
        }
        return "redirect:/postmortems/new?incidentRef=" + note.getIncidentRef() + "&note=" + content;
    }

    @GetMapping("/add-to-postmortem/{id}")
    public String showAddToPostmortem(@PathVariable UUID id, 
                                     @RequestParam(required = false) String keyword, 
                                     Model model) {
        model.addAttribute("note", service.findById(id));
        if (keyword != null && !keyword.isEmpty()) {
            model.addAttribute("postmortems", postmortemService.search(keyword).stream()
                .map(PostmortemSearchResultDTO::getPostmortem)
                .collect(java.util.stream.Collectors.toList()));
        } else {
            model.addAttribute("postmortems", postmortemService.findRecent());
        }
        model.addAttribute("keyword", keyword);
        return "incident-notes/select-postmortem";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable UUID id) {
        service.deleteById(id);
        return "redirect:/incident-notes";
    }
}
