package com.aiproject.architect.modules.patch.dto;

import com.aiproject.architect.modules.patch.entity.ChangeSet;
import com.aiproject.architect.modules.patch.entity.ChangeSetFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeSetResponse {
    private ChangeSet changeSet;
    private List<ChangeSetFile> files;
    private TechnicalPlanDto technicalPlan;
    private List<BusinessRuleImpactDto> businessRuleImpact;
    private ApiCompatibilityImpactDto apiCompatibilityImpact;
    private String migrationImpact;
    private boolean isStale;
}
