package com.aiproject.architect.modules.intelligence.repository;

import com.aiproject.architect.modules.intelligence.entity.AnalysisComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisComponentRepository extends JpaRepository<AnalysisComponent, String> {
    List<AnalysisComponent> findAllByModuleId(String moduleId);
    List<AnalysisComponent> findAllByComponentType(String componentType);
}
