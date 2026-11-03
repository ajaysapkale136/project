package com.aiproject.architect.modules.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRequest {
    private String projectId;
    private String taskType;
    private String prompt;
    private String systemPrompt;
    private Integer maxTokens;
    private Double temperature;
    private Map<String, Object> contextData;
}
