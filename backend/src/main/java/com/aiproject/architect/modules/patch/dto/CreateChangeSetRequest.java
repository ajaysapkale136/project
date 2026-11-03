package com.aiproject.architect.modules.patch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChangeSetRequest {
    private String targetType; // MODULE, COMPONENT, BUG, FEATURE, REFACTOR, API_OPERATION
    private String targetIdentifier;
    private String title;
    private String description;
    @Builder.Default
    private boolean preserveBusinessRules = true;
}
