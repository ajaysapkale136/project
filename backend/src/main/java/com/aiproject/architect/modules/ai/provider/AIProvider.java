package com.aiproject.architect.modules.ai.provider;

import com.aiproject.architect.modules.ai.dto.AIRequest;
import com.aiproject.architect.modules.ai.dto.AIResponse;
import com.aiproject.architect.modules.ai.dto.ProviderHealth;

import java.math.BigDecimal;

public interface AIProvider {

    String getProviderType();

    String getDefaultModel();

    boolean isConfigured();

    ProviderHealth checkHealth();

    AIResponse executeRequest(AIRequest request);

    BigDecimal calculateCost(int inputTokens, int outputTokens);
}
