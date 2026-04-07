package com.melomarit.melopost.external;

import org.springframework.stereotype.Service;

@Service
public class MockAzureDevOpsService implements ExternalIssueTracker {
    @Override
    public String createStory(String summary, String description) {
        // Mocking Azure DevOps story creation
        return "ADO-" + (int)(Math.random() * 1000);
    }
}
