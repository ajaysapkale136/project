package com.aiproject.architect.modules.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProposalRequest {
    private String moduleName;
    private String title;
    private String description;
    private String targetLanguage;
    private String providerType; // AUTO, GEMINI, OPENAI, CLAUDE
    @Builder.Default
    private boolean preserveBusinessRules = true;
}
