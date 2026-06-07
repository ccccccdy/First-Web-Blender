package com.jin.blender.dto;

import com.jin.blender.entity.ResourceStatus;

import java.util.List;

public class ArtworkUpsertPayload {
    private String title;
    private String description;
    private ResourceStatus status;
    private List<String> retainedPreviewImageUrls;
    private List<String> previewVideoUrls;
    private String quarkShareUrl;
    private String quarkCode;
    private String quarkNote;
    private String syncMessage;
    private Double fileSize;

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

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public List<String> getRetainedPreviewImageUrls() {
        return retainedPreviewImageUrls;
    }

    public void setRetainedPreviewImageUrls(List<String> retainedPreviewImageUrls) {
        this.retainedPreviewImageUrls = retainedPreviewImageUrls;
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

    public Double getFileSize() {
        return fileSize;
    }

    public void setFileSize(Double fileSize) {
        this.fileSize = fileSize;
    }
}
