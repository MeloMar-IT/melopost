package com.melomarit.melopost.controller;

import com.melomarit.melopost.model.User;
import com.melomarit.melopost.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserViewController.class)
public class UserViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testListUsers() throws Exception {
        when(userService.findAll()).thenReturn(Collections.emptyList());
        
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/list"))
                .andExpect(model().attributeExists("users"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testListUsers_NullUsers() throws Exception {
        when(userService.findAll()).thenReturn(null);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/list"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No Users Found")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testShowCreateForm() throws Exception {
        mockMvc.perform(get("/users/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/form"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testShowEditForm() throws Exception {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setUuid(id);
        user.setUsername("testuser");
        when(userService.findById(id)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/users/edit/" + id))
                .andExpect(status().isOk())
                .andExpect(view().name("users/form"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testShowEditForm_NotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/edit/" + id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"));
    }
}
