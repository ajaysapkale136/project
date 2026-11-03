package com.aiproject.architect.modules.security.hardening;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class PromptSanitizer {

    @Data
    @Builder
    public static class SanitizationResult {
        private boolean isSafe;
        private String sanitizedPrompt;
        private List<String> flaggedThreats;
    }

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)ignore (all )?(prior|previous) instructions"),
            Pattern.compile("(?i)(system prompt override|developer mode enabled|dan mode)"),
            Pattern.compile("(?i)reveal (the )?(system prompt|api[ _-]?key|credentials|secret)"),
            Pattern.compile("(?i)disregard security guidelines"),
            Pattern.compile("(?i)<script.*?>.*?</script>"),
            Pattern.compile("(?i)drop table|truncate table|delete from .* where 1=1")
    );

    public SanitizationResult sanitize(String input) {
        if (input == null || input.isBlank()) {
            return SanitizationResult.builder().isSafe(true).sanitizedPrompt("").flaggedThreats(List.of()).build();
        }

        List<String> threats = new ArrayList<>();
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                threats.add("Detected prompt injection/jailbreak pattern: " + pattern.pattern());
            }
        }

        // Basic escape of dangerous script/injection tags
        String sanitized = input.replaceAll("(?i)<script.*?>.*?</script>", "[STRIPPED_SCRIPT]");

        return SanitizationResult.builder()
                .isSafe(threats.isEmpty())
                .sanitizedPrompt(sanitized)
                .flaggedThreats(threats)
                .build();
    }
}
