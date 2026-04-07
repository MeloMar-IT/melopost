package com.melo.melopost;

import com.melo.melopost.model.Postmortem;
import com.melo.melopost.repository.PostmortemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PostmortemTagViewControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PostmortemRepository repository;

    @Test
    void testAddAndRemoveTag() {
        // Find a postmortem created by TestDataInitializer
        Postmortem pm = repository.findAll().get(0);
        Long id = pm.getId();
        // The list might be lazy, but it's okay because we are in the same JVM here if it's not transactional...
        // Wait, repository.findAll() doesn't guarantee a session is open after it returns.

        // Add a tag
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("tag", "new-test-tag");
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/postmortems/" + id + "/tags/add", request, String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("new-test-tag");

        // Verify tag added
        Optional<Postmortem> updatedPm = repository.findById(id);
        assertThat(updatedPm).isPresent();
        assertThat(updatedPm.get().getTags()).contains("new-test-tag");

        // Remove the tag
        MultiValueMap<String, String> removeMap = new LinkedMultiValueMap<>();
        removeMap.add("tag", "new-test-tag");
        HttpEntity<MultiValueMap<String, String>> removeRequest = new HttpEntity<>(removeMap, headers);

        response = restTemplate.postForEntity("/postmortems/" + id + "/tags/remove", removeRequest, String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).doesNotContain("new-test-tag");

        // Verify tag removed
        updatedPm = repository.findById(id);
        assertThat(updatedPm).isPresent();
        assertThat(updatedPm.get().getTags()).doesNotContain("new-test-tag");
    }
}
