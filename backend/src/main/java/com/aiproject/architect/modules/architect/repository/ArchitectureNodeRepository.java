package com.aiproject.architect.modules.architect.repository;

import com.aiproject.architect.modules.architect.entity.ArchitectureNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArchitectureNodeRepository extends JpaRepository<ArchitectureNode, String> {
    List<ArchitectureNode> findAllByVersionId(String versionId);
}
