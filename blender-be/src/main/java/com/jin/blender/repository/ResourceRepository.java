package com.jin.blender.repository;

import com.jin.blender.entity.Resource;
import com.jin.blender.entity.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findAllByTypeOrderByCreatedAtDesc(ResourceType type);

    List<Resource> findAllByTypeAndBlenderVersionOrderByCreatedAtDesc(ResourceType type, String blenderVersion);

    Optional<Resource> findByIdAndType(Long id, ResourceType type);
}
