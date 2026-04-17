package com.melomarit.melopost.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.CassandraType.Name;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Table("postmortem_document")
public class PostmortemDocument {
    @PrimaryKey
    @Column("uuid")
    private UUID uuid = UUID.randomUUID();

    @Column("file_name")
    private String fileName;

    @Column("content_type")
    private String contentType;

    @Column("size")
    private long size;

    @Column("upload_date")
    private LocalDateTime uploadDate;

    @Column("data")
    @CassandraType(type = Name.BLOB)
    private byte[] data;

    @Column("postmortem_uuid")
    private UUID postmortemUuid;

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
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
    public UUID getPostmortemUuid() { return postmortemUuid; }
    public void setPostmortemUuid(UUID postmortemUuid) { this.postmortemUuid = postmortemUuid; }

    public void onCreate() {
        uploadDate = LocalDateTime.now();
    }
}
