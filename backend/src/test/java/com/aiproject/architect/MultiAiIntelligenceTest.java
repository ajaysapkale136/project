package com.aiproject.architect;

import com.aiproject.architect.modules.ai.dto.AIRequest;
import com.aiproject.architect.modules.ai.dto.AIResponse;
import com.aiproject.architect.modules.ai.dto.ProviderHealth;
import com.aiproject.architect.modules.ai.provider.ClaudeProvider;
import com.aiproject.architect.modules.ai.provider.GeminiProvider;
import com.aiproject.architect.modules.ai.provider.OpenAIProvider;
import com.aiproject.architect.modules.ai.provider.ProviderRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MultiAiIntelligenceTest {

    private final OpenAIProvider openAIProvider = new OpenAIProvider();
    private final GeminiProvider geminiProvider = new GeminiProvider();
    private final ClaudeProvider claudeProvider = new ClaudeProvider();
    private final ProviderRegistry registry = new ProviderRegistry(List.of(openAIProvider, geminiProvider, claudeProvider));

    @Test
    @DisplayName("Should initialize and verify all 3 AI provider configurations")
    void testProviderRegistration() {
        assertEquals("OPENAI", openAIProvider.getProviderType());
        assertEquals("GEMINI", geminiProvider.getProviderType());
        assertEquals("CLAUDE", claudeProvider.getProviderType());

        assertNotNull(registry.getProvider("OPENAI"));
        assertNotNull(registry.getProvider("GEMINI"));
        assertNotNull(registry.getProvider("CLAUDE"));

        List<ProviderHealth> healthList = registry.getAllHealth();
        assertEquals(3, healthList.size());
    }

    @Test
    @DisplayName("Should accurately calculate token costs for each provider")
    void testTokenCostEstimation() {
        // 10,000 input tokens, 2,000 output tokens
        BigDecimal gptCost = openAIProvider.calculateCost(10_000, 2_000);
        assertTrue(gptCost.compareTo(BigDecimal.ZERO) > 0);

        BigDecimal geminiCost = geminiProvider.calculateCost(10_000, 2_000);
        assertTrue(geminiCost.compareTo(BigDecimal.ZERO) > 0);

        BigDecimal claudeCost = claudeProvider.calculateCost(10_000, 2_000);
        assertTrue(claudeCost.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should report providers unavailable when live API keys are not present")
    void testFallbackExecution() {
        AIRequest request = AIRequest.builder()
                .projectId("test-p1")
                .taskType("ARCHITECTURE_SYNTHESIS")
                .prompt("Evaluate architecture modularity")
                .build();

        AIResponse openAiResp = openAIProvider.executeRequest(request);
        assertFalse(openAiResp.isValid());
        assertNotNull(openAiResp.getContent());
        assertEquals("UNAVAILABLE", openAiResp.getValidationState());

        AIResponse geminiResp = geminiProvider.executeRequest(request);
        assertFalse(geminiResp.isValid());
        assertNotNull(geminiResp.getContent());
        assertEquals("UNAVAILABLE", geminiResp.getValidationState());

        AIResponse claudeResp = claudeProvider.executeRequest(request);
        assertFalse(claudeResp.isValid());
        assertNotNull(claudeResp.getContent());
        assertEquals("UNAVAILABLE", claudeResp.getValidationState());
    }
}
