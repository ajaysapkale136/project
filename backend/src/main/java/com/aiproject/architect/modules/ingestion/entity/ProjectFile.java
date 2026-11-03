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
@Table(name = "project_files")
public class ProjectFile extends BaseEntity {

    @Column(name = "snapshot_id", nullable = false, length = 36)
    private String snapshotId;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "extension", length = 50)
    private String extension;

    @Column(name = "size_bytes", nullable = false)
    @Builder.Default
    private Long sizeBytes = 0L;

    @Column(name = "sha256_hash", length = 64)
    private String sha256Hash;

    @Column(name = "is_binary", nullable = false)
    @Builder.Default
    private Boolean isBinary = false;

    @Column(name = "is_test", nullable = false)
    @Builder.Default
    private Boolean isTest = false;
}
