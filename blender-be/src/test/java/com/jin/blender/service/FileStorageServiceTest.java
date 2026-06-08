package com.jin.blender.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    public void storeArtworkFilesStoresPreviewSeparatelyAndArchivesRemainingFiles() throws IOException {
        FileStorageService service = new FileStorageService();
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());

        MockMultipartFile preview = new MockMultipartFile(
                "coverFiles",
                "preview.png",
                "image/png",
                "preview".getBytes()
        );
        MockMultipartFile media = new MockMultipartFile(
                "coverFiles",
                "scene.mp4",
                "video/mp4",
                "media".getBytes()
        );
        MockMultipartFile model = new MockMultipartFile(
                "modelFiles",
                "model.blend",
                "application/octet-stream",
                "model".getBytes()
        );

        FileStorageService.ArtworkStorageResult result = service.storeArtworkFiles(
                Arrays.asList(preview, media),
                Arrays.asList(model),
                "作品A"
        );

        Path artworkDir = tempDir.resolve("作品A");
        Path previewPath = artworkDir.resolve("preview.png");
        Path zipPath = artworkDir.resolve("作品A.zip");

        assertTrue(Files.exists(artworkDir));
        assertTrue(Files.exists(previewPath));
        assertTrue(Files.exists(zipPath));
        assertEquals("/uploads/作品A/preview.png", result.getCoverUrls());
        assertEquals("/uploads/作品A/作品A.zip", result.getModelFileUrls());

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            List<String> entryNames = zipFile.stream()
                    .map(entry -> entry.getName())
                    .sorted()
                    .collect(Collectors.toList());

            assertEquals(Arrays.asList("model.blend", "scene.mp4"), entryNames);
        }
    }

    @Test
    public void storeArchiveFilesCreatesSingleZipForResourceUploads() throws IOException {
        FileStorageService service = new FileStorageService();
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());

        MockMultipartFile readme = new MockMultipartFile(
                "assetFiles",
                "readme.txt",
                "text/plain",
                "hello".getBytes()
        );
        MockMultipartFile model = new MockMultipartFile(
                "assetFiles",
                "chair.blend",
                "application/octet-stream",
                "blend".getBytes()
        );

        String fileUrl = service.storeArchiveFiles(
                Arrays.asList(readme, model),
                "assets/AssetA",
                "AssetA"
        );

        Path zipPath = tempDir.resolve("assets").resolve("AssetA").resolve("AssetA.zip");

        assertTrue(Files.exists(zipPath));
        assertEquals("/uploads/assets/AssetA/AssetA.zip", fileUrl);

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            List<String> entryNames = zipFile.stream()
                    .map(entry -> entry.getName())
                    .sorted()
                    .collect(Collectors.toList());

            assertEquals(Arrays.asList("chair.blend", "readme.txt"), entryNames);
        }
    }
}
