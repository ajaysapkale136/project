package com.aiproject.architect.modules.architect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleBlueprintResponse {
    private String moduleName;
    private String purpose;
    private List<String> businessRequirements;
    private List<String> functionalRequirements;
    private List<String> nonFunctionalRequirements;
    private List<String> folderStructure;
    private List<String> testCases;
    private List<String> acceptanceCriteria;
    private List<String> implementationSequence;
    private String antigravityPrompt;
}
