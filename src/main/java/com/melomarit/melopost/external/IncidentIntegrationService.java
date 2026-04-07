package com.melomarit.melopost.external;

import java.util.List;

public interface IncidentIntegrationService {
    List<ExternalIncident> searchIncidents(String query);
    ExternalIncident getIncidentDetails(String id);
}
