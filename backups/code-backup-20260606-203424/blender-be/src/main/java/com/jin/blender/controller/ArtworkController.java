package com.jin.blender.controller;

import com.jin.blender.entity.Artwork;
import com.jin.blender.repository.ArtworkRepository;
import com.jin.blender.service.FileStorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/artworks")
@CrossOrigin(origins = "http://localhost:5173")
public class ArtworkController {

    @Autowired
    private ArtworkRepository artworkRepository;

    @Autowired
    private FileStorageService fileStorageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
            String coverUrls = fileStorageService.storeFiles(coverFiles, safeTitle);
            String modelFileUrls = fileStorageService.storeFiles(modelFiles, safeTitle);

            double totalSize = 0;
            for (MultipartFile f : coverFiles) totalSize += f.getSize();
            for (MultipartFile f : modelFiles) totalSize += f.getSize();
            double fileSizeMB = totalSize / (1024.0 * 1024.0);

            Artwork artwork = new Artwork();
            artwork.setTitle(title);
            artwork.setDescription(description == null ? "" : description);
            artwork.setCoverUrls(coverUrls);
            artwork.setModelFileUrls(modelFileUrls);
            artwork.setFileSize(fileSizeMB);
            artwork.setCreatedAt(new Date());
            artwork.setUpdatedAt(new Date());

            Artwork saved = artworkRepository.save(artwork);
            return ResponseEntity.ok(saved);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("文件上传失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteArtwork(@PathVariable Long id) {
        Optional<Artwork> optional = artworkRepository.findById(id);
        if (optional.isPresent()) {
            Artwork artwork = optional.get();
            String safeTitle = artwork.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_");
            try {
                fileStorageService.deleteFolder(safeTitle);
            } catch (IOException e) {
                e.printStackTrace();
            }
            artworkRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping
    public ResponseEntity<?> updateArtwork(
            @RequestParam("id") Long id,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam(value = "removedCoverIndexes", required = false) String removedCoverIndexes,
            @RequestParam(value = "removedModelIndexes", required = false) String removedModelIndexes,
            @RequestParam(value = "newCoverFiles", required = false) List<MultipartFile> newCoverFiles,
            @RequestParam(value = "newModelFiles", required = false) List<MultipartFile> newModelFiles) {

        Artwork artwork = artworkRepository.findById(id).orElse(null);
        if (artwork == null) {
            return ResponseEntity.notFound().build();
        }

        // 更新标题和描述
        artwork.setTitle(title);
        artwork.setDescription(description);

        // 获取当前文件列表（防御 null）
        List<String> coverList = artwork.getCoverUrls() == null ? new ArrayList<>()
                : new ArrayList<>(Arrays.asList(artwork.getCoverUrls().split(",")));
        List<String> modelList = artwork.getModelFileUrls() == null ? new ArrayList<>()
                : new ArrayList<>(Arrays.asList(artwork.getModelFileUrls().split(",")));

        // 解析并删除封面文件（同时删除物理文件）
        if (removedCoverIndexes != null && !removedCoverIndexes.isEmpty()) {
            try {
                List<Integer> removeIdx = objectMapper.readValue(removedCoverIndexes, new TypeReference<List<Integer>>() {});
                removeIdx.sort(Comparator.reverseOrder());
                for (int idx : removeIdx) {
                    if (idx < coverList.size()) {
                        String fileUrl = coverList.get(idx);
                        // 删除磁盘上的物理文件
                        try {
                            fileStorageService.deleteFile(fileUrl);
                        } catch (IOException e) {
                            System.err.println("删除物理文件失败: " + fileUrl);
                            e.printStackTrace();
                        }
                        coverList.remove(idx);
                    }
                }
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("封面索引格式错误");
            }
        }

        // 解析并删除模型文件（同时删除物理文件）
        if (removedModelIndexes != null && !removedModelIndexes.isEmpty()) {
            try {
                List<Integer> removeIdx = objectMapper.readValue(removedModelIndexes, new TypeReference<List<Integer>>() {});
                removeIdx.sort(Comparator.reverseOrder());
                for (int idx : removeIdx) {
                    if (idx < modelList.size()) {
                        String fileUrl = modelList.get(idx);
                        try {
                            fileStorageService.deleteFile(fileUrl);
                        } catch (IOException e) {
                            System.err.println("删除物理文件失败: " + fileUrl);
                            e.printStackTrace();
                        }
                        modelList.remove(idx);
                    }
                }
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("模型索引格式错误");
            }
        }

        // 保存新上传的文件
        String safeTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_");
        try {
            if (newCoverFiles != null && !newCoverFiles.isEmpty()) {
                String newPaths = fileStorageService.storeFiles(newCoverFiles, safeTitle);
                if (newPaths != null && !newPaths.isEmpty()) {
                    coverList.addAll(Arrays.asList(newPaths.split(",")));
                }
            }
            if (newModelFiles != null && !newModelFiles.isEmpty()) {
                String newPaths = fileStorageService.storeFiles(newModelFiles, safeTitle);
                if (newPaths != null && !newPaths.isEmpty()) {
                    modelList.addAll(Arrays.asList(newPaths.split(",")));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("文件保存失败");
        }

        artwork.setCoverUrls(String.join(",", coverList));
        artwork.setModelFileUrls(String.join(",", modelList));
        artwork.setUpdatedAt(new Date());
        artworkRepository.save(artwork);

        return ResponseEntity.ok().build();
    }
}