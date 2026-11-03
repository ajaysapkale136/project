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
@Table(name = "detected_technologies")
public class DetectedTechnology extends BaseEntity {

    @Column(name = "analysis_id", nullable = false, length = 36)
    private String analysisId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "category", nullable = false, length = 100)
    private String category; // BACKEND, FRONTEND, DATABASE, BUILD_TOOL, DEPLOYMENT, TESTING

    @Column(name = "version", length = 100)
    private String version;

    @Column(name = "detection_source", length = 255)
    private String detectionSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_state", nullable = false, length = 50)
    @Builder.Default
    private ConfidenceState confidenceState = ConfidenceState.VERIFIED;
}
