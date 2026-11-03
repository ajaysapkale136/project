package com.aiproject.architect.modules.intelligence.entity;

import com.aiproject.architect.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "analysis_modules")
public class AnalysisModule extends BaseEntity {

    @Column(name = "analysis_id", nullable = false, length = 36)
    private String analysisId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "module_type", nullable = false, length = 100)
    private String moduleType; // BACKEND_SERVICE, FRONTEND_SPA, SHARED_LIB, INFRASTRUCTURE

    @Column(name = "base_path", nullable = false, length = 1000)
    private String basePath;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "component_count", nullable = false)
    @Builder.Default
    private Integer componentCount = 0;
}
