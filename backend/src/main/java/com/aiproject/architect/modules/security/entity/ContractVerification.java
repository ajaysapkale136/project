package com.aiproject.architect.modules.security.entity;

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
@Table(name = "contract_verifications")
public class ContractVerification extends BaseEntity {

    @Column(name = "project_id", nullable = false, length = 36)
    private String projectId;

    @Column(name = "snapshot_id", length = 36)
    private String snapshotId;

    @Column(name = "endpoint_method", nullable = false, length = 10)
    private String endpointMethod; // GET, POST, PUT, DELETE, PATCH

    @Column(name = "endpoint_path", nullable = false, length = 255)
    private String endpointPath;

    @Column(name = "expected_contract", columnDefinition = "TEXT")
    private String expectedContract;

    @Column(name = "actual_contract", columnDefinition = "TEXT")
    private String actualContract;

    @Column(name = "compliance_status", nullable = false, length = 50)
    private String complianceStatus; // COMPLIANT, BREAKING_CHANGE, MISSING_FIELD, TYPE_MISMATCH

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
