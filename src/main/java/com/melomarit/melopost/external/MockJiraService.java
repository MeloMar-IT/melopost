package com.melomarit.melopost.external;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;

@Service
public class MockJiraService implements IncidentIntegrationService {

    @Override
    public List<ExternalIncident> searchIncidents(String query) {
        // In a real implementation, this would call Jira REST API
        List<ExternalIncident> results = new ArrayList<>();
        ExternalIncident incident = new ExternalIncident();
        incident.setId("JIRA-123");
        incident.setSummary("Critical Database Failure");
        incident.setDescription("Main database is down since 2 PM");
        incident.setCreatedDate(LocalDateTime.now().minusDays(1));
        incident.setSource("Jira");
        results.add(incident);
        return results;
    }

    @Override
    public ExternalIncident getIncidentDetails(String id) {
        ExternalIncident incident = new ExternalIncident();
        incident.setId(id);
        incident.setSummary("Incident " + id);
        incident.setDescription("Detailed description for " + id);
        incident.setSource("Jira");
        return incident;
    }
}
