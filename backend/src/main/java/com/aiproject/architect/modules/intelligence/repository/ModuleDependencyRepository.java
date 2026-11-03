package com.aiproject.architect.modules.intelligence.repository;

import com.aiproject.architect.modules.intelligence.entity.ModuleDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModuleDependencyRepository extends JpaRepository<ModuleDependency, String> {
    List<ModuleDependency> findAllByAnalysisId(String analysisId);
}
