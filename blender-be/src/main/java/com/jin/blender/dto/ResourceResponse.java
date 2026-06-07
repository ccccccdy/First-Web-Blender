package com.jin.blender.dto;

import com.jin.blender.entity.ResourceStatus;
import com.jin.blender.entity.ResourceType;

import java.util.Date;
import java.util.List;

public class ResourceResponse {
    private Long id;
    private String title;
    private String description;
    private ResourceType type;
    private ResourceStatus status;
    private String coverImageUrl;
    private List<String> previewImageUrls;
    private List<String> previewVideoUrls;
    private String quarkShareUrl;
    private String quarkCode;
    private String quarkNote;
    private String syncMessage;
    private String blenderVersion;
    private Double fileSize;
    private boolean available;
    private Date createdAt;
    private Date updatedAt;

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

    public List<String> getPreviewImageUrls() {
        return previewImageUrls;
    }

    public void setPreviewImageUrls(List<String> previewImageUrls) {
        this.previewImageUrls = previewImageUrls;
    }

    public List<String> getPreviewVideoUrls() {
        return previewVideoUrls;
    }

    public void setPreviewVideoUrls(List<String> previewVideoUrls) {
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

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
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
