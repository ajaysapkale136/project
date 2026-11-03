package com.aiproject.architect.modules.intelligence.entity;

import com.aiproject.architect.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "project_analyses")
public class ProjectAnalysis extends BaseEntity {

    @Column(name = "snapshot_id", nullable = false, length = 36)
    private String snapshotId;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "ANALYZING"; // ANALYZING, COMPLETED, FAILED

    @Column(name = "primary_language", length = 100)
    private String primaryLanguage;

    @Column(name = "framework_family", length = 100)
    private String frameworkFamily;

    @Column(name = "total_files", nullable = false)
    @Builder.Default
    private Integer totalFiles = 0;

    @Column(name = "total_lines_of_code", nullable = false)
    @Builder.Default
    private Long totalLinesOfCode = 0L;

    @Column(name = "total_controllers", nullable = false)
    @Builder.Default
    private Integer totalControllers = 0;

    @Column(name = "total_endpoints", nullable = false)
    @Builder.Default
    private Integer totalEndpoints = 0;

    @Column(name = "total_services", nullable = false)
    @Builder.Default
    private Integer totalServices = 0;

    @Column(name = "total_repositories", nullable = false)
    @Builder.Default
    private Integer totalRepositories = 0;

    @Column(name = "total_entities", nullable = false)
    @Builder.Default
    private Integer totalEntities = 0;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
