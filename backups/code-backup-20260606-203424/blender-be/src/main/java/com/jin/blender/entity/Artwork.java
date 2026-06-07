package com.jin.blender.entity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "artwork")
public class Artwork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_urls", columnDefinition = "TEXT")
    private String coverUrls;      // 多个封面文件路径，逗号分隔

    @Column(name = "model_file_urls", columnDefinition = "TEXT")
    private String modelFileUrls;  // 多个模型文件路径，逗号分隔

    @Column(name = "file_size")
    private Double fileSize;       // 可选，可以存储总大小或忽略

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;

    // 无参构造
    public Artwork() {}

    // getter / setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCoverUrls() { return coverUrls; }
    public void setCoverUrls(String coverUrls) { this.coverUrls = coverUrls; }

    public String getModelFileUrls() { return modelFileUrls; }
    public void setModelFileUrls(String modelFileUrls) { this.modelFileUrls = modelFileUrls; }

    public Double getFileSize() { return fileSize; }
    public void setFileSize(Double fileSize) { this.fileSize = fileSize; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}