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
public class ProviderHealth {
    private String providerType;
    private String defaultModel;
    private boolean configured;
    private boolean available;
    private String statusMessage;
    private BigDecimal costPer1kInput;
    private BigDecimal costPer1kOutput;
    private Long latencyMs;
}
