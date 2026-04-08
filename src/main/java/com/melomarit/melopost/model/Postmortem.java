package com.melomarit.melopost.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Data
@Entity
public class Postmortem {
    public static final List<String> STANDARD_LAYERS = List.of(
            "Define", "Design", "Build", "Test", "Release", "Run", "Resilience", "Observability", "Incident Handling", "Human"
    );

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
    private String storyStore; // Application where stories are made
    private String department;
    private String failedApplication;
    private String type;
    
    public static final List<String> POSTMORTEM_TYPES = List.of(
            "Local postmortem", "Major Postmortem", "Orchestrated P1"
    );
    
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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "postmortem")
    private List<PostmortemDocument> documents = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "major_local_postmortems",
        joinColumns = @JoinColumn(name = "major_postmortem_id"),
        inverseJoinColumns = @JoinColumn(name = "local_postmortem_id")
    )
    private List<Postmortem> localPostmortems = new ArrayList<>();

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
    public String getStoryStore() { return storyStore; }
    public void setStoryStore(String storyStore) { this.storyStore = storyStore; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getFailedApplication() { return failedApplication; }
    public void setFailedApplication(String failedApplication) { this.failedApplication = failedApplication; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public List<TimelineEvent> getTimelineEvents() { return timelineEvents; }
    public void setTimelineEvents(List<TimelineEvent> timelineEvents) { this.timelineEvents = timelineEvents; }

    public List<PostmortemQuestion> getQuestions() { return questions; }
    public void setQuestions(List<PostmortemQuestion> questions) { this.questions = questions; }

    public List<PostmortemDocument> getDocuments() { return documents; }
    public void setDocuments(List<PostmortemDocument> documents) { this.documents = documents; }

    public List<Postmortem> getLocalPostmortems() { return localPostmortems; }
    public void setLocalPostmortems(List<Postmortem> localPostmortems) { this.localPostmortems = localPostmortems; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getTotalHoles() {
        if (layers == null) return 0;
        return layers.stream()
                .filter(l -> l.getHoles() != null)
                .mapToInt(l -> l.getHoles().size())
                .sum();
    }
}
