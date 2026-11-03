package com.aiproject.architect.modules.security.hardening.entity;

import com.aiproject.architect.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "security_audit_events")
public class SecurityAuditEvent extends BaseEntity {

    @Column(name = "project_id", nullable = false, length = 36)
    private String projectId;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType; // IMPORT, ANALYSIS, PROPOSAL_GEN, CHANGE_APPROVAL, CHANGE_APPLIED, ROLLBACK, EXPORT, SECURITY_ALERT

    @Column(name = "actor", nullable = false, length = 100)
    @Builder.Default
    private String actor = "SYSTEM";

    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;
}
