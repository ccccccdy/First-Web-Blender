package com.jin.blender.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jin.blender.dto.ArtworkUpsertPayload;
import com.jin.blender.dto.ResourceRequest;
import com.jin.blender.dto.ResourceResponse;
import com.jin.blender.entity.Resource;
import com.jin.blender.entity.ResourceStatus;
import com.jin.blender.entity.ResourceType;
import com.jin.blender.repository.ResourceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ResourceService {

    private static final String ARTWORK_COVER_DIR = "artworks/covers";
    private static final String ARTWORK_PREVIEW_DIR = "artworks/previews";

    private final ResourceRepository resourceRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    public ResourceService(ResourceRepository resourceRepository,
                           FileStorageService fileStorageService,
                           ObjectMapper objectMapper) {
        this.resourceRepository = resourceRepository;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    public List<ResourceResponse> listByType(ResourceType type) {
        return toResponses(resourceRepository.findAllByTypeOrderByCreatedAtDesc(type));
    }

    public List<ResourceResponse> listPlugins(String version) {
        List<Resource> resources;
        if (version == null || version.trim().isEmpty()) {
            resources = resourceRepository.findAllByTypeOrderByCreatedAtDesc(ResourceType.PLUGIN);
        } else {
            resources = resourceRepository.findAllByTypeAndBlenderVersionOrderByCreatedAtDesc(ResourceType.PLUGIN, version);
        }
        return toResponses(resources);
    }

    public ResourceResponse createArtwork(ArtworkUpsertPayload payload,
                                          MultipartFile coverImage,
                                          List<MultipartFile> previewImages) throws IOException {
        validateArtworkPayload(payload, true);
        validateImageFile(coverImage, "建模作品必须上传封面图片。");
        validateImageFiles(previewImages, "建模作品预览文件只能是图片。");

        Resource resource = new Resource();
        resource.setType(ResourceType.ARTWORK);
        applyArtworkPayload(resource, payload);
        resource.setCoverImageUrl(fileStorageService.storeFile(coverImage, ARTWORK_COVER_DIR));
        resource.setPreviewImageUrls(writeList(fileStorageService.storeFiles(previewImages, ARTWORK_PREVIEW_DIR)));

        return toResponse(resourceRepository.save(resource));
    }

    public ResourceResponse updateArtwork(Long id,
                                          ArtworkUpsertPayload payload,
                                          MultipartFile coverImage,
                                          List<MultipartFile> previewImages) throws IOException {
        validateArtworkPayload(payload, false);
        validateImageFiles(previewImages, "建模作品预览文件只能是图片。");
        if (coverImage != null && !coverImage.isEmpty()) {
            validateImageFile(coverImage, "建模作品封面必须是图片。");
        }

        Resource resource = findByIdAndType(id, ResourceType.ARTWORK);
        List<String> retainedPreviewImageUrls = sanitizeList(payload.getRetainedPreviewImageUrls());
        List<String> existingPreviewImageUrls = readList(resource.getPreviewImageUrls());
        deleteRemovedPreviewImages(existingPreviewImageUrls, retainedPreviewImageUrls);

        if (coverImage != null && !coverImage.isEmpty()) {
            deleteStoredFile(resource.getCoverImageUrl());
            resource.setCoverImageUrl(fileStorageService.storeFile(coverImage, ARTWORK_COVER_DIR));
        } else if (resource.getCoverImageUrl() == null || resource.getCoverImageUrl().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "建模作品必须保留一张封面图片。");
        }

        List<String> mergedPreviewImages = new ArrayList<String>(retainedPreviewImageUrls);
        mergedPreviewImages.addAll(fileStorageService.storeFiles(previewImages, ARTWORK_PREVIEW_DIR));

        applyArtworkPayload(resource, payload);
        resource.setPreviewImageUrls(writeList(mergedPreviewImages));

        return toResponse(resourceRepository.save(resource));
    }

    public ResourceResponse createBasicResource(ResourceType type, ResourceRequest request) {
        validateBasicRequest(type, request);

        Resource resource = new Resource();
        resource.setType(type);
        applyBasicRequest(resource, type, request);
        return toResponse(resourceRepository.save(resource));
    }

    public ResourceResponse updateBasicResource(ResourceType type, Long id, ResourceRequest request) {
        validateBasicRequest(type, request);

        Resource resource = findByIdAndType(id, type);
        applyBasicRequest(resource, type, request);
        return toResponse(resourceRepository.save(resource));
    }

    public void deleteResource(ResourceType type, Long id) {
        Resource resource = findByIdAndType(id, type);
        if (type == ResourceType.ARTWORK) {
            deleteStoredFile(resource.getCoverImageUrl());
            deleteStoredFiles(readList(resource.getPreviewImageUrls()));
        }
        resourceRepository.delete(resource);
    }

    public ResourceResponse retrySync(ResourceType type, Long id, String syncMessage) {
        Resource resource = findByIdAndType(id, type);
        resource.setStatus(ResourceStatus.SYNCING);
        resource.setSyncMessage(syncMessage == null || syncMessage.trim().isEmpty() ? "同步中" : syncMessage.trim());
        return toResponse(resourceRepository.save(resource));
    }

    private void applyArtworkPayload(Resource resource, ArtworkUpsertPayload payload) {
        resource.setTitle(requireText(payload.getTitle(), "标题不能为空。"));
        resource.setDescription(defaultText(payload.getDescription()));
        resource.setStatus(defaultStatus(payload.getStatus()));
        resource.setPreviewVideoUrls(writeList(payload.getPreviewVideoUrls()));
        resource.setQuarkShareUrl(defaultText(payload.getQuarkShareUrl()));
        resource.setQuarkCode(defaultText(payload.getQuarkCode()));
        resource.setQuarkNote(defaultText(payload.getQuarkNote()));
        resource.setSyncMessage(defaultSyncMessage(resource.getStatus(), payload.getSyncMessage()));
        resource.setFileSize(payload.getFileSize());
        resource.setBlenderVersion(null);
    }

    private void applyBasicRequest(Resource resource, ResourceType type, ResourceRequest request) {
        resource.setTitle(requireText(request.getTitle(), "标题不能为空。"));
        resource.setDescription(defaultText(request.getDescription()));
        resource.setStatus(defaultStatus(request.getStatus()));
        resource.setQuarkShareUrl(defaultText(request.getQuarkShareUrl()));
        resource.setQuarkCode(defaultText(request.getQuarkCode()));
        resource.setQuarkNote(defaultText(request.getQuarkNote()));
        resource.setSyncMessage(defaultSyncMessage(resource.getStatus(), request.getSyncMessage()));
        resource.setFileSize(request.getFileSize());
        resource.setCoverImageUrl(null);
        resource.setPreviewImageUrls(writeList(Collections.<String>emptyList()));
        resource.setPreviewVideoUrls(writeList(Collections.<String>emptyList()));
        resource.setBlenderVersion(type == ResourceType.PLUGIN
                ? requireText(request.getBlenderVersion(), "插件必须填写 Blender 版本。")
                : null);
    }

    private void validateArtworkPayload(ArtworkUpsertPayload payload, boolean creating) {
        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少建模作品元数据。");
        }
        requireText(payload.getTitle(), "标题不能为空。");
        if (creating && payload.getRetainedPreviewImageUrls() != null && !payload.getRetainedPreviewImageUrls().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新建建模作品时不能保留旧的预览图片。");
        }
    }

    private void validateBasicRequest(ResourceType type, ResourceRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少资源请求数据。");
        }
        requireText(request.getTitle(), "标题不能为空。");
        if (type == ResourceType.PLUGIN) {
            requireText(request.getBlenderVersion(), "插件必须填写 Blender 版本。");
        }
    }

    private void validateImageFiles(List<MultipartFile> files, String message) {
        if (files == null) {
            return;
        }
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                validateImageFile(file, message);
            }
        }
    }

    private void validateImageFile(MultipartFile file, String message) {
        if (file == null || file.isEmpty() || !isImageFile(file)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("image/")) {
            return true;
        }

        String name = file.getOriginalFilename();
        if (name == null) {
            return false;
        }

        String lowerName = name.toLowerCase();
        return lowerName.endsWith(".jpg")
                || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".png")
                || lowerName.endsWith(".gif")
                || lowerName.endsWith(".webp")
                || lowerName.endsWith(".bmp")
                || lowerName.endsWith(".svg");
    }

    private Resource findByIdAndType(Long id, ResourceType type) {
        return resourceRepository.findByIdAndType(id, type)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "资源不存在。"));
    }

    private List<ResourceResponse> toResponses(List<Resource> resources) {
        List<ResourceResponse> responses = new ArrayList<ResourceResponse>();
        for (Resource resource : resources) {
            responses.add(toResponse(resource));
        }
        return responses;
    }

    private ResourceResponse toResponse(Resource resource) {
        ResourceResponse response = new ResourceResponse();
        response.setId(resource.getId());
        response.setTitle(resource.getTitle());
        response.setDescription(resource.getDescription());
        response.setType(resource.getType());
        response.setStatus(resource.getStatus());
        response.setCoverImageUrl(resource.getCoverImageUrl());
        response.setPreviewImageUrls(readList(resource.getPreviewImageUrls()));
        response.setPreviewVideoUrls(readList(resource.getPreviewVideoUrls()));
        response.setQuarkShareUrl(resource.getQuarkShareUrl());
        response.setQuarkCode(resource.getQuarkCode());
        response.setQuarkNote(resource.getQuarkNote());
        response.setSyncMessage(resource.getSyncMessage());
        response.setBlenderVersion(resource.getBlenderVersion());
        response.setFileSize(resource.getFileSize());
        response.setAvailable(resource.getStatus() == ResourceStatus.READY
                && resource.getQuarkShareUrl() != null
                && !resource.getQuarkShareUrl().trim().isEmpty());
        response.setCreatedAt(resource.getCreatedAt());
        response.setUpdatedAt(resource.getUpdatedAt());
        return response;
    }

    private List<String> readList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<String>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "读取资源列表失败。", e);
        }
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(sanitizeList(values));
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "保存资源列表失败。", e);
        }
    }

    private List<String> sanitizeList(List<String> values) {
        List<String> result = new ArrayList<String>();
        if (values == null) {
            return result;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                result.add(value.trim());
            }
        }
        return result;
    }

    private void deleteRemovedPreviewImages(List<String> existingUrls, List<String> retainedUrls) {
        for (String existingUrl : existingUrls) {
            if (!retainedUrls.contains(existingUrl)) {
                deleteStoredFile(existingUrl);
            }
        }
    }

    private void deleteStoredFiles(List<String> fileUrls) {
        for (String fileUrl : fileUrls) {
            deleteStoredFile(fileUrl);
        }
    }

    private void deleteStoredFile(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return;
        }
        try {
            fileStorageService.deleteFile(fileUrl);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "删除本地预览文件失败。", e);
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String defaultText(String value) {
        return value == null ? "" : value.trim();
    }

    private ResourceStatus defaultStatus(ResourceStatus status) {
        return status == null ? ResourceStatus.SYNCING : status;
    }

    private String defaultSyncMessage(ResourceStatus status, String syncMessage) {
        if (syncMessage != null && !syncMessage.trim().isEmpty()) {
            return syncMessage.trim();
        }
        if (status == ResourceStatus.SYNCING) {
            return "同步中";
        }
        if (status == ResourceStatus.FAILED) {
            return "同步失败";
        }
        return "";
    }
}
