package com.aiproject.architect.modules.architect.repository;

import com.aiproject.architect.modules.architect.entity.ArchitectureEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArchitectureEdgeRepository extends JpaRepository<ArchitectureEdge, String> {
    List<ArchitectureEdge> findAllByVersionId(String versionId);
}
