package com.aiproject.architect.modules.architect.repository;

import com.aiproject.architect.modules.architect.entity.ArchitectureVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArchitectureVersionRepository extends JpaRepository<ArchitectureVersion, String> {
    Optional<ArchitectureVersion> findTopByAnalysisIdAndVersionTypeOrderByRevisionNumberDesc(String analysisId, String versionType);
    List<ArchitectureVersion> findAllByAnalysisIdOrderByRevisionNumberDesc(String analysisId);
}
