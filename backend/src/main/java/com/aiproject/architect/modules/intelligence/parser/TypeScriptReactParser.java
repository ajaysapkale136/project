package com.aiproject.architect.modules.intelligence.parser;

import com.aiproject.architect.common.ConfidenceState;
import com.aiproject.architect.modules.intelligence.entity.AnalysisComponent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class TypeScriptReactParser {

    private static final Logger log = LoggerFactory.getLogger(TypeScriptReactParser.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<AnalysisComponent> parse(Path filePath, String relativePath, String moduleId) {
        List<AnalysisComponent> components = new ArrayList<>();
        try {
            String fileName = filePath.getFileName().toString();
            Process process = new ProcessBuilder("node", scriptPath().toString(), filePath.toAbsolutePath().toString())
                    .redirectErrorStream(true).start();
            if (!process.waitFor(Duration.ofSeconds(8).toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("TypeScript parser timed out");
            }
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().reduce("", (left, right) -> left + right);
            }
            if (process.exitValue() != 0) throw new IllegalStateException("TypeScript compiler parser failed: " + output);
            var root = objectMapper.readTree(output);
            for (var parsed : root.path("components")) {
                String componentType = parsed.path("componentType").asText("UI_COMPONENT");
                int endpointCount = parsed.path("endpoints").isArray() ? parsed.path("endpoints").size() : 0;
                Map<String, Object> details = objectMapper.convertValue(parsed, Map.class);
                details.put("parser", root.path("parser").asText());
                details.put("fileExtension", fileName.substring(fileName.lastIndexOf('.') + 1));
                components.add(AnalysisComponent.builder()
                        .moduleId(moduleId).name(parsed.path("name").asText())
                        .componentType(componentType).filePath(relativePath).endpointCount(endpointCount)
                        .confidenceState(ConfidenceState.VERIFIED).detailsJson(objectMapper.writeValueAsString(details)).build());
            }

        } catch (Exception e) {
            log.warn("Could not parse TS/JS file {}: {}", relativePath, e.getMessage());
        }
        return components;
    }

    private Path scriptPath() {
        return Path.of(System.getProperty("user.dir"), "scripts", "typescript-ast.mjs");
    }
}
