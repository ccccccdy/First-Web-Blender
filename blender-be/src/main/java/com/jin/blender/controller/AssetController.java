package com.jin.blender.controller;

import com.jin.blender.dto.ResourceRequest;
import com.jin.blender.dto.ResourceResponse;
import com.jin.blender.dto.RetrySyncRequest;
import com.jin.blender.entity.ResourceType;
import com.jin.blender.service.ResourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@CrossOrigin(origins = "http://localhost:5173")
public class AssetController {

    private final ResourceService resourceService;

    public AssetController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public List<ResourceResponse> getAll() {
        return resourceService.listByType(ResourceType.ASSET);
    }

    @PostMapping
    public ResponseEntity<ResourceResponse> createAsset(@RequestBody ResourceRequest request) {
        return ResponseEntity.ok(resourceService.createBasicResource(ResourceType.ASSET, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResourceResponse> updateAsset(@PathVariable Long id,
                                                        @RequestBody ResourceRequest request) {
        return ResponseEntity.ok(resourceService.updateBasicResource(ResourceType.ASSET, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long id) {
        resourceService.deleteResource(ResourceType.ASSET, id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/retry-sync")
    public ResponseEntity<ResourceResponse> retrySync(@PathVariable Long id,
                                                      @RequestBody(required = false) RetrySyncRequest request) {
        String syncMessage = request == null ? null : request.getSyncMessage();
        return ResponseEntity.ok(resourceService.retrySync(ResourceType.ASSET, id, syncMessage));
    }
}
