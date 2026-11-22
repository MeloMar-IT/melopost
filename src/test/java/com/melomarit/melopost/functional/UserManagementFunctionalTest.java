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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserManagementFunctionalTest extends com.melomarit.melopost.support.AbstractCassandraIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
        // password is write-only in JSON, so the request body is built by hand
        String body = "{\"username\":\"testuser\",\"password\":\"testpass\","
                + "\"email\":\"test@example.com\",\"roles\":[\"USER\"]}";

        MvcResult createResult = mockMvc.perform(post("/api/users").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", notNullValue()))
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        User createdUser = objectMapper.readValue(createResult.getResponse().getContentAsString(), User.class);
        UUID userId = createdUser.getUuid();
        String storedHash = userRepository.findById(userId).orElseThrow().getPassword();
        assertTrue(passwordEncoder.matches("testpass", storedHash));

        // 2. Read the user
        mockMvc.perform(get("/api/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.email", is("test@example.com")));

        // 3. Update the user roles and info
        createdUser.setFirstName("Test");
        createdUser.setLastName("User");
        createdUser.getRoles().add("MANAGER");

        mockMvc.perform(put("/api/users/" + userId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createdUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Test")))
                .andExpect(jsonPath("$.roles", hasItems("USER", "MANAGER")));
        assertEquals(storedHash, userRepository.findById(userId).orElseThrow().getPassword());

        // 4. List all users
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2)))) // admin_mgr and testuser
                .andExpect(jsonPath("$[*].username", hasItems("admin_mgr", "testuser")));

        // 5. Delete the user
        mockMvc.perform(delete("/api/users/" + userId).with(csrf()))
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
