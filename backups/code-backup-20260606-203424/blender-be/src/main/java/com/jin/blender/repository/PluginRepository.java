package com.jin.blender.repository;

import com.jin.blender.entity.Plugin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PluginRepository extends JpaRepository<Plugin, Long> {
    // 可按版本查询（可选，用于筛选）
    List<Plugin> findByBlenderVersion(String blenderVersion);
}