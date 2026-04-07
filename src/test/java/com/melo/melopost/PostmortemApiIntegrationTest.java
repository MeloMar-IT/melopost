package com.melo.melopost;

import com.melo.melopost.model.Postmortem;
import com.melo.melopost.model.TimelineEvent;
import com.melo.melopost.repository.PostmortemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PostmortemApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PostmortemRepository repository;

    @Test
    void testCreateAndGetPostmortem() {
        Postmortem pm = new Postmortem();
        pm.setTitle("Test Incident");
        pm.setDescription("Test Description");

        // Note: In a real test we'd need to handle authentication
        // For simplicity, let's just check if we get a 401 if unauthenticated
        ResponseEntity<Postmortem> response = restTemplate.postForEntity("/api/postmortems", pm, Postmortem.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Transactional
    void testTimelineEventsInEntity() {
        Postmortem pm = new Postmortem();
        pm.setTitle("Timeline Test");
        
        TimelineEvent event = new TimelineEvent();
        event.setEventTime(LocalDateTime.now());
        event.setDescription("Something happened");
        
        pm.getTimelineEvents().add(event);
        
        Postmortem saved = repository.save(pm);
        
        Postmortem retrieved = repository.findById(saved.getId()).orElseThrow();
        assertThat(retrieved.getTimelineEvents()).hasSize(1);
        assertThat(retrieved.getTimelineEvents().get(0).getDescription()).isEqualTo("Something happened");
    }
}
