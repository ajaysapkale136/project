package com.aiproject.architect.modules.intelligence.repository;

import com.aiproject.architect.modules.intelligence.entity.AnalysisModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisModuleRepository extends JpaRepository<AnalysisModule, String> {
    List<AnalysisModule> findAllByAnalysisId(String analysisId);
}
