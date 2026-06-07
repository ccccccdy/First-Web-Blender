package com.jin.blender.entity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "resource")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResourceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResourceStatus status;

    @Column(name = "cover_image_url", columnDefinition = "TEXT")
    private String coverImageUrl;

    @Column(name = "preview_image_urls", columnDefinition = "TEXT")
    private String previewImageUrls;

    @Column(name = "preview_video_urls", columnDefinition = "TEXT")
    private String previewVideoUrls;

    @Column(name = "quark_share_url", columnDefinition = "TEXT")
    private String quarkShareUrl;

    @Column(name = "quark_code", length = 100)
    private String quarkCode;

    @Column(name = "quark_note", columnDefinition = "TEXT")
    private String quarkNote;

    @Column(name = "sync_message", columnDefinition = "TEXT")
    private String syncMessage;

    @Column(name = "blender_version", length = 50)
    private String blenderVersion;

    @Column(name = "file_size")
    private Double fileSize;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    private Date updatedAt;

    @PrePersist
    public void prePersist() {
        Date now = new Date();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = new Date();
    }

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

    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public String getPreviewImageUrls() {
        return previewImageUrls;
    }

    public void setPreviewImageUrls(String previewImageUrls) {
        this.previewImageUrls = previewImageUrls;
    }

    public String getPreviewVideoUrls() {
        return previewVideoUrls;
    }

    public void setPreviewVideoUrls(String previewVideoUrls) {
        this.previewVideoUrls = previewVideoUrls;
    }

    public String getQuarkShareUrl() {
        return quarkShareUrl;
    }

    public void setQuarkShareUrl(String quarkShareUrl) {
        this.quarkShareUrl = quarkShareUrl;
    }

    public String getQuarkCode() {
        return quarkCode;
    }

    public void setQuarkCode(String quarkCode) {
        this.quarkCode = quarkCode;
    }

    public String getQuarkNote() {
        return quarkNote;
    }

    public void setQuarkNote(String quarkNote) {
        this.quarkNote = quarkNote;
    }

    public String getSyncMessage() {
        return syncMessage;
    }

    public void setSyncMessage(String syncMessage) {
        this.syncMessage = syncMessage;
    }

    public String getBlenderVersion() {
        return blenderVersion;
    }

    public void setBlenderVersion(String blenderVersion) {
        this.blenderVersion = blenderVersion;
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
}
