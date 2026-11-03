package com.aiproject.architect.modules.architect.entity;

import com.aiproject.architect.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "architecture_versions")
public class ArchitectureVersion extends BaseEntity {

    @Column(name = "analysis_id", nullable = false, length = 36)
    private String analysisId;

    @Column(name = "version_type", nullable = false, length = 50)
    private String versionType; // EXISTING, PROPOSED

    @Column(name = "revision_number", nullable = false)
    @Builder.Default
    private Integer revisionNumber = 1;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "anti_patterns_json", columnDefinition = "TEXT")
    private String antiPatternsJson;

    @Column(name = "recommendations_json", columnDefinition = "TEXT")
    private String recommendationsJson;
}
