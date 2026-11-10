
package com.aiproject.architect.modules.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRequest {

    private String projectId;

    @NotBlank(message = "Task type is required")
    private String taskType;

    @NotBlank(message = "Prompt is required")
    @Size(max = 50000, message = "Prompt must not exceed 50000 characters")
    private String prompt;

    @Size(max = 20000, message = "System prompt must not exceed 20000 characters")
    private String systemPrompt;

    @NotNull(message = "Max tokens is required")
    @Min(value = 1, message = "Max tokens must be at least 1")
    @Max(value = 32000, message = "Max tokens cannot exceed 32000")
    @Builder.Default
    private Integer maxTokens = 4000;

    @Min(value = 0, message = "Temperature cannot be below 0")
    @Max(value = 2, message = "Temperature cannot exceed 2")
    @Builder.Default
    private Double temperature = 0.7;
}
