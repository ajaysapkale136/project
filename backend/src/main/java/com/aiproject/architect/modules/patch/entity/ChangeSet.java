package com.aiproject.architect.modules.patch.entity;

import com.aiproject.architect.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "change_sets")
public class ChangeSet extends BaseEntity {

    @Column(name = "project_id", nullable = false, length = 36)
    private String projectId;

    @Column(name = "workspace_id", nullable = false, length = 36)
    private String workspaceId;

    @Column(name = "source_snapshot_id", nullable = false, length = 36)
    private String sourceSnapshotId;

    @Column(name = "architecture_version_id", length = 36)
    private String architectureVersionId;

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType; // MODULE, COMPONENT, BUG, FEATURE, REFACTOR, API_OPERATION

    @Column(name = "target_identifier", nullable = false)
    private String targetIdentifier;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "technical_plan_json", nullable = false, columnDefinition = "TEXT")
    private String technicalPlanJson;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING_APPROVAL"; // PENDING_APPROVAL, APPROVED, REJECTED, APPLIED, ROLLED_BACK, STALE_CONFLICT

    @Column(name = "risk_level", nullable = false, length = 50)
    @Builder.Default
    private String riskLevel = "MEDIUM"; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "business_rule_impact_json", columnDefinition = "TEXT")
    private String businessRuleImpactJson;

    @Column(name = "api_compatibility_impact_json", columnDefinition = "TEXT")
    private String apiCompatibilityImpactJson;

    @Column(name = "migration_impact_json", columnDefinition = "TEXT")
    private String migrationImpactJson;

    @Column(name = "rollback_instructions", columnDefinition = "TEXT")
    private String rollbackInstructions;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "rolled_back_at")
    private LocalDateTime rolledBackAt;
}
