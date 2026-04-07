package com.melo.melopost;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PostmortemViewControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testIndexPage() {
        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("MeloPost");
        assertThat(response.getBody()).contains("Dashboard");
    }

    @Test
    void testPostmortemListPage() {
        ResponseEntity<String> response = restTemplate.getForEntity("/postmortems", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Postmortems");
    }

    @Test
    void testNewPostmortemPage() {
        ResponseEntity<String> response = restTemplate.getForEntity("/postmortems/new", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Create New Postmortem");
        assertThat(response.getBody()).contains("Design");
        assertThat(response.getBody()).contains("Build");
        assertThat(response.getBody()).contains("Release");
    }

    @Test
    void testViewPostmortemPage() {
        // ID 1 should exist due to TestDataInitializer
        ResponseEntity<String> response = restTemplate.getForEntity("/postmortems/details/1", String.class);
        if (response.getStatusCode() != HttpStatus.OK) {
            System.out.println("[DEBUG_LOG] Response body: " + response.getBody());
        }
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Timeline");
        assertThat(response.getBody()).contains("Event 1: Progress during incident handling.");
    }

    @Test
    void testEditPostmortemPage() {
        ResponseEntity<String> response = restTemplate.getForEntity("/postmortems/edit/1", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
