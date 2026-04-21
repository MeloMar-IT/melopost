package com.melomarit.melopost.dto;

import com.melomarit.melopost.model.HoleUDT;
import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.model.StoryUDT;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

public class HoleDTO {
    private UUID uuid;
    private String description;
    private String teamName;
    private String remedialAction;
    private String actionStatus;
    private List<String> tags;
    private StoryUDT story;
    
    // Parent context
    private UUID postmortemId;
    private String postmortemTitle;
    private String layerName;

    public HoleDTO() {
    }

    public HoleDTO(UUID uuid, String description, String teamName, String remedialAction, String actionStatus, List<String> tags, StoryUDT story, UUID postmortemId, String postmortemTitle, String layerName) {
        this.uuid = uuid;
        this.description = description;
        this.teamName = teamName;
        this.remedialAction = remedialAction;
        this.actionStatus = actionStatus;
        this.tags = tags;
        this.story = story;
        this.postmortemId = postmortemId;
        this.postmortemTitle = postmortemTitle;
        this.layerName = layerName;
    }

    public HoleDTO(HoleUDT hole, Postmortem postmortem, String layerName) {
        this.uuid = hole.getUuid();
        this.description = hole.getDescription();
        this.teamName = hole.getTeamName();
        this.remedialAction = hole.getRemedialAction();
        this.actionStatus = hole.getActionStatus();
        this.tags = hole.getTags();
        this.story = hole.getStory();
        
        if (postmortem != null) {
            this.postmortemId = postmortem.getUuid();
            this.postmortemTitle = postmortem.getTitle();
        }
        this.layerName = layerName;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getRemedialAction() {
        return remedialAction;
    }

    public void setRemedialAction(String remedialAction) {
        this.remedialAction = remedialAction;
    }

    public String getActionStatus() {
        return actionStatus;
    }

    public void setActionStatus(String actionStatus) {
        this.actionStatus = actionStatus;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public StoryUDT getStory() {
        return story;
    }

    public void setStory(StoryUDT story) {
        this.story = story;
    }

    public UUID getPostmortemId() {
        return postmortemId;
    }

    public void setPostmortemId(UUID postmortemId) {
        this.postmortemId = postmortemId;
    }

    public String getPostmortemTitle() {
        return postmortemTitle;
    }

    public void setPostmortemTitle(String postmortemTitle) {
        this.postmortemTitle = postmortemTitle;
    }

    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }
}
