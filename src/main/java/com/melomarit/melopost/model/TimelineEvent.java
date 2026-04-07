package com.melomarit.melopost.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
public class TimelineEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime eventTime;
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
