package com.melomarit.melopost.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melomarit.melopost.model.User;
import com.melomarit.melopost.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserManagementFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        // Create an admin user in the database for consistency, 
        // though @WithMockUser(roles="ADMIN") handles the security filter.
        User admin = new User();
        admin.setUsername("admin_mgr");
        admin.setPassword("pass");
        admin.getRoles().add("ADMIN");
        userRepository.save(admin);
    }

    @AfterEach
    public void cleanup() {
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin_mgr", roles = "ADMIN")
    public void userLifecycle_shouldWorkCorrectly() throws Exception {
        // 1. Create a new user
        User newUser = new User();
        newUser.setUsername("testuser");
        newUser.setPassword("testpass");
        newUser.setEmail("test@example.com");
        newUser.getRoles().add("USER");

        MvcResult createResult = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", notNullValue()))
                .andExpect(jsonPath("$.username", is("testuser")))
                .andReturn();

        User createdUser = objectMapper.readValue(createResult.getResponse().getContentAsString(), User.class);
        UUID userId = createdUser.getUuid();

        // 2. Read the user
        mockMvc.perform(get("/api/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.email", is("test@example.com")));

        // 3. Update the user roles and info
        createdUser.setFirstName("Test");
        createdUser.setLastName("User");
        createdUser.getRoles().add("MANAGER");

        mockMvc.perform(put("/api/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createdUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Test")))
                .andExpect(jsonPath("$.roles", hasItems("USER", "MANAGER")));

        // 4. List all users
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2)))) // admin_mgr and testuser
                .andExpect(jsonPath("$[*].username", hasItems("admin_mgr", "testuser")));

        // 5. Delete the user
        mockMvc.perform(delete("/api/users/" + userId))
                .andExpect(status().isNoContent());

        // 6. Verify deletion
        mockMvc.perform(get("/api/users/" + userId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void userRole_shouldNotBeAbleToManageUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }
}
