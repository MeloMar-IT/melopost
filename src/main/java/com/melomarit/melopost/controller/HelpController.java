package com.melomarit.melopost.controller;

import com.melomarit.melopost.dto.ApiEndpointDTO;
import com.melomarit.melopost.dto.DatabaseTableDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
public class HelpController {

    @Autowired
    private DatabaseAdminRestController databaseAdminRestController;

    @GetMapping("/help")
    public String help(@RequestParam(required = false, defaultValue = "userguide") String section, Model model) {
        model.addAttribute("currentSection", section);
        
        if ("api".equals(section)) {
            model.addAttribute("apiEndpoints", getApiEndpoints());
        } else if ("database".equals(section)) {
            model.addAttribute("databaseTables", databaseAdminRestController.getTablesInternal(false));
        }
        
        return "help";
    }

    private List<ApiEndpointDTO> getApiEndpoints() {
        List<ApiEndpointDTO> apis = new ArrayList<>();
        
        // Postmortems API
        apis.add(new ApiEndpointDTO("GET", "/api/postmortems", "List all postmortems", 
                "[{\"id\":1, \"title\":\"DB Outage\", \"type\":\"Major Postmortem\", \"incidentDate\":\"2026-04-09T10:00:00\", \"incidentRef\":\"INC-123\", \"department\":\"Engineering\"}]"));
        
        String fullPostmortemJson = "{\n" +
                "  \"id\": 1,\n" +
                "  \"title\": \"Production Database Outage\",\n" +
                "  \"description\": \"Detailed description of the incident...\",\n" +
                "  \"startDate\": \"2026-04-09T09:00:00\",\n" +
                "  \"incidentDate\": \"2026-04-09T10:00:00\",\n" +
                "  \"postMortemMeetingDate\": \"2026-04-12T14:00:00\",\n" +
                "  \"dueDate\": \"2026-04-30T23:59:59\",\n" +
                "  \"incidentRef\": \"INC-88273\",\n" +
                "  \"incidentSource\": \"ServiceNow\",\n" +
                "  \"storyStore\": \"Jira\",\n" +
                "  \"department\": \"Infrastructure\",\n" +
                "  \"failedApplication\": \"Core-API\",\n" +
                "  \"type\": \"Major Postmortem\",\n" +
                "  \"note\": \"Detailed analysis notes and findings...\",\n" +
                "  \"tags\": [\"database\", \"outage\", \"p1\"],\n" +
                "  \"layers\": [\n" +
                "    {\n" +
                "      \"id\": 101,\n" +
                "      \"name\": \"Test\",\n" +
                "      \"description\": \"Analysis of testing phase failures\",\n" +
                "      \"holes\": [\n" +
                "        {\n" +
                "          \"id\": 201,\n" +
                "          \"description\": \"Missing load test for DB migration\",\n" +
                "          \"teamName\": \"QA-Team\",\n" +
                "          \"remedialAction\": \"Add automated load tests to CI/CD\",\n" +
                "          \"actionStatus\": \"PENDING\",\n" +
                "          \"tags\": [\"automation\", \"load-test\"],\n" +
                "          \"story\": {\n" +
                "            \"id\": 301,\n" +
                "            \"storyNumber\": \"STORY-5542\",\n" +
                "            \"teamName\": \"QA-Team\",\n" +
                "            \"backlogName\": \"QA Backlog\",\n" +
                "            \"platform\": \"Jira\",\n" +
                "            \"whatToFix\": \"Implement load testing suite\",\n" +
                "            \"foundByDepartment\": \"Infrastructure\",\n" +
                "            \"toSolveByDepartment\": \"QA\",\n" +
                "            \"priority\": \"High\",\n" +
                "            \"managerName\": \"John Doe\",\n" +
                "            \"storyLink\": \"https://jira.example.com/browse/STORY-5542\",\n" +
                "            \"status\": \"In Progress\",\n" +
                "            \"notes\": \"Initial research started\",\n" +
                "            \"tags\": [\"performance\"]\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"timelineEvents\": [\n" +
                "    {\n" +
                "      \"id\": 401,\n" +
                "      \"eventTime\": \"2026-04-09T10:05:00\",\n" +
                "      \"description\": \"Alert triggered: DB connection pool exhausted\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"createdAt\": \"2026-04-09T11:00:00\",\n" +
                "  \"updatedAt\": \"2026-04-09T11:15:00\"\n" +
                "}";
        
        apis.add(new ApiEndpointDTO("GET", "/api/postmortems/{uuid}", "Get details for a specific postmortem", fullPostmortemJson));
        apis.add(new ApiEndpointDTO("GET", "/api/postmortems/recent", "List 5 most recent postmortems",
                "[{\"uuid\":\"550e8400-e29b-41d4-a716-446655440000\", \"title\":\"Recent Incident\", \"incidentRef\":\"INC-9999\"}]"));
        apis.add(new ApiEndpointDTO("GET", "/api/postmortems/search?keyword=xxx", "Search postmortems by title/ref/desc/tags",
                "[{\"uuid\":\"550e8400-e29b-41d4-a716-446655440000\", \"title\":\"Search Result\", \"incidentRef\":\"SEARCH-1\"}]"));
        apis.add(new ApiEndpointDTO("POST", "/api/postmortems", "Create a new postmortem",
                "{\n" +
                "  \"title\": \"New Incident\",\n" +
                "  \"description\": \"Brief description...\",\n" +
                "  \"type\": \"Local postmortem\",\n" +
                "  \"incidentDate\": \"2026-04-09T11:00:00\",\n" +
                "  \"incidentRef\": \"INC-1002\",\n" +
                "  \"incidentSource\": \"ServiceNow\",\n" +
                "  \"department\": \"IT\",\n" +
                "  \"failedApplication\": \"WebPortal\",\n" +
                "  \"tags\": [\"frontend\", \"login\"]\n" +
                "}"));
        apis.add(new ApiEndpointDTO("PUT", "/api/postmortems/{uuid}", "Update an existing postmortem",
                "{\"title\":\"Updated Incident Title\", \"note\":\"Added more analysis notes.\"}"));
        apis.add(new ApiEndpointDTO("DELETE", "/api/postmortems/{uuid}", "Delete a postmortem",
                "{\"message\":\"Postmortem deleted successfully\"}"));
        apis.add(new ApiEndpointDTO("GET", "/api/postmortems/{uuid}/report", "Download PDF report for a postmortem",
                "Binary PDF Stream"));

        // Users API
        apis.add(new ApiEndpointDTO("GET", "/api/users", "List all users",
                "[{\"uuid\":\"550e8400-e29b-41d4-a716-446655440001\", \"username\":\"admin\", \"email\":\"admin@example.com\"}]"));
        apis.add(new ApiEndpointDTO("GET", "/api/users/{uuid}", "Get a specific user",
                "{\"uuid\":\"550e8400-e29b-41d4-a716-446655440001\", \"username\":\"admin\", \"roles\":[\"ADMIN\"]}"));
        apis.add(new ApiEndpointDTO("POST", "/api/users", "Create a new user",
                "{\n" +
                "  \"username\": \"jdoe\",\n" +
                "  \"password\": \"p@ssword123\",\n" +
                "  \"email\": \"john.doe@example.com\",\n" +
                "  \"fullName\": \"John Doe\",\n" +
                "  \"roles\": [\"USER\"]\n" +
                "}"));
        apis.add(new ApiEndpointDTO("PUT", "/api/users/{uuid}", "Update an existing user",
                "{\"email\":\"updated@example.com\", \"active\":true}"));
        apis.add(new ApiEndpointDTO("DELETE", "/api/users/{uuid}", "Delete a user",
                "{\"message\":\"User deleted successfully\"}"));

        // DataSources API
        apis.add(new ApiEndpointDTO("GET", "/api/datasources", "List all data sources",
                "[{\"uuid\":\"550e8400-e29b-41d4-a716-446655440002\", \"name\":\"Local ServiceNow\", \"type\":\"ServiceNow\"}]"));
        apis.add(new ApiEndpointDTO("GET", "/api/datasources/{uuid}", "Get a specific data source",
                "{\"uuid\":\"550e8400-e29b-41d4-a716-446655440002\", \"name\":\"Local ServiceNow\", \"url\":\"http://sn.example.com\"}"));
        apis.add(new ApiEndpointDTO("POST", "/api/datasources", "Create a new data source",
                "{\n" +
                "  \"name\": \"Operations ServiceNow\",\n" +
                "  \"type\": \"ServiceNow\",\n" +
                "  \"url\": \"https://example.service-now.com\",\n" +
                "  \"description\": \"Main production instance\",\n" +
                "  \"operation\": \"INCIDENT_SYNC\"\n" +
                "}"));
        apis.add(new ApiEndpointDTO("PUT", "/api/datasources/{uuid}", "Update an existing data source",
                "{\"description\":\"Updated connection settings\"}"));
        apis.add(new ApiEndpointDTO("DELETE", "/api/datasources/{uuid}", "Delete a data source",
                "{\"message\":\"Data source deleted successfully\"}"));
        apis.add(new ApiEndpointDTO("GET", "/api/datasources/templates", "List template data sources",
                "[{\"name\":\"ServiceNow Template\", \"type\":\"ServiceNow\"}]"));

        // Admin Database API
        apis.add(new ApiEndpointDTO("GET", "/api/admin/database/tables", "List all Cassandra tables (ADMIN)",
                "[{\"name\":\"postmortem\", \"rowCount\":25}]"));
        apis.add(new ApiEndpointDTO("GET", "/api/admin/database/table/{name}", "Get details for a specific table (ADMIN)",
                "{\"name\":\"users\", \"columns\":[\"uuid\",\"username\"], \"data\":[[\"550e8400-e29b-41d4-a716-446655440001\",\"admin\"]]}"));
        apis.add(new ApiEndpointDTO("POST", "/api/admin/database/query", "Execute raw SQL query (ADMIN)",
                "{\n" +
                "  \"query\": \"SELECT * FROM USERS WHERE ACTIVE = TRUE\",\n" +
                "  \"results\": []\n" +
                "}"));

        return apis;
    }
}
