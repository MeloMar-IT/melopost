package com.melomarit.melopost.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.repository.PostmortemRepository;
import org.junit.jupiter.api.AfterEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PostmortemFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostmortemRepository postmortemRepository;

    @Autowired
    private com.melomarit.melopost.repository.UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    public void cleanup() {
        postmortemRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin_test", roles = "ADMIN")
    public void postmortemLifecycle_shouldWorkCorrectly() throws Exception {
        // Create the user in the database so PostmortemService can find it
        com.melomarit.melopost.model.User adminUser = new com.melomarit.melopost.model.User();
        adminUser.setUsername("admin_test");
        adminUser.setPassword("password");
        adminUser.getRoles().add("ADMIN");
        adminUser.getAllowedDepartments().add("IT");
        userRepository.save(adminUser);

        // 1. Create a new postmortem
        Postmortem pm = new Postmortem();
        pm.setTitle("Functional Test Postmortem");
        pm.setDescription("This is a postmortem created by a functional test.");
        pm.setDepartment("IT"); // Added department to ensure it matches user's allowed departments if needed

        MvcResult createResult = mockMvc.perform(post("/api/postmortems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid", notNullValue()))
                .andExpect(jsonPath("$.title", is("Functional Test Postmortem")))
                .andReturn();

        Postmortem createdPm = objectMapper.readValue(createResult.getResponse().getContentAsString(), Postmortem.class);
        UUID pmId = createdPm.getUuid();

        // 2. Read the postmortem
        mockMvc.perform(get("/api/postmortems/" + pmId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Functional Test Postmortem")))
                .andExpect(jsonPath("$.description", is("This is a postmortem created by a functional test.")));

        // 3. Update the postmortem
        createdPm.setTitle("Updated Functional Test Postmortem");
        mockMvc.perform(put("/api/postmortems/" + pmId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createdPm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Functional Test Postmortem")));

        // 4. Search for the postmortem
        mockMvc.perform(get("/api/postmortems/search").param("keyword", "Updated Functional"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[*].postmortem.title", hasItem("Updated Functional Test Postmortem")));

        // 5. Delete the postmortem
        mockMvc.perform(delete("/api/postmortems/" + pmId))
                .andExpect(status().isNoContent());

        // 6. Verify deletion (Service throws RuntimeException which controller translates to 404)
        mockMvc.perform(get("/api/postmortems/" + pmId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin_test", roles = "ADMIN")
    public void getAllPostmortems_shouldReturnList() throws Exception {
        com.melomarit.melopost.model.User adminUser = new com.melomarit.melopost.model.User();
        adminUser.setUsername("admin_test");
        adminUser.setPassword("password");
        adminUser.getRoles().add("ADMIN");
        adminUser.getAllowedDepartments().add("IT");
        userRepository.save(adminUser);

        Postmortem pm1 = new Postmortem();
        pm1.setTitle("PM 1");
        pm1.setDepartment("IT");
        postmortemRepository.save(pm1);

        Postmortem pm2 = new Postmortem();
        pm2.setTitle("PM 2");
        pm2.setDepartment("IT");
        postmortemRepository.save(pm2);

        mockMvc.perform(get("/api/postmortems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[*].title", hasItems("PM 1", "PM 2")));
    }
}
