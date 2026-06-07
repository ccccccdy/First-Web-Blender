package com.jin.blender.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jin.blender.entity.Asset;
import com.jin.blender.repository.AssetRepository;
import com.jin.blender.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/assets")
@CrossOrigin(origins = "http://localhost:5173")
public class AssetController {

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private FileStorageService fileStorageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
            String fileUrls = fileStorageService.storeFiles(assetFiles, subDir);

            double totalSize = 0;
            for (MultipartFile file : assetFiles) {
                totalSize += file.getSize();
            }

            Asset asset = new Asset();
            asset.setTitle(title);
            asset.setDescription(description == null ? "" : description);
            asset.setFileUrls(fileUrls);
            asset.setFileSize(totalSize / (1024.0 * 1024.0));
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
            @RequestParam("description") String description,
            @RequestParam(value = "removedFileIndexes", required = false) String removedFileIndexes,
            @RequestParam(value = "newFiles", required = false) List<MultipartFile> newFiles) {

        Asset asset = assetRepository.findById(id).orElse(null);
        if (asset == null) {
            return ResponseEntity.notFound().build();
        }

        asset.setTitle(title);
        asset.setDescription(description);

        List<String> fileList = asset.getFileUrls() == null || asset.getFileUrls().isEmpty()
                ? new ArrayList<>()
                : new ArrayList<>(Arrays.asList(asset.getFileUrls().split(",")));

        if (removedFileIndexes != null && !removedFileIndexes.isEmpty()) {
            try {
                List<Integer> removeIdx = objectMapper.readValue(removedFileIndexes, new TypeReference<List<Integer>>() {});
                removeIdx.sort(Comparator.reverseOrder());
                for (int idx : removeIdx) {
                    if (idx >= 0 && idx < fileList.size()) {
                        String fileUrl = fileList.get(idx);
                        try {
                            fileStorageService.deleteFile(fileUrl);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        fileList.remove(idx);
                    }
                }
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Invalid file index format");
            }
        }

        String safeTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_");
        String subDir = "assets/" + safeTitle;
        try {
            if (newFiles != null && !newFiles.isEmpty()) {
                String newFilePaths = fileStorageService.storeFiles(newFiles, subDir);
                if (newFilePaths != null && !newFilePaths.isEmpty()) {
                    fileList.addAll(Arrays.asList(newFilePaths.split(",")));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File save failed");
        }

        asset.setFileUrls(String.join(",", fileList));
        asset.setFileSize(calculateStoredSize(fileList));
        asset.setUpdatedAt(new Date());
        assetRepository.save(asset);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAsset(@PathVariable Long id) {
        Asset asset = assetRepository.findById(id).orElse(null);
        if (asset == null) {
            return ResponseEntity.notFound().build();
        }

        if (asset.getFileUrls() != null && !asset.getFileUrls().isEmpty()) {
            for (String fileUrl : asset.getFileUrls().split(",")) {
                try {
                    fileStorageService.deleteFile(fileUrl);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        assetRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    private double calculateStoredSize(List<String> fileList) {
        if (fileList == null) {
            return 0;
        }
        double totalSize = 0;
        for (String fileUrl : fileList) {
            try {
                totalSize += fileStorageService.getFileSizeMB(fileUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return totalSize;
    }
}
