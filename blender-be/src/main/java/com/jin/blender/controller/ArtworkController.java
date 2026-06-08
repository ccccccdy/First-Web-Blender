package com.jin.blender.controller;

import com.jin.blender.entity.Artwork;
import com.jin.blender.repository.ArtworkRepository;
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
import java.util.Optional;

@RestController
@RequestMapping("/api/artworks")
@CrossOrigin(origins = "http://localhost:5173")
public class ArtworkController {

    private static final String ACTION_SAVE = "save";
    private static final String ACTION_SUBMIT_REVIEW = "submitReview";
    private static final String ACTION_RESUBMIT = "resubmit";
    private static final String DECISION_APPROVE = "approve";
    private static final String DECISION_REJECT = "reject";

    @Autowired
    private ArtworkRepository artworkRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping
    public List<Artwork> getAll() {
        return artworkRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> createArtwork(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("coverFiles") List<MultipartFile> coverFiles,
            @RequestParam("modelFiles") List<MultipartFile> modelFiles) {

        try {
            String safeTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_");
            FileStorageService.ArtworkStorageResult storageResult =
                    fileStorageService.storeArtworkFiles(coverFiles, modelFiles, safeTitle);

            double totalSize = 0;
            for (MultipartFile file : coverFiles) {
                if (file != null) {
                    totalSize += file.getSize();
                }
            }
            for (MultipartFile file : modelFiles) {
                if (file != null) {
                    totalSize += file.getSize();
                }
            }

            Artwork artwork = new Artwork();
            artwork.setTitle(title);
            artwork.setDescription(description == null ? "" : description);
            artwork.setCoverUrls(storageResult.getCoverUrls());
            artwork.setModelFileUrls(storageResult.getModelFileUrls());
            artwork.setFileSize(totalSize / (1024.0 * 1024.0));
            artwork.setSyncStatus(ReviewStatus.PENDING_LINK);
            artwork.setCreatedAt(new Date());
            artwork.setUpdatedAt(new Date());

            return ResponseEntity.ok(artworkRepository.save(artwork));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed: " + e.getMessage());
        }
    }

    @PutMapping
    public ResponseEntity<?> updateArtwork(
            @RequestParam("id") Long id,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "quarkUrl", required = false) String quarkUrl,
            @RequestParam(value = "extractionCode", required = false) String extractionCode,
            @RequestParam(value = "action", defaultValue = ACTION_SAVE) String action,
            @RequestParam(value = "removedCoverIndexes", required = false) String removedCoverIndexes,
            @RequestParam(value = "removedModelIndexes", required = false) String removedModelIndexes,
            @RequestParam(value = "newCoverFiles", required = false) List<MultipartFile> newCoverFiles,
            @RequestParam(value = "newModelFiles", required = false) List<MultipartFile> newModelFiles) {

        Artwork artwork = artworkRepository.findById(id).orElse(null);
        if (artwork == null) {
            return ResponseEntity.notFound().build();
        }
        if (!ReviewStatus.canEdit(artwork.getSyncStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Only Pending Link or Sync Failed artworks can be edited");
        }
        if (!ReviewSupport.isBlank(removedCoverIndexes)
                || !ReviewSupport.isBlank(removedModelIndexes)
                || (newCoverFiles != null && !newCoverFiles.isEmpty())
                || (newModelFiles != null && !newModelFiles.isEmpty())) {
            return ResponseEntity.badRequest().body("File editing is unavailable in the current workflow");
        }

        String draftValidation = ReviewSupport.validateDraftQuarkInfo(quarkUrl);
        if (draftValidation != null) {
            return ResponseEntity.badRequest().body(draftValidation);
        }

        artwork.setTitle(title);
        artwork.setDescription(description == null ? "" : description);
        artwork.setQuarkUrl(ReviewSupport.trimToNull(quarkUrl));
        artwork.setExtractionCode(ReviewSupport.trimToNull(extractionCode));

        String normalizedAction = action == null ? ACTION_SAVE : action.trim();
        if (ACTION_SUBMIT_REVIEW.equals(normalizedAction)) {
            String validation = ReviewSupport.validateReviewSubmission(artwork.getQuarkUrl(), artwork.getExtractionCode());
            if (validation != null) {
                return ResponseEntity.badRequest().body(validation);
            }
            artwork.setSyncStatus(ReviewStatus.SYNCING);
            artwork.setFailureReason(null);
        } else if (ACTION_RESUBMIT.equals(normalizedAction)) {
            if (!ReviewStatus.isSyncFailed(artwork.getSyncStatus())) {
                return ResponseEntity.badRequest().body("Only Sync Failed artworks can be resubmitted");
            }
            String validation = ReviewSupport.validateReviewSubmission(artwork.getQuarkUrl(), artwork.getExtractionCode());
            if (validation != null) {
                return ResponseEntity.badRequest().body(validation);
            }
            artwork.setSyncStatus(ReviewStatus.SYNCING);
            artwork.setFailureReason(null);
        } else {
            if (ReviewStatus.isPendingLink(artwork.getSyncStatus())) {
                artwork.setSyncStatus(ReviewStatus.PENDING_LINK);
            } else {
                artwork.setSyncStatus(ReviewStatus.SYNC_FAILED);
            }
        }

        artwork.setUpdatedAt(new Date());
        return ResponseEntity.ok(artworkRepository.save(artwork));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<?> reviewArtwork(
            @PathVariable Long id,
            @RequestParam("decision") String decision,
            @RequestParam(value = "failureReason", required = false) String failureReason) {

        Artwork artwork = artworkRepository.findById(id).orElse(null);
        if (artwork == null) {
            return ResponseEntity.notFound().build();
        }
        if (!ReviewStatus.isSyncing(artwork.getSyncStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Only Syncing artworks can be reviewed");
        }

        if (DECISION_APPROVE.equals(decision)) {
            try {
                fileStorageService.deleteFiles(artwork.getModelFileUrls());
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to clean local archive");
            }
            artwork.setModelFileUrls(null);
            artwork.setSyncStatus(ReviewStatus.SYNCED);
            artwork.setFailureReason(null);
        } else if (DECISION_REJECT.equals(decision)) {
            String normalizedReason = ReviewSupport.trimToNull(failureReason);
            if (normalizedReason == null) {
                return ResponseEntity.badRequest().body("Failure reason is required");
            }
            artwork.setFailureReason(normalizedReason);
            artwork.setSyncStatus(ReviewStatus.SYNC_FAILED);
        } else {
            return ResponseEntity.badRequest().body("Unsupported review decision");
        }

        artwork.setUpdatedAt(new Date());
        return ResponseEntity.ok(artworkRepository.save(artwork));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteArtwork(@PathVariable Long id) {
        Optional<Artwork> optional = artworkRepository.findById(id);
        if (!optional.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Artwork artwork = optional.get();
        try {
            fileStorageService.deleteFiles(artwork.getCoverUrls());
            fileStorageService.deleteFiles(artwork.getModelFileUrls());
        } catch (IOException e) {
            e.printStackTrace();
        }

        artworkRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
