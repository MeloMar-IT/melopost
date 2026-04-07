package com.melomarit.melopost.controller;

import com.melomarit.melopost.model.User;
import com.melomarit.melopost.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public List<User> getAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getById(@PathVariable Long id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public User create(@RequestBody User user) {
        return userService.save(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable Long id, @RequestBody User user) {
        return userService.findById(id)
                .map(existing -> {
                    if (user.getUsername() != null) existing.setUsername(user.getUsername());
                    if (user.getEmail() != null) existing.setEmail(user.getEmail());
                    if (user.getFirstName() != null) existing.setFirstName(user.getFirstName());
                    if (user.getLastName() != null) existing.setLastName(user.getLastName());
                    if (user.getRoles() != null) existing.setRoles(user.getRoles());
                    if (user.getActive() != null) existing.setActive(user.getActive());
                    
                    if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                        existing.setPassword(user.getPassword());
                    }
                    return ResponseEntity.ok(userService.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (userService.findById(id).isPresent()) {
            userService.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
