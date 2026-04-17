package com.melomarit.melopost.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melomarit.melopost.model.Postmortem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class DocumentImportMappingTest {

    private DocumentImportService documentImportService;

    @Mock
    private PostmortemService postmortemService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        documentImportService = new DocumentImportService(postmortemService, objectMapper);
        when(postmortemService.save(any(Postmortem.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void testMapping() throws Exception {
        String json = "{" +
                "\"title\": \"Original Title\"," +
                "\"description\": \"Original Description\"," +
                "\"incidentRef\": \"INC12345\"," +
                "\"teamName\": \"Database Department\"," +
                "\"businessImpact\": {" +
                "  \"service\": \"Database Application\"," +
                "  \"description\": \"Database Description\"" +
                "}" +
                "}";
        JsonNode root = objectMapper.readTree(json);

        // Access private method mapJsonToPostmortem via reflection
        Method mapMethod = DocumentImportService.class.getDeclaredMethod("mapJsonToPostmortem", JsonNode.class);
        mapMethod.setAccessible(true);
        Postmortem pm = (Postmortem) mapMethod.invoke(documentImportService, root);

        assertEquals("INC12345", pm.getIncidentRef());
        assertEquals("INC12345", pm.getTitle()); // Title should be Incident ID
        assertEquals("Service NOW read", pm.getIncidentSource());
        assertEquals("Azure DEV/OPS create new record", pm.getStoryStore());
        assertEquals("Database Application", pm.getFailedApplication());
        assertEquals("Database Description", pm.getDescription());
        assertEquals("Database Department", pm.getDepartment());
    }
}
