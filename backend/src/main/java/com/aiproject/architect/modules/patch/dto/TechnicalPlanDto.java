package com.aiproject.architect.modules.patch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalPlanDto {
    private String targetType;
    private String targetIdentifier;
    private List<String> steps;
    private List<String> detectedConventions;
    private List<String> affectedModules;
    private List<String> affectedEndpoints;
    private List<String> affectedDatabaseTables;
    private String riskAssessment;
}
