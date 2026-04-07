package com.melomarit.melopost.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;
import java.util.ArrayList;

@Data
@Entity
public class Hole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;
    private String teamName;
    private String remedialAction;
    private String actionStatus; // e.g., "PENDING", "COMPLETED"

    @ElementCollection
    @CollectionTable(name = "hole_tags", joinColumns = @JoinColumn(name = "hole_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "story_id")
    private Story story;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public String getRemedialAction() { return remedialAction; }
    public void setRemedialAction(String remedialAction) { this.remedialAction = remedialAction; }
    public String getActionStatus() { return actionStatus; }
    public void setActionStatus(String actionStatus) { this.actionStatus = actionStatus; }
    public Story getStory() { return story; }
    public void setStory(Story story) { this.story = story; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
