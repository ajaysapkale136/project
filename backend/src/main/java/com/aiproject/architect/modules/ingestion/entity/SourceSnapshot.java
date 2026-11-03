package com.aiproject.architect.modules.ingestion.entity;

import com.aiproject.architect.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "source_snapshots")
public class SourceSnapshot extends BaseEntity {

    @Column(name = "project_id", nullable = false, length = 36)
    private String projectId;

    @Column(name = "snapshot_type", nullable = false, length = 50)
    private String snapshotType; // ZIP_UPLOAD, GITHUB_IMPORT, WORKSPACE

    @Column(name = "commit_hash", length = 100)
    private String commitHash;

    @Column(name = "branch", length = 100)
    private String branch;

    @Column(name = "file_count", nullable = false)
    @Builder.Default
    private Long fileCount = 0L;

    @Column(name = "total_bytes", nullable = false)
    @Builder.Default
    private Long totalBytes = 0L;

    @Column(name = "storage_path", nullable = false, length = 1000)
    private String storagePath;
}
