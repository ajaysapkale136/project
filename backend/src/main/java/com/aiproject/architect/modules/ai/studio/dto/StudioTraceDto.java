package com.aiproject.architect.modules.ai.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudioTraceDto {
    private String primaryProvider;
    private String model;
    private Long durationMs;
    private Integer inputTokens;
    private Integer outputTokens;
    private BigDecimal estimatedCostUsd;
    private String taskDistribution; // e.g. "GPT-4o (Reasoning) + Claude 3.5 (AST Code) + Gemini 1.5 (Visuals)"
}
