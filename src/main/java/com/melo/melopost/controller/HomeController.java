package com.melo.melopost.controller;

import com.melo.melopost.service.PostmortemService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final PostmortemService postmortemService;

    public HomeController(PostmortemService postmortemService) {
        this.postmortemService = postmortemService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("recentPostmortems", postmortemService.findRecent());
        model.addAttribute("postmortemCount", postmortemService.findAll().size());
        return "index";
    }
}
