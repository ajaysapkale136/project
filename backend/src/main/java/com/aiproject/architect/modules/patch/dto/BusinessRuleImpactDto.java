package com.aiproject.architect.modules.patch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessRuleImpactDto {
    private String ruleCode;
    private String title;
    private String existingRule;
    private String proposedChange;
    private String sourceEvidence;
    private String affectedClass;
    private String affectedModules;
    private String apiCompatibilityRisk;
    private String dataMigrationRisk;
    private String securityImpact;
    private String regressionTests;
    private String rollbackPlan;
}
