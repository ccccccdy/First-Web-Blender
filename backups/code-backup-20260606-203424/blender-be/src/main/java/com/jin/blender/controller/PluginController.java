package com.jin.blender.controller;

import com.jin.blender.entity.Plugin;
import com.jin.blender.repository.PluginRepository;
import com.jin.blender.service.FileStorageService;
import com.jin.blender.support.ReviewStatus;
import com.jin.blender.support.ReviewSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/plugins")
@CrossOrigin(origins = "http://localhost:5173")
public class PluginController {

    private static final String ACTION_SAVE = "save";
    private static final String ACTION_SUBMIT_REVIEW = "submitReview";
    private static final String ACTION_RESUBMIT = "resubmit";
    private static final String DECISION_APPROVE = "approve";
    private static final String DECISION_REJECT = "reject";

    @Autowired
    private PluginRepository pluginRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping
    public List<Plugin> getAll(@RequestParam(value = "version", required = false) String version) {
        if (version != null && !version.isEmpty()) {
            return pluginRepository.findByBlenderVersion(version);
        }
        return pluginRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> createPlugin(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "blenderVersion", required = false) String blenderVersion,
            @RequestParam("pluginFiles") List<MultipartFile> pluginFiles) {

        try {
            String safeTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_");
            String subDir = "plugins/" + safeTitle;
            String fileUrl = fileStorageService.storeArchiveFiles(pluginFiles, subDir, safeTitle);
            double fileSize = fileStorageService.getFileSizeMB(fileUrl);

            Plugin plugin = new Plugin();
            plugin.setTitle(title);
            plugin.setDescription(description == null ? "" : description);
            plugin.setBlenderVersion(blenderVersion == null ? "" : blenderVersion);
            plugin.setFileUrls(fileUrl);
            plugin.setFileSize(fileSize);
            plugin.setSyncStatus(ReviewStatus.PENDING_LINK);
            plugin.setCreatedAt(new Date());
            plugin.setUpdatedAt(new Date());

            return ResponseEntity.ok(pluginRepository.save(plugin));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed: " + e.getMessage());
        }
    }

    @PutMapping
    public ResponseEntity<?> updatePlugin(
            @RequestParam("id") Long id,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("blenderVersion") String blenderVersion,
            @RequestParam(value = "quarkUrl", required = false) String quarkUrl,
            @RequestParam(value = "extractionCode", required = false) String extractionCode,
            @RequestParam(value = "action", defaultValue = ACTION_SAVE) String action,
            @RequestParam(value = "removedFileIndexes", required = false) String removedFileIndexes,
            @RequestParam(value = "newFiles", required = false) List<MultipartFile> newFiles) {

        Plugin plugin = pluginRepository.findById(id).orElse(null);
        if (plugin == null) {
            return ResponseEntity.notFound().build();
        }
        if (!ReviewStatus.canEdit(plugin.getSyncStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Only Pending Link or Sync Failed plugins can be edited");
        }
        if (!ReviewSupport.isBlank(removedFileIndexes) || (newFiles != null && !newFiles.isEmpty())) {
            return ResponseEntity.badRequest().body("File editing is unavailable in the current workflow");
        }

        String draftValidation = ReviewSupport.validateDraftQuarkInfo(quarkUrl);
        if (draftValidation != null) {
            return ResponseEntity.badRequest().body(draftValidation);
        }

        plugin.setTitle(title);
        plugin.setDescription(description == null ? "" : description);
        plugin.setBlenderVersion(blenderVersion);
        plugin.setQuarkUrl(ReviewSupport.trimToNull(quarkUrl));
        plugin.setExtractionCode(ReviewSupport.trimToNull(extractionCode));

        String normalizedAction = action == null ? ACTION_SAVE : action.trim();
        if (ACTION_SUBMIT_REVIEW.equals(normalizedAction)) {
            String validation = ReviewSupport.validateReviewSubmission(plugin.getQuarkUrl(), plugin.getExtractionCode());
            if (validation != null) {
                return ResponseEntity.badRequest().body(validation);
            }
            plugin.setSyncStatus(ReviewStatus.SYNCING);
            plugin.setFailureReason(null);
        } else if (ACTION_RESUBMIT.equals(normalizedAction)) {
            if (!ReviewStatus.isSyncFailed(plugin.getSyncStatus())) {
                return ResponseEntity.badRequest().body("Only Sync Failed plugins can be resubmitted");
            }
            String validation = ReviewSupport.validateReviewSubmission(plugin.getQuarkUrl(), plugin.getExtractionCode());
            if (validation != null) {
                return ResponseEntity.badRequest().body(validation);
            }
            plugin.setSyncStatus(ReviewStatus.SYNCING);
            plugin.setFailureReason(null);
        } else {
            if (ReviewStatus.isPendingLink(plugin.getSyncStatus())) {
                plugin.setSyncStatus(ReviewStatus.PENDING_LINK);
            } else {
                plugin.setSyncStatus(ReviewStatus.SYNC_FAILED);
            }
        }

        plugin.setUpdatedAt(new Date());
        return ResponseEntity.ok(pluginRepository.save(plugin));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<?> reviewPlugin(
            @PathVariable Long id,
            @RequestParam("decision") String decision,
            @RequestParam(value = "failureReason", required = false) String failureReason) {

        Plugin plugin = pluginRepository.findById(id).orElse(null);
        if (plugin == null) {
            return ResponseEntity.notFound().build();
        }
        if (!ReviewStatus.isSyncing(plugin.getSyncStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Only Syncing plugins can be reviewed");
        }

        if (DECISION_APPROVE.equals(decision)) {
            try {
                fileStorageService.deleteFiles(plugin.getFileUrls());
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to clean local archive");
            }
            plugin.setFileUrls(null);
            plugin.setSyncStatus(ReviewStatus.SYNCED);
            plugin.setFailureReason(null);
        } else if (DECISION_REJECT.equals(decision)) {
            String normalizedReason = ReviewSupport.trimToNull(failureReason);
            if (normalizedReason == null) {
                return ResponseEntity.badRequest().body("Failure reason is required");
            }
            plugin.setFailureReason(normalizedReason);
            plugin.setSyncStatus(ReviewStatus.SYNC_FAILED);
        } else {
            return ResponseEntity.badRequest().body("Unsupported review decision");
        }

        plugin.setUpdatedAt(new Date());
        return ResponseEntity.ok(pluginRepository.save(plugin));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlugin(@PathVariable Long id) {
        Plugin plugin = pluginRepository.findById(id).orElse(null);
        if (plugin == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            fileStorageService.deleteFiles(plugin.getFileUrls());
        } catch (IOException e) {
            e.printStackTrace();
        }

        pluginRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
