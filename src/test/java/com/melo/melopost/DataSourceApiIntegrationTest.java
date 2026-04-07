package com.melo.melopost;

import com.melo.melopost.model.DataSource;
import com.melo.melopost.repository.DataSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DataSourceApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @BeforeEach
    void setUp() {
        dataSourceRepository.deleteAll();
    }

    @Test
    void testCreateDataSourceUnauthenticated() {
        DataSource ds = new DataSource();
        ds.setName("Test Source");
        ds.setType("IJRA");
        ResponseEntity<DataSource> response = restTemplate.postForEntity("/api/datasources", ds, DataSource.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
