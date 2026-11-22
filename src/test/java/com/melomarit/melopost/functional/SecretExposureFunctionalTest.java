package com.melomarit.melopost.functional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melomarit.melopost.model.DataSource;
import com.melomarit.melopost.model.User;
import com.melomarit.melopost.repository.DataSourceRepository;
import com.melomarit.melopost.repository.UserRepository;
import com.melomarit.melopost.service.SecretCipher;
import com.melomarit.melopost.support.AbstractCassandraIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "melopost.datasource.key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecretExposureFunctionalTest extends AbstractCassandraIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DataSourceRepository dataSourceRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private SecretCipher secretCipher;
    @Autowired private CassandraOperations cassandraOperations;

    @AfterEach
    void cleanup() {
        dataSourceRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req) throws Exception {
        return mockMvc.perform(req.with(csrf())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private void assertNoSecret(String json, String... forbidden) {
        assertFalse(json.contains("\"password\""), "response must not contain a password field: " + json);
        for (String f : forbidden) {
            assertFalse(json.contains(f), "response must not contain " + f);
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void dataSourceTokenIsNeverReturnedAndStoredEncrypted() throws Exception {
        String created = send(post("/api/datasources").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Jira\",\"password\":\"secret-123\"}"));
        assertNoSecret(created, "secret-123");
        UUID id = UUID.fromString(objectMapper.readTree(created).get("uuid").asText());

        assertNoSecret(send(get("/api/datasources")), "secret-123", "enc:v1:");
        assertNoSecret(send(get("/api/datasources/" + id)), "secret-123", "enc:v1:");

        String stored = dataSourceRepository.findById(id).orElseThrow().getPassword();
        assertTrue(stored.startsWith("enc:v1:"));
        assertEquals("secret-123", secretCipher.decrypt(stored));

        assertNoSecret(send(put("/api/datasources/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Jira 2\"}")), "secret-123");
        String after = dataSourceRepository.findById(id).orElseThrow().getPassword();
        assertEquals(stored, after);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void userPasswordIsNeverReturnedAndAlwaysEncoded() throws Exception {
        String created = send(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"u1\",\"password\":\"user-pass-1\"}"));
        assertNoSecret(created, "$2a$");
        UUID id = UUID.fromString(objectMapper.readTree(created).get("uuid").asText());
        assertNoSecret(send(get("/api/users")), "$2a$");
        assertNoSecret(send(get("/api/users/" + id)), "$2a$");

        String hash = userRepository.findById(id).orElseThrow().getPassword();
        assertTrue(passwordEncoder.matches("user-pass-1", hash));

        send(put("/api/users/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"a@b.c\"}"));
        assertEquals(hash, userRepository.findById(id).orElseThrow().getPassword());

        String fake = "$2a$10$abcdefghijklmnopqrstuvABCDEFGHIJKLMNOPQRSTUVWXYZ01234";
        String created2 = send(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"u2\",\"password\":\"" + fake + "\"}"));
        UUID id2 = UUID.fromString(objectMapper.readTree(created2).get("uuid").asText());
        String stored2 = userRepository.findById(id2).orElseThrow().getPassword();
        assertNotEquals(fake, stored2);
        assertTrue(passwordEncoder.matches(fake, stored2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void databaseConsoleMasksPasswordColumns() throws Exception {
        User u = new User();
        u.setUsername("consoleuser");
        u.setPassword(passwordEncoder.encode("x"));
        userRepository.save(u);
        DataSource ds = new DataSource();
        ds.setName("c");
        ds.setPassword(secretCipher.encrypt("tok"));
        dataSourceRepository.save(ds);

        String table = send(get("/api/admin/database/table/users"));
        assertFalse(table.contains("$2a$"));
        assertTrue(table.contains("***"));
        String dsTable = send(get("/api/admin/database/table/data_source"));
        assertFalse(dsTable.contains("enc:v1:"));
        assertTrue(dsTable.contains("***"));
    }

    @Test
    void conditionalUpdateDoesNotOverwriteChangesOrRecreateDeletedRows() {
        DataSource ds = new DataSource();
        ds.setName("m");
        ds.setPassword("old");
        dataSourceRepository.save(ds);
        String cql = "UPDATE data_source SET password = ? WHERE uuid = ? IF password = ?";

        dataSourceRepository.save(changed(ds, "changed"));
        assertFalse(cassandraOperations.getCqlOperations().execute(cql, "enc", ds.getUuid(), "old"));
        assertEquals("changed", dataSourceRepository.findById(ds.getUuid()).orElseThrow().getPassword());

        dataSourceRepository.deleteById(ds.getUuid());
        assertFalse(cassandraOperations.getCqlOperations().execute(cql, "enc", ds.getUuid(), "changed"));
        assertTrue(dataSourceRepository.findById(ds.getUuid()).isEmpty());
    }

    private DataSource changed(DataSource ds, String password) {
        ds.setPassword(password);
        return ds;
    }
}
