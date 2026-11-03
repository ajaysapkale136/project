package com.aiproject.architect.modules.ai.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudioQueryRequest {
    private String prompt;
    private String activeModuleId;
    private String activeOperation;
    private String preferredProvider; // AUTO | OPENAI | CLAUDE | GEMINI
}
