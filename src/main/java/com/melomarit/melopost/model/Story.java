package com.melo.melopost.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;
import java.util.ArrayList;

@Data
@Entity
public class Story {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String storyNumber;
    private String teamName;
    private String backlogName;
    private String platform; // e.g., "Jira", "Azure DevOps"
    private String whatToFix;
    private String foundByDepartment;
    private String toSolveByDepartment;
    private String priority;
    private String managerName;
    private String storyLink; // URL to story
    private String status;
    private String notes;

    @ElementCollection
    @CollectionTable(name = "story_tags", joinColumns = @JoinColumn(name = "story_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStoryNumber() { return storyNumber; }
    public void setStoryNumber(String storyNumber) { this.storyNumber = storyNumber; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public String getBacklogName() { return backlogName; }
    public void setBacklogName(String backlogName) { this.backlogName = backlogName; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getWhatToFix() { return whatToFix; }
    public void setWhatToFix(String whatToFix) { this.whatToFix = whatToFix; }
    public String getFoundByDepartment() { return foundByDepartment; }
    public void setFoundByDepartment(String foundByDepartment) { this.foundByDepartment = foundByDepartment; }
    public String getToSolveByDepartment() { return toSolveByDepartment; }
    public void setToSolveByDepartment(String toSolveByDepartment) { this.toSolveByDepartment = toSolveByDepartment; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }
    public String getStoryLink() { return storyLink; }
    public void setStoryLink(String storyLink) { this.storyLink = storyLink; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
