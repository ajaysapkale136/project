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
@Table(name = "security_scans")
public class SecurityScan extends BaseEntity {

    @Column(name = "project_id", nullable = false, length = 36)
    private String projectId;

    @Column(name = "snapshot_id", length = 36)
    private String snapshotId;

    @Column(name = "scanned_files_count", nullable = false)
    @Builder.Default
    private Integer scannedFilesCount = 0;

    @Column(name = "critical_count", nullable = false)
    @Builder.Default
    private Integer criticalCount = 0;

    @Column(name = "high_count", nullable = false)
    @Builder.Default
    private Integer highCount = 0;

    @Column(name = "medium_count", nullable = false)
    @Builder.Default
    private Integer mediumCount = 0;

    @Column(name = "low_count", nullable = false)
    @Builder.Default
    private Integer lowCount = 0;

    @Column(name = "passed", nullable = false)
    @Builder.Default
    private Boolean passed = true;

    @Column(name = "scan_summary", columnDefinition = "TEXT")
    private String scanSummary;
}
