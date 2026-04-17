package com.melomarit.melopost.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/database")
@PreAuthorize("hasRole('ADMIN')")
public class DatabaseAdminController {

    @GetMapping
    public String getDatabaseInfo() {
        return "admin/database";
    }
}
