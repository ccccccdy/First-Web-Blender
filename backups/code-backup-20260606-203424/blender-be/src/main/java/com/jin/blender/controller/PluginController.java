package com.jin.blender.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jin.blender.entity.Plugin;
import com.jin.blender.repository.PluginRepository;
import com.jin.blender.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

@RestController
@RequestMapping("/api/plugins")
@CrossOrigin(origins = "http://localhost:5173")
public class PluginController {

    @Autowired
    private PluginRepository pluginRepository;

    @Autowired
    private FileStorageService fileStorageService;

    // 获取所有插件（支持按版本筛选，可选）
    @GetMapping
    public List<Plugin> getAll(@RequestParam(value = "version", required = false) String version) {
        if (version != null && !version.isEmpty()) {
            return pluginRepository.findByBlenderVersion(version);
        }
        return pluginRepository.findAll();
    }

    // 上传插件
    @PostMapping
    public ResponseEntity<?> createPlugin(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("blenderVersion") String blenderVersion,
            @RequestParam("pluginFiles") List<MultipartFile> pluginFiles) {

        try {
            // 使用插件标题作为子目录名（安全处理）
            String safeTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_");
            String subDir = "plugins/" + safeTitle;   // 统一存放在 up/plugins/ 下
            String fileUrls = fileStorageService.storeFiles(pluginFiles, subDir);

            Plugin plugin = new Plugin();
            plugin.setTitle(title);
            plugin.setDescription(description == null ? "" : description);
            plugin.setBlenderVersion(blenderVersion);
            plugin.setFileUrls(fileUrls);
            plugin.setCreatedAt(new Date());
            plugin.setUpdatedAt(new Date());

            Plugin saved = pluginRepository.save(plugin);
            return ResponseEntity.ok(saved);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("文件上传失败：" + e.getMessage());
        }
    }

    // 删除插件（同时删除磁盘文件）
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlugin(@PathVariable Long id) {
        Plugin plugin = pluginRepository.findById(id).orElse(null);
        if (plugin == null) return ResponseEntity.notFound().build();

        // 删除磁盘文件（根据 fileUrls 中的路径删除对应的子目录）
        String safeTitle = plugin.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_");
        String subDir = "plugins/" + safeTitle;
        try {
            fileStorageService.deleteFolder(subDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        pluginRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    private final ObjectMapper objectMapper = new ObjectMapper();
    @PutMapping
    public ResponseEntity<?> updatePlugin(
            @RequestParam("id") Long id,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("blenderVersion") String blenderVersion,
            @RequestParam(value = "removedFileIndexes", required = false) String removedFileIndexes,
            @RequestParam(value = "newFiles", required = false) List<MultipartFile> newFiles) {

        // 1. 查找原插件
        Plugin plugin = pluginRepository.findById(id).orElse(null);
        if (plugin == null) {
            return ResponseEntity.notFound().build();
        }

        // 2. 更新基本字段
        plugin.setTitle(title);
        plugin.setDescription(description);
        plugin.setBlenderVersion(blenderVersion);

        // 3. 获取现有文件列表
        List<String> fileList = plugin.getFileUrls() == null ? new ArrayList<>()
                : new ArrayList<>(Arrays.asList(plugin.getFileUrls().split(",")));

        // 4. 删除被移除的文件（物理删除 + 从列表中移除）
        if (removedFileIndexes != null && !removedFileIndexes.isEmpty()) {
            try {
                List<Integer> removeIdx = objectMapper.readValue(removedFileIndexes, new TypeReference<List<Integer>>() {});
                removeIdx.sort(Comparator.reverseOrder());
                for (int idx : removeIdx) {
                    if (idx < fileList.size()) {
                        String fileUrl = fileList.get(idx);
                        // 物理删除文件
                        try {
                            fileStorageService.deleteFile(fileUrl);
                        } catch (IOException e) {
                            e.printStackTrace();
                            // 可以记录日志但继续处理
                        }
                        fileList.remove(idx);
                    }
                }
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("文件索引格式错误");
            }
        }

        // 5. 保存新上传的文件
        String safeTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_");
        String subDir = "plugins/" + safeTitle;
        if (newFiles != null && !newFiles.isEmpty()) {
            try {
                String newFilePaths = fileStorageService.storeFiles(newFiles, subDir);
                if (newFilePaths != null && !newFilePaths.isEmpty()) {
                    fileList.addAll(Arrays.asList(newFilePaths.split(",")));
                }
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("文件保存失败");
            }
        }

        // 6. 更新数据库
        plugin.setFileUrls(String.join(",", fileList));
        plugin.setUpdatedAt(new Date());
        pluginRepository.save(plugin);

        return ResponseEntity.ok().build();
    }
}