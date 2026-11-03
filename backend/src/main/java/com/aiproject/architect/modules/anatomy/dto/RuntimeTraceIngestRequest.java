package com.aiproject.architect.modules.anatomy.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.Map;

public record RuntimeTraceIngestRequest(
        @NotBlank @Size(max = 500) String operation,
        @NotBlank @Size(max = 255) String componentName,
        @NotNull LocalDateTime occurredAt,
        @PositiveOrZero Long durationMs,
        @NotBlank @Size(max = 50) String outcome,
        Map<String, String> attributes) {}
