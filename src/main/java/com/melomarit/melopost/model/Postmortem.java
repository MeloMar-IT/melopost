package com.melomarit.melopost.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;
import org.springframework.data.cassandra.core.mapping.CassandraType.Name;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@Data
@Table("postmortem")
public class Postmortem {
    public static final List<String> STANDARD_LAYERS = List.of(
            "Define", "Design", "Build", "Test", "Release", "Run", "Resilience", "Observability", "Incident Handling", "Human"
    );

    @PrimaryKey
    @Column("uuid")
    private UUID uuid = UUID.randomUUID();

    @Column("title")
    private String title;

    @Column("description")
    private String description;

    @Column("start_date")
    private LocalDateTime startDate;

    @Column("incident_date")
    private LocalDateTime incidentDate;

    @Column("post_mortem_meeting_date")
    private LocalDateTime postMortemMeetingDate;

    @Column("due_date")
    private LocalDateTime dueDate;

    @Column("incident_ref")
    private String incidentRef;

    @Column("incident_source")
    private String incidentSource;

    @Column("story_store")
    private String storyStore;

    @Column("department")
    private String department;

    @Column("failed_application")
    private String failedApplication;

    @Column("type")
    private String type;

    @Column("note")
    private String note;
    
    @Column("status")
    private String status = "Triggered";

    public static final List<String> POSTMORTEM_STATES = List.of(
            "Triggered", "In Analysis", "Actioned", "Published"
    );
    
    public static final List<String> POSTMORTEM_TYPES = List.of(
            "Local postmortem", "Major Postmortem", "Orchestrated P1"
    );
    
    @Column("tags")
    private List<String> tags = new ArrayList<>();

    @Column("layers")
    @CassandraType(type = CassandraType.Name.LIST, typeArguments = CassandraType.Name.UDT, userTypeName = "cheese_layer")
    private List<CheeseLayerUDT> layers = new ArrayList<>();

    @Column("timeline_events")
    @CassandraType(type = CassandraType.Name.LIST, typeArguments = CassandraType.Name.UDT, userTypeName = "timeline_event")
    private List<TimelineEventUDT> timelineEvents = new ArrayList<>();

    @Column("questions")
    @CassandraType(type = CassandraType.Name.LIST, typeArguments = CassandraType.Name.UDT, userTypeName = "postmortem_question")
    private List<PostmortemQuestionUDT> questions = new ArrayList<>();

    // Documents are stored in a separate table, but we can keep a list of their UUIDs if needed
    // For now, let's keep them separate as they were and managed by PostmortemDocumentRepository
    @Column("document_uuids")
    private List<UUID> documentUuids = new ArrayList<>();

    @Column("local_postmortem_uuids")
    private List<UUID> localPostmortemUuids = new ArrayList<>();

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    public void addLayer(CheeseLayerUDT layer) {
        layers.add(layer);
    }

    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
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
    public List<CheeseLayerUDT> getLayers() {
        if (layers == null) layers = new ArrayList<>();
        return layers;
    }
    public void setLayers(List<CheeseLayerUDT> layers) { this.layers = layers; }
    
    public List<String> getTags() {
        if (tags == null) tags = new ArrayList<>();
        return tags;
    }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<TimelineEventUDT> getTimelineEvents() {
        if (timelineEvents == null) timelineEvents = new ArrayList<>();
        return timelineEvents;
    }
    public void setTimelineEvents(List<TimelineEventUDT> timelineEvents) { this.timelineEvents = timelineEvents; }

    public List<PostmortemQuestionUDT> getQuestions() {
        if (questions == null) questions = new ArrayList<>();
        return questions;
    }
    public void setQuestions(List<PostmortemQuestionUDT> questions) { this.questions = questions; }

    public List<UUID> getDocumentUuids() {
        if (documentUuids == null) documentUuids = new ArrayList<>();
        return documentUuids;
    }
    public void setDocumentUuids(List<UUID> documentUuids) { this.documentUuids = documentUuids; }

    public List<UUID> getLocalPostmortemUuids() {
        if (localPostmortemUuids == null) localPostmortemUuids = new ArrayList<>();
        return localPostmortemUuids;
    }
    public void setLocalPostmortemUuids(List<UUID> localPostmortemUuids) { this.localPostmortemUuids = localPostmortemUuids; }
    
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
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
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

    // Helper method to keep compatibility with some code that might expect getDocuments()
    // though it's better to fetch them from service
    @org.springframework.data.annotation.Transient
    private List<PostmortemDocument> documents = new ArrayList<>();
    public List<PostmortemDocument> getDocuments() { return documents; }
    public void setDocuments(List<PostmortemDocument> documents) { this.documents = documents; }

    @org.springframework.data.annotation.Transient
    private List<Postmortem> localPostmortems = new ArrayList<>();
    public List<Postmortem> getLocalPostmortems() { return localPostmortems; }
    public void setLocalPostmortems(List<Postmortem> localPostmortems) { this.localPostmortems = localPostmortems; }
}
