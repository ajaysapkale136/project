package com.aiproject.architect.modules.ingestion.repository;

import com.aiproject.architect.modules.ingestion.entity.ProjectFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectFileRepository extends JpaRepository<ProjectFile, String> {
    List<ProjectFile> findAllBySnapshotId(String snapshotId);
    List<ProjectFile> findAllBySnapshotIdAndIsBinaryFalse(String snapshotId);
    long countBySnapshotId(String snapshotId);
}
