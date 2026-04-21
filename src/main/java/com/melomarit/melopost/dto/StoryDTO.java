package com.melomarit.melopost.dto;

import com.melomarit.melopost.model.StoryUDT;
import com.melomarit.melopost.model.Postmortem;
import java.util.UUID;

public class StoryDTO {
    private StoryUDT story;
    private UUID postmortemId;
    private String postmortemTitle;
    private String holeDescription;

    public StoryDTO() {}

    public StoryDTO(StoryUDT story, Postmortem postmortem, String holeDescription) {
        this.story = story;
        if (postmortem != null) {
            this.postmortemId = postmortem.getUuid();
            this.postmortemTitle = postmortem.getTitle();
        }
        this.holeDescription = holeDescription;
    }

    public StoryUDT getStory() { return story; }
    public void setStory(StoryUDT story) { this.story = story; }

    public UUID getPostmortemId() { return postmortemId; }
    public void setPostmortemId(UUID postmortemId) { this.postmortemId = postmortemId; }

    public String getPostmortemTitle() { return postmortemTitle; }
    public void setPostmortemTitle(String postmortemTitle) { this.postmortemTitle = postmortemTitle; }

    public String getHoleDescription() { return holeDescription; }
    public void setHoleDescription(String holeDescription) { this.holeDescription = holeDescription; }
}
