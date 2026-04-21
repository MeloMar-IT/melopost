package com.melomarit.melopost.model;

import org.springframework.data.cassandra.core.mapping.UserDefinedType;
import org.springframework.data.cassandra.core.mapping.Column;
import lombok.Data;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@Data
@UserDefinedType("hole")
public class HoleUDT {
    private UUID uuid = UUID.randomUUID();

    private String description;

    @Column("teamname")
    private String teamName;

    @Column("remedialaction")
    private String remedialAction;

    @Column("actionstatus")
    private String actionStatus;

    private List<String> tags = new ArrayList<>();

    private StoryUDT story;

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public String getRemedialAction() { return remedialAction; }
    public void setRemedialAction(String remedialAction) { this.remedialAction = remedialAction; }
    public String getActionStatus() { return actionStatus; }
    public void setActionStatus(String actionStatus) { this.actionStatus = actionStatus; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public StoryUDT getStory() { return story; }
    public void setStory(StoryUDT story) { this.story = story; }
}
