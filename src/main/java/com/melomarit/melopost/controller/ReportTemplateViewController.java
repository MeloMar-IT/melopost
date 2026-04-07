package com.melomarit.melopost.controller;

import com.melomarit.melopost.model.ReportTemplate;
import com.melomarit.melopost.service.ReportTemplateService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/templates")
public class ReportTemplateViewController {

    private final ReportTemplateService service;

    public ReportTemplateViewController(ReportTemplateService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("templates", service.findAll());
        return "templates/list";
    }

    @GetMapping("/new")
    public String showForm(Model model) {
        model.addAttribute("template", new ReportTemplate());
        return "templates/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute ReportTemplate template) {
        service.save(template);
        return "redirect:/templates";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("template", service.findById(id).orElseThrow());
        return "templates/form";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        service.deleteById(id);
        return "redirect:/templates";
    }
}
