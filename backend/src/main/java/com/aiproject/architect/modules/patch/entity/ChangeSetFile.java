package com.aiproject.architect.modules.patch.entity;

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
@Table(name = "change_set_files")
public class ChangeSetFile extends BaseEntity {

    @Column(name = "change_set_id", nullable = false, length = 36)
    private String changeSetId;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "change_type", nullable = false, length = 50)
    private String changeType; // CREATE, UPDATE, DELETE

    @Column(name = "original_content", columnDefinition = "TEXT")
    private String originalContent;

    @Column(name = "proposed_content", nullable = false, columnDefinition = "TEXT")
    private String proposedContent;

    @Column(name = "diff_unified", columnDefinition = "TEXT")
    private String diffUnified;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING"; // PENDING, ACCEPTED, REJECTED

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "additions_count", nullable = false)
    @Builder.Default
    private Integer additionsCount = 0;

    @Column(name = "deletions_count", nullable = false)
    @Builder.Default
    private Integer deletionsCount = 0;
}
