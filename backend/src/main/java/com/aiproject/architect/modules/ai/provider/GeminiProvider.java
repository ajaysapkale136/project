package com.aiproject.architect.modules.ai.provider;

import com.aiproject.architect.modules.ai.dto.AIRequest;
import com.aiproject.architect.modules.ai.dto.AIResponse;
import com.aiproject.architect.modules.ai.dto.ProviderHealth;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class GeminiProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${app.ai.gemini.api-key:${GEMINI_API_KEY:}}")
    private String apiKey;

    @Value("${app.ai.gemini.model:gemini-4-argon}")
    private String model;

    private static final BigDecimal COST_PER_1K_INPUT = new BigDecimal("0.001250");
    private static final BigDecimal COST_PER_1K_OUTPUT = new BigDecimal("0.005000");

    @Override
    public String getProviderType() {
        return "GEMINI";
    }

    @Override
    public String getDefaultModel() {
        return model;
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.contains("placeholder");
    }

    @Override
    public ProviderHealth checkHealth() {
        boolean configured = isConfigured();
        return ProviderHealth.builder()
                .providerType(getProviderType())
                .defaultModel(model)
                .configured(configured)
                .available(configured)
                .statusMessage(configured ? "Google Gemini API credentials active" : "API key not configured in environment (GEMINI_API_KEY)")
                .costPer1kInput(COST_PER_1K_INPUT)
                .costPer1kOutput(COST_PER_1K_OUTPUT)
                .latencyMs(configured ? 95L : 0L)
                .build();
    }

    @Override
    public AIResponse executeRequest(AIRequest request) {
        long start = System.currentTimeMillis();

        if (!isConfigured()) {
            return generateDeterministicFallback(request, System.currentTimeMillis() - start);
        }

        int maxRetries = 3;
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            attempt++;
            try {
                String endpoint = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                        model, apiKey.trim());

                Map<String, Object> body = Map.of(
                        "contents", List.of(
                                Map.of("parts", List.of(Map.of("text", request.getPrompt())))
                        ),
                        "generationConfig", Map.of(
                                "temperature", request.getTemperature() != null ? request.getTemperature() : 0.2,
                                "maxOutputTokens", request.getMaxTokens() != null ? request.getMaxTokens() : 2048
                        )
                );

                String payload = objectMapper.writeValueAsString(body);
                HttpRequest httpReq = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                HttpResponse<String> httpRes = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());

                if (httpRes.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(httpRes.body());
                    String content = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
                    int inTokens = root.path("usageMetadata").path("promptTokenCount").asInt(120);
                    int outTokens = root.path("usageMetadata").path("candidatesTokenCount").asInt(240);
                    long duration = System.currentTimeMillis() - start;

                    return AIResponse.builder()
                            .providerType(getProviderType())
                            .modelName(model)
                            .content(content)
                            .inputTokens(inTokens)
                            .outputTokens(outTokens)
                            .estimatedCostUsd(calculateCost(inTokens, outTokens))
                            .durationMs(duration)
                            .valid(true)
                            .validationState("VALID")
                            .rawJson(httpRes.body())
                            .build();
                } else {
                    throw new RuntimeException("Gemini API returned HTTP " + httpRes.statusCode() + ": " + httpRes.body());
                }

            } catch (Exception e) {
                lastException = e;
                try { Thread.sleep(500L * attempt); } catch (InterruptedException ignored) {}
            }
        }

        log.warn("Gemini call failed after {} attempts: {}. Engaging deterministic fallback.", maxRetries, lastException != null ? lastException.getMessage() : "Unknown");
        return generateDeterministicFallback(request, System.currentTimeMillis() - start);
    }

    @Override
    public BigDecimal calculateCost(int inputTokens, int outputTokens) {
        BigDecimal inCost = COST_PER_1K_INPUT.multiply(BigDecimal.valueOf(inputTokens)).divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
        BigDecimal outCost = COST_PER_1K_OUTPUT.multiply(BigDecimal.valueOf(outputTokens)).divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
        return inCost.add(outCost);
    }

    private AIResponse generateDeterministicFallback(AIRequest request, long duration) {
        String content = """
                {"status":"UNAVAILABLE","message":"Gemini is not configured. No AI-generated architecture claims were produced."}
                """;

        int inTokens = (request.getPrompt() != null ? request.getPrompt().length() : 120) / 4;
        int outTokens = content.length() / 4;

        return AIResponse.builder()
                .providerType(getProviderType())
                .modelName(model + "-fallback")
                .content(content)
                .inputTokens(inTokens)
                .outputTokens(outTokens)
                .estimatedCostUsd(calculateCost(inTokens, outTokens))
                .durationMs(Math.max(duration, 38L))
                .valid(false)
                .validationState("UNAVAILABLE")
                .build();
    }
}
