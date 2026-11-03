package com.aiproject.architect.modules.intelligence.repository;

import com.aiproject.architect.modules.intelligence.entity.BusinessRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessRuleRepository extends JpaRepository<BusinessRule, String> {
    List<BusinessRule> findAllByAnalysisId(String analysisId);
}
