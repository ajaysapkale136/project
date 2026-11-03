package com.aiproject.architect.modules.ai.provider;

import com.aiproject.architect.common.exception.AppException;
import com.aiproject.architect.modules.ai.dto.ProviderHealth;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ProviderRegistry {

    private final Map<String, AIProvider> providers = new HashMap<>();

    public ProviderRegistry(List<AIProvider> providerList) {
        for (AIProvider provider : providerList) {
            providers.put(provider.getProviderType().toUpperCase(), provider);
        }
    }

    public AIProvider getProvider(String providerType) {
        if (providerType == null || providerType.trim().isEmpty()) {
            return getDefaultProvider();
        }
        AIProvider provider = providers.get(providerType.toUpperCase().trim());
        if (provider == null) {
            throw new AppException("Unsupported AI provider: " + providerType + ". Supported: " + providers.keySet());
        }
        return provider;
    }

    public AIProvider getDefaultProvider() {
        // Preference: Configured Gemini -> Configured OpenAI -> Configured Claude -> Gemini fallback
        for (String p : List.of("GEMINI", "OPENAI", "CLAUDE")) {
            AIProvider provider = providers.get(p);
            if (provider != null && provider.isConfigured()) {
                return provider;
            }
        }
        return providers.getOrDefault("GEMINI", providers.values().iterator().next());
    }

    public List<ProviderHealth> getAllHealth() {
        return providers.values().stream()
                .map(AIProvider::checkHealth)
                .collect(Collectors.toList());
    }

    public List<String> getAvailableProviders() {
        return List.copyOf(providers.keySet());
    }
}
