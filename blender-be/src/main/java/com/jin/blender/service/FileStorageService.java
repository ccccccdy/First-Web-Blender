package com.jin.blender.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String storeFile(MultipartFile file, String subDir) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        Path subPath = ensureSubDirectory(subDir);
        String safeFileName = sanitizeFileName(file.getOriginalFilename());
        String uniqueFileName = ensureUniqueFileName(subPath, safeFileName);
        Path filePath = subPath.resolve(uniqueFileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/" + subDir + "/" + uniqueFileName;
    }

    public List<String> storeFiles(List<MultipartFile> files, String subDir) throws IOException {
        List<String> savedUrls = new ArrayList<>();
        if (files == null) {
            return savedUrls;
        }

        for (MultipartFile file : files) {
            String savedUrl = storeFile(file, subDir);
            if (savedUrl != null) {
                savedUrls.add(savedUrl);
            }
        }
        return savedUrls;
    }

    public void deleteFile(String fileUrl) throws IOException {
        Path filePath = resolveUploadUrl(fileUrl);
        if (filePath != null && Files.exists(filePath)) {
            Files.deleteIfExists(filePath);
        }
    }

    public void deleteFolder(String subDir) throws IOException {
        Path subPath = Paths.get(uploadDir).resolve(subDir);
        if (!Files.exists(subPath)) {
            return;
        }

        Files.walk(subPath)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private Path ensureSubDirectory(String subDir) throws IOException {
        Path rootPath = Paths.get(uploadDir);
        if (!Files.exists(rootPath)) {
            Files.createDirectories(rootPath);
        }

        Path subPath = rootPath.resolve(subDir);
        if (!Files.exists(subPath)) {
            Files.createDirectories(subPath);
        }
        return subPath;
    }

    private String sanitizeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return "file";
        }
        return originalFilename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String ensureUniqueFileName(Path directory, String safeFileName) {
        String baseName = safeFileName;
        String extension = "";
        int dotIndex = safeFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = safeFileName.substring(0, dotIndex);
            extension = safeFileName.substring(dotIndex);
        }

        Path filePath = directory.resolve(safeFileName);
        int counter = 1;
        while (Files.exists(filePath)) {
            String candidate = baseName + "(" + counter + ")" + extension;
            filePath = directory.resolve(candidate);
            counter++;
        }

        return filePath.getFileName().toString();
    }

    private Path resolveUploadUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
            return null;
        }
        String relativePath = fileUrl.substring("/uploads/".length());
        return Paths.get(uploadDir).resolve(relativePath);
    }
}
