package com.melo.melopost.controller;

import com.melo.melopost.model.DataSource;
import com.melo.melopost.service.DataSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/datasources")
public class DataSourceViewController {

    @Autowired
    private DataSourceService dataSourceService;

    @GetMapping
    public String listDataSources(Model model) {
        model.addAttribute("datasources", dataSourceService.getAllDataSources());
        return "datasources/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("datasource", new DataSource());
        model.addAttribute("templates", dataSourceService.getTemplates());
        return "datasources/form";
    }

    @PostMapping("/save")
    public String saveDataSource(@ModelAttribute DataSource dataSource) {
        dataSourceService.saveDataSource(dataSource);
        return "redirect:/datasources";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        dataSourceService.getDataSourceById(id).ifPresent(ds -> model.addAttribute("datasource", ds));
        model.addAttribute("templates", dataSourceService.getTemplates());
        return "datasources/form";
    }

    @GetMapping("/delete/{id}")
    public String deleteDataSource(@PathVariable Long id) {
        dataSourceService.deleteDataSource(id);
        return "redirect:/datasources";
    }
}
