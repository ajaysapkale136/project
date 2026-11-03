package com.aiproject.architect.modules.intelligence.repository;

import com.aiproject.architect.modules.intelligence.entity.DetectedTechnology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetectedTechnologyRepository extends JpaRepository<DetectedTechnology, String> {
    List<DetectedTechnology> findAllByAnalysisId(String analysisId);
}
