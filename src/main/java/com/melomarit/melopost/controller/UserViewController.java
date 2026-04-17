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
import java.util.UUID;

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
    public String saveUser(@ModelAttribute User user, @RequestParam(required = false) String allowedDepartments, @RequestParam(required = false) String password) {
        if (allowedDepartments != null && !allowedDepartments.trim().isEmpty()) {
            user.setAllowedDepartments(new HashSet<>(Arrays.asList(allowedDepartments.split("\\s*,\\s*"))));
        } else {
            user.setAllowedDepartments(new HashSet<>());
        }
        
        if (user.getUuid() != null) {
            userService.findById(user.getUuid()).ifPresent(existing -> {
                user.setPassword(existing.getPassword());
                if (password != null && !password.isEmpty()) {
                    user.setPassword(password);
                }
            });
        } else {
            user.setPassword(password);
        }

        userService.save(user);
        return "redirect:/users";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable UUID id, Model model) {
        return userService.findById(id)
                .map(user -> {
                    model.addAttribute("user", user);
                    return "users/form";
                })
                .orElse("redirect:/users");
    }

    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable UUID id) {
        userService.deleteById(id);
        return "redirect:/users";
    }
}
