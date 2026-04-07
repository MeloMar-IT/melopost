package com.melo.melopost;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DataSourceViewControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testDataSourceListPage() {
        ResponseEntity<String> response = restTemplate.getForEntity("/datasources", String.class);
        if (response.getStatusCode() != HttpStatus.OK) {
            System.out.println("[DEBUG_LOG] Response status: " + response.getStatusCode());
            System.out.println("[DEBUG_LOG] Response body: " + response.getBody());
        }
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("External Integrations");
    }
}
