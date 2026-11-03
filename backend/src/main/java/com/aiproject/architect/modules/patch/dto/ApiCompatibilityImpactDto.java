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
public class ApiCompatibilityImpactDto {
    private List<String> breakingChanges;
    private List<String> compatibleAdditions;
    private List<String> deprecatedEndpoints;
    private boolean isBackwardCompatible;
}
