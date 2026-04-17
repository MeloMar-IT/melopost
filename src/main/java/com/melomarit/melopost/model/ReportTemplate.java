package com.melomarit.melopost.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.cassandra.core.mapping.Column;
import lombok.Data;
import java.util.UUID;

@Data
@Table("report_template")
public class ReportTemplate {
    @PrimaryKey
    @Column("uuid")
    private UUID uuid = UUID.randomUUID();

    @Column("name")
    private String name;

    @Column("content")
    private String content;

    @Column("is_default")
    private boolean isDefault;

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { this.isDefault = aDefault; }

    public boolean getIsDefault() { return isDefault; }
    public void setIsDefault(boolean isDefault) { this.isDefault = isDefault; }
}
