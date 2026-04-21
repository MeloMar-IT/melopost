package com.melomarit.melopost.model;

import org.springframework.data.cassandra.core.mapping.UserDefinedType;
import org.springframework.data.cassandra.core.mapping.Column;
import lombok.Data;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@Data
@UserDefinedType("story")
public class StoryUDT {
    private UUID uuid = UUID.randomUUID();

    @Column("storynumber")
    private String storyNumber;

    @Column("teamname")
    private String teamName;

    @Column("backlogname")
    private String backlogName;

    private String platform;

    @Column("whattofix")
    private String whatToFix;

    @Column("foundbydepartment")
    private String foundByDepartment;

    @Column("tosolvebydepartment")
    private String toSolveByDepartment;

    private String priority;

    @Column("managername")
    private String managerName;

    @Column("storylink")
    private String storyLink;

    private String status;

    private String notes;

    private List<String> tags = new ArrayList<>();

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
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
