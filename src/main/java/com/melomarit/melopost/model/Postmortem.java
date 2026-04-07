package com.melo.melopost.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Data
@Entity
public class Postmortem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime incidentDate;
    private LocalDateTime postMortemMeetingDate;
    private LocalDateTime dueDate;
    private String incidentRef; // ServiceNow or Jira ID
    private String incidentSource; // e.g., "ServiceNow", "Jira"
    private String storyApplication; // Application where stories are made
    
    @ElementCollection
    @CollectionTable(name = "postmortem_tags", joinColumns = @JoinColumn(name = "postmortem_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "postmortem_id")
    private List<CheeseLayer> layers = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "postmortem_id")
    private List<TimelineEvent> timelineEvents = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "postmortem")
    private List<PostmortemQuestion> questions = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void addLayer(CheeseLayer layer) {
        layers.add(layer);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getIncidentDate() { return incidentDate; }
    public void setIncidentDate(LocalDateTime incidentDate) {
        this.incidentDate = incidentDate;
        if (incidentDate != null) {
            this.dueDate = incidentDate.plusWeeks(3);
        } else {
            this.dueDate = null;
        }
    }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getPostMortemMeetingDate() { return postMortemMeetingDate; }
    public void setPostMortemMeetingDate(LocalDateTime postMortemMeetingDate) { this.postMortemMeetingDate = postMortemMeetingDate; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<CheeseLayer> getLayers() { return layers; }
    public void setLayers(List<CheeseLayer> layers) { this.layers = layers; }
    public String getIncidentRef() { return incidentRef; }
    public void setIncidentRef(String incidentRef) { this.incidentRef = incidentRef; }
    public String getIncidentSource() { return incidentSource; }
    public void setIncidentSource(String incidentSource) { this.incidentSource = incidentSource; }
    public String getStoryApplication() { return storyApplication; }
    public void setStoryApplication(String storyApplication) { this.storyApplication = storyApplication; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public List<TimelineEvent> getTimelineEvents() { return timelineEvents; }
    public void setTimelineEvents(List<TimelineEvent> timelineEvents) { this.timelineEvents = timelineEvents; }

    public List<PostmortemQuestion> getQuestions() { return questions; }
    public void setQuestions(List<PostmortemQuestion> questions) { this.questions = questions; }

    public int getTotalHoles() {
        if (layers == null) return 0;
        return layers.stream()
                .filter(l -> l.getHoles() != null)
                .mapToInt(l -> l.getHoles().size())
                .sum();
    }
}
