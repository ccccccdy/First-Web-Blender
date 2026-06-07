package com.jin.blender.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String storeFiles(List<MultipartFile> files, String subDir) throws IOException {
        if (files == null || files.isEmpty()) {
            return "";
        }

        Path rootPath = Paths.get(uploadDir);
        if (!Files.exists(rootPath)) {
            Files.createDirectories(rootPath);
        }

        Path subPath = rootPath.resolve(subDir);
        if (!Files.exists(subPath)) {
            Files.createDirectories(subPath);
        }

        List<String> savedPaths = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String originalFilename = file.getOriginalFilename();
            String safeFileName = originalFilename == null
                    ? "file"
                    : originalFilename.replaceAll("[\\\\/:*?\"<>|]", "_");

            String baseName = safeFileName;
            String extension = "";
            int dotIndex = safeFileName.lastIndexOf(".");
            if (dotIndex > 0) {
                baseName = safeFileName.substring(0, dotIndex);
                extension = safeFileName.substring(dotIndex);
            }

            Path filePath = subPath.resolve(safeFileName);
            int counter = 1;
            while (Files.exists(filePath)) {
                filePath = subPath.resolve(baseName + "(" + counter + ")" + extension);
                counter++;
            }

            Files.copy(file.getInputStream(), filePath);
            savedPaths.add("/uploads/" + subDir + "/" + filePath.getFileName().toString());
        }

        return String.join(",", savedPaths);
    }

    public void deleteFolder(String subDir) throws IOException {
        Path subPath = Paths.get(uploadDir).resolve(subDir);
        if (Files.exists(subPath)) {
            Files.walk(subPath)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    public void deleteFile(String fileUrl) throws IOException {
        Path filePath = resolveUploadUrl(fileUrl);
        if (filePath != null && Files.exists(filePath)) {
            Files.deleteIfExists(filePath);
        }
    }

    public double getFileSizeMB(String fileUrl) throws IOException {
        Path filePath = resolveUploadUrl(fileUrl);
        if (filePath == null || !Files.exists(filePath)) {
            return 0;
        }
        return Files.size(filePath) / (1024.0 * 1024.0);
    }

    private Path resolveUploadUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
            return null;
        }
        String relativePath = fileUrl.substring("/uploads/".length());
        return Paths.get(uploadDir).resolve(relativePath);
    }
}
