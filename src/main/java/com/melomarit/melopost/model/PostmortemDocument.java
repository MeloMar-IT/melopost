package com.melomarit.melopost.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
public class PostmortemDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String contentType;
    private long size;
    private LocalDateTime uploadDate;

    @Lob
    @Column(columnDefinition = "BLOB")
    private byte[] data;

    @ManyToOne
    @JoinColumn(name = "postmortem_id")
    @JsonIgnore
    private Postmortem postmortem;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public LocalDateTime getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDateTime uploadDate) { this.uploadDate = uploadDate; }
    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
    public Postmortem getPostmortem() { return postmortem; }
    public void setPostmortem(Postmortem postmortem) { this.postmortem = postmortem; }

    @PrePersist
    protected void onCreate() {
        uploadDate = LocalDateTime.now();
    }
}
