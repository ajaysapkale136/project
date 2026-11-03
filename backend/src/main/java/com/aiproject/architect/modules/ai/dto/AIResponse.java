package com.aiproject.architect.modules.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIResponse {
    private String providerType;
    private String modelName;
    private String content;
    private Integer inputTokens;
    private Integer outputTokens;
    private BigDecimal estimatedCostUsd;
    private Long durationMs;
    private boolean valid;
    private String validationState;
    private String rawJson;
    private String errorMessage;
}
