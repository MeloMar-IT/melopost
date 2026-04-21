package com.melomarit.melopost.model;

import org.springframework.data.cassandra.core.mapping.UserDefinedType;
import org.springframework.data.cassandra.core.mapping.Column;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@UserDefinedType("service_impact")
public class ServiceImpactUDT {
    private UUID uuid = UUID.randomUUID();
    private String service;
    private String country;
    @Column("start_time")
    private LocalDateTime startTime;
    @Column("end_time")
    private LocalDateTime endTime;
    private String duration;
    @Column("impact_description")
    private String impactDescription;

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public String getImpactDescription() { return impactDescription; }
    public void setImpactDescription(String impactDescription) { this.impactDescription = impactDescription; }
}
