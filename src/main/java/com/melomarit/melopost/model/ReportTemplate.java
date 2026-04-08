package com.melomarit.melopost.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class ReportTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_default", nullable = false, columnDefinition = "boolean default false")
    private boolean isDefault;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { this.isDefault = aDefault; }
}
