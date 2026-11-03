package com.aiproject.architect.modules.intelligence.repository;

import com.aiproject.architect.modules.intelligence.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectAnalysisRepository extends JpaRepository<ProjectAnalysis, String> {
    Optional<ProjectAnalysis> findTopBySnapshotIdOrderByCreatedAtDesc(String snapshotId);
    List<ProjectAnalysis> findAllBySnapshotIdOrderByCreatedAtDesc(String snapshotId);
}
