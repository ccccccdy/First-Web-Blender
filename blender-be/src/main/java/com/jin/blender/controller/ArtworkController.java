package com.jin.blender.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jin.blender.dto.ArtworkUpsertPayload;
import com.jin.blender.dto.ResourceResponse;
import com.jin.blender.dto.RetrySyncRequest;
import com.jin.blender.entity.ResourceType;
import com.jin.blender.service.ResourceService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/artworks")
@CrossOrigin(origins = "http://localhost:5173")
public class ArtworkController {

    private final ResourceService resourceService;
    private final ObjectMapper objectMapper;

    public ArtworkController(ResourceService resourceService, ObjectMapper objectMapper) {
        this.resourceService = resourceService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<ResourceResponse> getAll() {
        return resourceService.listByType(ResourceType.ARTWORK);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResourceResponse> createArtwork(
            @RequestParam("metadata") String metadata,
            @RequestParam("coverImage") MultipartFile coverImage,
            @RequestParam(value = "previewImages", required = false) List<MultipartFile> previewImages) throws IOException {

        ArtworkUpsertPayload payload = objectMapper.readValue(metadata, ArtworkUpsertPayload.class);
        return ResponseEntity.ok(resourceService.createArtwork(payload, coverImage, previewImages));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResourceResponse> updateArtwork(
            @PathVariable Long id,
            @RequestParam("metadata") String metadata,
            @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
            @RequestParam(value = "previewImages", required = false) List<MultipartFile> previewImages) throws IOException {

        ArtworkUpsertPayload payload = objectMapper.readValue(metadata, ArtworkUpsertPayload.class);
        return ResponseEntity.ok(resourceService.updateArtwork(id, payload, coverImage, previewImages));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArtwork(@PathVariable Long id) {
        resourceService.deleteResource(ResourceType.ARTWORK, id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/retry-sync")
    public ResponseEntity<ResourceResponse> retrySync(@PathVariable Long id,
                                                      @RequestBody(required = false) RetrySyncRequest request) {
        String syncMessage = request == null ? null : request.getSyncMessage();
        return ResponseEntity.ok(resourceService.retrySync(ResourceType.ARTWORK, id, syncMessage));
    }
}
