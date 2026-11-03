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
@Table(name = "analysis_components")
public class AnalysisComponent extends BaseEntity {

    @Column(name = "module_id", nullable = false, length = 36)
    private String moduleId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "component_type", nullable = false, length = 100)
    private String componentType; // CONTROLLER, SERVICE, REPOSITORY, ENTITY, DTO, UI_COMPONENT, CONFIG

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "package_or_namespace", length = 500)
    private String packageOrNamespace;

    @Column(name = "endpoint_count", nullable = false)
    @Builder.Default
    private Integer endpointCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_state", nullable = false, length = 50)
    @Builder.Default
    private ConfidenceState confidenceState = ConfidenceState.VERIFIED;

    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;
}
