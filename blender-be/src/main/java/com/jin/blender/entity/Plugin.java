package com.jin.blender.entity;

import com.jin.blender.support.ReviewStatus;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "plugin")
public class Plugin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "blender_version", nullable = false)
    private String blenderVersion;

    @Column(name = "file_urls", columnDefinition = "TEXT")
    private String fileUrls;

    @Column(name = "file_size")
    private Double fileSize;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;

    @Column(name = "quark_url", columnDefinition = "TEXT")
    private String quarkUrl;

    @Column(name = "extraction_code")
    private String extractionCode;

    @Column(name = "sync_status")
    private String syncStatus;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    // 无参构造
    public Plugin() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBlenderVersion() {
        return blenderVersion;
    }

    public void setBlenderVersion(String blenderVersion) {
        this.blenderVersion = blenderVersion;
    }

    public String getFileUrls() {
        return fileUrls;
    }

    public void setFileUrls(String fileUrls) {
        this.fileUrls = fileUrls;
    }

    public Double getFileSize() {
        return fileSize;
    }

    public void setFileSize(Double fileSize) {
        this.fileSize = fileSize;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getQuarkUrl() {
        return quarkUrl;
    }

    public void setQuarkUrl(String quarkUrl) {
        this.quarkUrl = quarkUrl;
    }

    public String getExtractionCode() {
        return extractionCode;
    }

    public void setExtractionCode(String extractionCode) {
        this.extractionCode = extractionCode;
    }

    public String getSyncStatus() {
        return ReviewStatus.normalize(syncStatus);
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
