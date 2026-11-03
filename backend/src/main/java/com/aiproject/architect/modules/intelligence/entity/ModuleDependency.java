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
@Table(name = "module_dependencies")
public class ModuleDependency extends BaseEntity {

    @Column(name = "analysis_id", nullable = false, length = 36)
    private String analysisId;

    @Column(name = "source_module_id", nullable = false, length = 36)
    private String sourceModuleId;

    @Column(name = "target_module_id", nullable = false, length = 36)
    private String targetModuleId;

    @Column(name = "dependency_type", nullable = false, length = 100)
    private String dependencyType; // SYNCHRONOUS_REST, DIRECT_CALL, DATABASE_SHARED, EVENT_MESSAGE

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_state", nullable = false, length = 50)
    @Builder.Default
    private ConfidenceState confidenceState = ConfidenceState.VERIFIED;
}
