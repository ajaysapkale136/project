package com.aiproject.architect.modules.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefactorComponentRequest {
    private String componentId;
    private String refactorGoal;
    private String providerType; // AUTO, GEMINI, OPENAI, CLAUDE
}
