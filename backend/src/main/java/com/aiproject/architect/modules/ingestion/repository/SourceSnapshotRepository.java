package com.aiproject.architect.modules.ingestion.repository;

import com.aiproject.architect.modules.ingestion.entity.SourceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SourceSnapshotRepository extends JpaRepository<SourceSnapshot, String> {
    Optional<SourceSnapshot> findTopByProjectIdOrderByCreatedAtDesc(String projectId);
    List<SourceSnapshot> findAllByProjectIdOrderByCreatedAtDesc(String projectId);
}
