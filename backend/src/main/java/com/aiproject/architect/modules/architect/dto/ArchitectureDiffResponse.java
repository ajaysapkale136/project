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
public class ArchitectureDiffResponse {
    private String projectId;
    private String existingVersionId;
    private String proposedVersionId;
    private Integer existingNodeCount;
    private Integer proposedNodeCount;
    private List<String> addedCapabilities;
    private List<String> resolvedAntiPatterns;
    private String riskLevel;
    private String rollbackStrategy;
}
