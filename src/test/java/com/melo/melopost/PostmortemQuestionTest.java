package com.melo.melopost;

import com.melo.melopost.model.Postmortem;
import com.melo.melopost.model.PostmortemQuestion;
import com.melo.melopost.repository.PostmortemQuestionRepository;
import com.melo.melopost.service.PostmortemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PostmortemQuestionTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PostmortemService postmortemService;

    @Autowired
    private PostmortemQuestionRepository questionRepository;

    @Test
    void testAddAndRemoveQuestion() {
        // 1. Create a postmortem first or use existing one (ID 1 should exist)
        Long postmortemId = 1L;
        
        // 2. Add a question
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("cheeseLayer", "Define");
        map.add("question", "What was the initial trigger?");
        map.add("answer", "High CPU usage on the database.");
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity("/postmortems/" + postmortemId + "/questions/add", request, String.class);
        
        // TestRestTemplate follows redirects by default, so we might get 200 OK of the target page
        assertThat(response.getStatusCode().is2xxSuccessful() || response.getStatusCode() == HttpStatus.FOUND).isTrue();
        
        if (response.getStatusCode() == HttpStatus.OK) {
            assertThat(response.getBody()).contains("Analysis Questions");
            assertThat(response.getBody()).contains("What was the initial trigger?");
        } else {
            // Verify question is displayed
            response = restTemplate.getForEntity("/postmortems/details/" + postmortemId, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("Analysis Questions");
            assertThat(response.getBody()).contains("What was the initial trigger?");
        }
        assertThat(response.getBody()).contains("High CPU usage on the database.");
        assertThat(response.getBody()).contains("Define");

        // 4. Remove the question
        List<PostmortemQuestion> questions = questionRepository.findByPostmortemId(postmortemId);
        PostmortemQuestion q = questions.stream()
                .filter(question -> question.getQuestion().equals("What was the initial trigger?"))
                .findFirst()
                .orElseThrow();
        Long questionId = q.getId();

        response = restTemplate.postForEntity("/postmortems/" + postmortemId + "/questions/" + questionId + "/remove", null, String.class);
        assertThat(response.getStatusCode().is2xxSuccessful() || response.getStatusCode() == HttpStatus.FOUND).isTrue();

        // 5. Verify question is removed
        if (response.getStatusCode() == HttpStatus.FOUND) {
            response = restTemplate.getForEntity("/postmortems/details/" + postmortemId, String.class);
        }
        assertThat(response.getBody()).doesNotContain("What was the initial trigger?");
    }
}
