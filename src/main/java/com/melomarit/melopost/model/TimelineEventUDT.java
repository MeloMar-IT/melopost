package com.melomarit.melopost.model;

import org.springframework.data.cassandra.core.mapping.UserDefinedType;
import org.springframework.data.cassandra.core.mapping.Column;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@UserDefinedType("timeline_event")
public class TimelineEventUDT {
    private UUID uuid = UUID.randomUUID();
    @Column("eventtime")
    private LocalDateTime eventTime;
    private String description;

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
