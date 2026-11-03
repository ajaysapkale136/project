package com.aiproject.architect.modules.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiModelReviewRequest {
    private String primaryProvider; // OPENAI, GEMINI, CLAUDE
    private List<String> reviewerProviders;
    private String taskFocus; // e.g. "Security & Transaction Isolation"
}
