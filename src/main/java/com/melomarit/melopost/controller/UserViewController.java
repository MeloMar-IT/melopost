package com.melomarit.melopost.controller;

import com.melomarit.melopost.model.User;
import com.melomarit.melopost.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/users")
public class UserViewController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        return "users/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new User());
        return "users/form";
    }

    @PostMapping("/save")
    public String saveUser(@ModelAttribute User user, @RequestParam(required = false) String allowedDepartments) {
        if (allowedDepartments != null && !allowedDepartments.trim().isEmpty()) {
            user.setAllowedDepartments(new HashSet<>(Arrays.asList(allowedDepartments.split("\\s*,\\s*"))));
        } else {
            user.setAllowedDepartments(new HashSet<>());
        }
        userService.save(user);
        return "redirect:/users";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        userService.findById(id).ifPresent(user -> model.addAttribute("user", user));
        return "users/form";
    }

    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        return "redirect:/users";
    }
}
