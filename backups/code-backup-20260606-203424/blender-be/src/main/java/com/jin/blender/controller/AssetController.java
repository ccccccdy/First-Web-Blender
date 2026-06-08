package com.jin.blender.controller;

import com.jin.blender.entity.Asset;
import com.jin.blender.repository.AssetRepository;
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
@RequestMapping("/api/assets")
@CrossOrigin(origins = "http://localhost:5173")
public class AssetController {

    private static final String ACTION_SAVE = "save";
    private static final String ACTION_SUBMIT_REVIEW = "submitReview";
    private static final String ACTION_RESUBMIT = "resubmit";
    private static final String DECISION_APPROVE = "approve";
    private static final String DECISION_REJECT = "reject";

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping
    public List<Asset> getAll() {
        return assetRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> createAsset(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("assetFiles") List<MultipartFile> assetFiles) {

        try {
            String safeTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_");
            String subDir = "assets/" + safeTitle;
            String fileUrl = fileStorageService.storeArchiveFiles(assetFiles, subDir, safeTitle);
            double fileSize = fileStorageService.getFileSizeMB(fileUrl);

            Asset asset = new Asset();
            asset.setTitle(title);
            asset.setDescription(description == null ? "" : description);
            asset.setFileUrls(fileUrl);
            asset.setFileSize(fileSize);
            asset.setSyncStatus(ReviewStatus.PENDING_LINK);
            asset.setCreatedAt(new Date());
            asset.setUpdatedAt(new Date());

            return ResponseEntity.ok(assetRepository.save(asset));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed: " + e.getMessage());
        }
    }

    @PutMapping
    public ResponseEntity<?> updateAsset(
            @RequestParam("id") Long id,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "quarkUrl", required = false) String quarkUrl,
            @RequestParam(value = "extractionCode", required = false) String extractionCode,
            @RequestParam(value = "action", defaultValue = ACTION_SAVE) String action,
            @RequestParam(value = "removedFileIndexes", required = false) String removedFileIndexes,
            @RequestParam(value = "newFiles", required = false) List<MultipartFile> newFiles) {

        Asset asset = assetRepository.findById(id).orElse(null);
        if (asset == null) {
            return ResponseEntity.notFound().build();
        }
        if (!ReviewStatus.canEdit(asset.getSyncStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Only Pending Link or Sync Failed assets can be edited");
        }
        if (!ReviewSupport.isBlank(removedFileIndexes) || (newFiles != null && !newFiles.isEmpty())) {
            return ResponseEntity.badRequest().body("File editing is unavailable in the current workflow");
        }

        String draftValidation = ReviewSupport.validateDraftQuarkInfo(quarkUrl);
        if (draftValidation != null) {
            return ResponseEntity.badRequest().body(draftValidation);
        }

        asset.setTitle(title);
        asset.setDescription(description == null ? "" : description);
        asset.setQuarkUrl(ReviewSupport.trimToNull(quarkUrl));
        asset.setExtractionCode(ReviewSupport.trimToNull(extractionCode));

        String normalizedAction = action == null ? ACTION_SAVE : action.trim();
        if (ACTION_SUBMIT_REVIEW.equals(normalizedAction)) {
            String validation = ReviewSupport.validateReviewSubmission(asset.getQuarkUrl(), asset.getExtractionCode());
            if (validation != null) {
                return ResponseEntity.badRequest().body(validation);
            }
            asset.setSyncStatus(ReviewStatus.SYNCING);
            asset.setFailureReason(null);
        } else if (ACTION_RESUBMIT.equals(normalizedAction)) {
            if (!ReviewStatus.isSyncFailed(asset.getSyncStatus())) {
                return ResponseEntity.badRequest().body("Only Sync Failed assets can be resubmitted");
            }
            String validation = ReviewSupport.validateReviewSubmission(asset.getQuarkUrl(), asset.getExtractionCode());
            if (validation != null) {
                return ResponseEntity.badRequest().body(validation);
            }
            asset.setSyncStatus(ReviewStatus.SYNCING);
            asset.setFailureReason(null);
        } else {
            if (ReviewStatus.isPendingLink(asset.getSyncStatus())) {
                asset.setSyncStatus(ReviewStatus.PENDING_LINK);
            } else {
                asset.setSyncStatus(ReviewStatus.SYNC_FAILED);
            }
        }

        asset.setUpdatedAt(new Date());
        return ResponseEntity.ok(assetRepository.save(asset));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<?> reviewAsset(
            @PathVariable Long id,
            @RequestParam("decision") String decision,
            @RequestParam(value = "failureReason", required = false) String failureReason) {

        Asset asset = assetRepository.findById(id).orElse(null);
        if (asset == null) {
            return ResponseEntity.notFound().build();
        }
        if (!ReviewStatus.isSyncing(asset.getSyncStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Only Syncing assets can be reviewed");
        }

        if (DECISION_APPROVE.equals(decision)) {
            try {
                fileStorageService.deleteFiles(asset.getFileUrls());
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to clean local archive");
            }
            asset.setFileUrls(null);
            asset.setSyncStatus(ReviewStatus.SYNCED);
            asset.setFailureReason(null);
        } else if (DECISION_REJECT.equals(decision)) {
            String normalizedReason = ReviewSupport.trimToNull(failureReason);
            if (normalizedReason == null) {
                return ResponseEntity.badRequest().body("Failure reason is required");
            }
            asset.setFailureReason(normalizedReason);
            asset.setSyncStatus(ReviewStatus.SYNC_FAILED);
        } else {
            return ResponseEntity.badRequest().body("Unsupported review decision");
        }

        asset.setUpdatedAt(new Date());
        return ResponseEntity.ok(assetRepository.save(asset));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAsset(@PathVariable Long id) {
        Asset asset = assetRepository.findById(id).orElse(null);
        if (asset == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            fileStorageService.deleteFiles(asset.getFileUrls());
        } catch (IOException e) {
            e.printStackTrace();
        }

        assetRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
