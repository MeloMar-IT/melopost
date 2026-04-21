package com.melomarit.melopost.model;

import org.springframework.data.cassandra.core.mapping.UserDefinedType;
import org.springframework.data.cassandra.core.mapping.Column;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@UserDefinedType("cbs_impact")
public class CbsImpactUDT {
    private UUID uuid = UUID.randomUUID();
    @Column("cbs_name")
    private String cbsName;
    @Column("it_services")
    private String itServices;
    @Column("start_time")
    private LocalDateTime startTime;
    @Column("end_time")
    private LocalDateTime endTime;
    private String duration;
    @Column("tolerance_level_exceeded")
    private String toleranceLevelExceeded;

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    public String getCbsName() { return cbsName; }
    public void setCbsName(String cbsName) { this.cbsName = cbsName; }
    public String getItServices() { return itServices; }
    public void setItServices(String itServices) { this.itServices = itServices; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public String getToleranceLevelExceeded() { return toleranceLevelExceeded; }
    public void setToleranceLevelExceeded(String toleranceLevelExceeded) { this.toleranceLevelExceeded = toleranceLevelExceeded; }
}
