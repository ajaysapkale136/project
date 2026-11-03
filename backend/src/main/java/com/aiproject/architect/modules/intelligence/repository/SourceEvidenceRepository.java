package com.aiproject.architect.modules.intelligence.repository;

import com.aiproject.architect.modules.intelligence.entity.SourceEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SourceEvidenceRepository extends JpaRepository<SourceEvidence, String> {
    List<SourceEvidence> findAllByBusinessRuleId(String businessRuleId);
    List<SourceEvidence> findAllByComponentId(String componentId);
}
