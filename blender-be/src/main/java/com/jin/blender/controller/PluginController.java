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
@RequestMapping("/api/plugins")
@CrossOrigin(origins = "http://localhost:5173")
public class PluginController {

    private final ResourceService resourceService;

    public PluginController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public List<ResourceResponse> getAll(@RequestParam(value = "version", required = false) String version) {
        return resourceService.listPlugins(version);
    }

    @PostMapping
    public ResponseEntity<ResourceResponse> createPlugin(@RequestBody ResourceRequest request) {
        return ResponseEntity.ok(resourceService.createBasicResource(ResourceType.PLUGIN, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResourceResponse> updatePlugin(@PathVariable Long id,
                                                         @RequestBody ResourceRequest request) {
        return ResponseEntity.ok(resourceService.updateBasicResource(ResourceType.PLUGIN, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlugin(@PathVariable Long id) {
        resourceService.deleteResource(ResourceType.PLUGIN, id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/retry-sync")
    public ResponseEntity<ResourceResponse> retrySync(@PathVariable Long id,
                                                      @RequestBody(required = false) RetrySyncRequest request) {
        String syncMessage = request == null ? null : request.getSyncMessage();
        return ResponseEntity.ok(resourceService.retrySync(ResourceType.PLUGIN, id, syncMessage));
    }
}
