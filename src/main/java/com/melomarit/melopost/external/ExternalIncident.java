package com.melo.melopost.external;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ExternalIncident {
    private String id;
    private String summary;
    private String description;
    private LocalDateTime createdDate;
    private String source; // ServiceNow, Jira

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
