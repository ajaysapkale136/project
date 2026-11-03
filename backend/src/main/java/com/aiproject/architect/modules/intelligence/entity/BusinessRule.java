package com.aiproject.architect.modules.intelligence.entity;

import com.aiproject.architect.common.BaseEntity;
import com.aiproject.architect.common.ConfidenceState;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "business_rules")
public class BusinessRule extends BaseEntity {

    @Column(name = "analysis_id", nullable = false, length = 36)
    private String analysisId;

    @Column(name = "rule_code", nullable = false, length = 100)
    private String ruleCode;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", nullable = false, length = 100)
    private String category; // AUTH_SECURITY, DATA_INTEGRITY, TRANSACTIONAL, API_CONTRACT, WORKFLOW

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_state", nullable = false, length = 50)
    @Builder.Default
    private ConfidenceState confidenceState = ConfidenceState.VERIFIED;
}
