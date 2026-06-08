package com.jin.blender.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public ArtworkStorageResult storeArtworkFiles(List<MultipartFile> coverFiles, List<MultipartFile> modelFiles, String subDir) throws IOException {
        Path subPath = ensureSubDirectory(subDir);

        MultipartFile previewFile = firstNonEmptyFile(coverFiles);
        String coverUrls = "";
        if (previewFile != null) {
            Path previewPath = storeFile(previewFile, subPath);
            coverUrls = buildUploadUrl(subDir, previewPath.getFileName().toString());
        }

        List<MultipartFile> archiveFiles = new ArrayList<>();
        if (coverFiles != null && coverFiles.size() > 1) {
            for (int i = 1; i < coverFiles.size(); i++) {
                MultipartFile file = coverFiles.get(i);
                if (file != null && !file.isEmpty()) {
                    archiveFiles.add(file);
                }
            }
        }
        if (modelFiles != null) {
            for (MultipartFile file : modelFiles) {
                if (file != null && !file.isEmpty()) {
                    archiveFiles.add(file);
                }
            }
        }

        String modelFileUrls = "";
        if (!archiveFiles.isEmpty()) {
            String safeZipName = sanitizeFileName(Paths.get(subDir).getFileName().toString()) + ".zip";
            Path zipPath = createZipFile(archiveFiles, subPath, safeZipName);
            modelFileUrls = buildUploadUrl(subDir, zipPath.getFileName().toString());
        }

        return new ArtworkStorageResult(coverUrls, modelFileUrls);
    }

    public String storeFiles(List<MultipartFile> files, String subDir) throws IOException {
        if (files == null || files.isEmpty()) {
            return "";
        }

        Path subPath = ensureSubDirectory(subDir);

        List<String> savedPaths = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            Path filePath = storeFile(file, subPath);
            savedPaths.add(buildUploadUrl(subDir, filePath.getFileName().toString()));
        }

        return String.join(",", savedPaths);
    }

    public String storeArchiveFiles(List<MultipartFile> files, String subDir, String archiveBaseName) throws IOException {
        if (files == null || files.isEmpty()) {
            return "";
        }

        List<MultipartFile> nonEmptyFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                nonEmptyFiles.add(file);
            }
        }

        if (nonEmptyFiles.isEmpty()) {
            return "";
        }

        Path subPath = ensureSubDirectory(subDir);
        String zipName = sanitizeFileName(archiveBaseName) + ".zip";
        Path zipPath = createZipFile(nonEmptyFiles, subPath, zipName);
        return buildUploadUrl(subDir, zipPath.getFileName().toString());
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
            pruneEmptyDirectories(filePath.getParent());
        }
    }

    public void deleteFiles(String fileUrls) throws IOException {
        if (fileUrls == null || fileUrls.trim().isEmpty()) {
            return;
        }

        String[] urls = fileUrls.split(",");
        for (String url : urls) {
            if (url == null || url.trim().isEmpty()) {
                continue;
            }
            deleteFile(url.trim());
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

    private void pruneEmptyDirectories(Path currentPath) throws IOException {
        if (currentPath == null) {
            return;
        }

        Path rootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path normalizedCurrent = currentPath.toAbsolutePath().normalize();

        while (normalizedCurrent != null
                && !normalizedCurrent.equals(rootPath)
                && normalizedCurrent.startsWith(rootPath)
                && Files.exists(normalizedCurrent)
                && Files.isDirectory(normalizedCurrent)) {
            try (Stream<Path> children = Files.list(normalizedCurrent)) {
                if (children.findAny().isPresent()) {
                    break;
                }
            } catch (IOException e) {
                break;
            }

            Files.deleteIfExists(normalizedCurrent);
            normalizedCurrent = normalizedCurrent.getParent();
        }
    }

    private MultipartFile firstNonEmptyFile(List<MultipartFile> files) {
        if (files == null) {
            return null;
        }
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                return file;
            }
        }
        return null;
    }

    private Path storeFile(MultipartFile file, Path subPath) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String safeFileName = sanitizeFileName(originalFilename);
        Path filePath = resolveUniquePath(subPath, safeFileName);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath);
        }
        return filePath;
    }

    private Path createZipFile(List<MultipartFile> files, Path subPath, String zipName) throws IOException {
        Path zipPath = resolveUniquePath(subPath, zipName);
        Set<String> usedEntryNames = new HashSet<>();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            for (MultipartFile file : files) {
                String entryName = buildUniqueEntryName(sanitizeFileName(file.getOriginalFilename()), usedEntryNames);
                zipOutputStream.putNextEntry(new ZipEntry(entryName));
                try (InputStream inputStream = file.getInputStream()) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        zipOutputStream.write(buffer, 0, len);
                    }
                }
                zipOutputStream.closeEntry();
            }
        }

        return zipPath;
    }

    private String buildUniqueEntryName(String fileName, Set<String> usedEntryNames) {
        String baseName = fileName;
        String extension = "";
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex > 0) {
            baseName = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        }

        String candidate = fileName;
        int counter = 1;
        while (usedEntryNames.contains(candidate)) {
            candidate = baseName + "(" + counter + ")" + extension;
            counter++;
        }
        usedEntryNames.add(candidate);
        return candidate;
    }

    private Path resolveUniquePath(Path subPath, String safeFileName) {
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
        return filePath;
    }

    private String sanitizeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return "file";
        }
        return originalFilename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String buildUploadUrl(String subDir, String fileName) {
        return "/uploads/" + subDir + "/" + fileName;
    }

    public static class ArtworkStorageResult {
        private final String coverUrls;
        private final String modelFileUrls;

        public ArtworkStorageResult(String coverUrls, String modelFileUrls) {
            this.coverUrls = coverUrls;
            this.modelFileUrls = modelFileUrls;
        }

        public String getCoverUrls() {
            return coverUrls;
        }

        public String getModelFileUrls() {
            return modelFileUrls;
        }
    }
}
