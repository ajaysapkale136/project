package com.aiproject.architect.modules.ai.entity;

import com.aiproject.architect.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ai_interaction_traces")
public class AiInteractionTrace extends BaseEntity {

    @Column(name = "project_id", nullable = false, length = 36)
    private String projectId;

    @Column(name = "snapshot_id", length = 36)
    private String snapshotId;

    @Column(name = "provider_type", nullable = false, length = 50)
    private String providerType; // OPENAI, GEMINI, CLAUDE

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "task_type", nullable = false, length = 100)
    private String taskType; // ARCHITECTURE_SYNTHESIS, CODE_REVIEW, CONSENSUS_SYNTHESIS

    @Column(name = "prompt_version", nullable = false, length = 50)
    private String promptVersion;

    @Column(name = "input_tokens", nullable = false)
    @Builder.Default
    private Integer inputTokens = 0;

    @Column(name = "output_tokens", nullable = false)
    @Builder.Default
    private Integer outputTokens = 0;

    @Column(name = "estimated_cost_usd", nullable = false, precision = 10, scale = 6)
    @Builder.Default
    private BigDecimal estimatedCostUsd = BigDecimal.ZERO;

    @Column(name = "execution_duration_ms", nullable = false)
    @Builder.Default
    private Long executionDurationMs = 0L;

    @Column(name = "validation_state", nullable = false, length = 50)
    @Builder.Default
    private String validationState = "VALID"; // VALID, INVALID_SCHEMA, TIMEOUT, RATE_LIMITED

    @Column(name = "request_payload_json", columnDefinition = "TEXT")
    private String requestPayloadJson;

    @Column(name = "response_payload_json", columnDefinition = "TEXT")
    private String responsePayloadJson;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
