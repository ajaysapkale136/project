package com.aiproject.architect.modules.assistant.entity;

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
@Table(name = "code_changes")
public class CodeChange extends BaseEntity {

    @Column(name = "proposal_id", nullable = false, length = 36)
    private String proposalId;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "change_type", nullable = false, length = 50)
    private String changeType; // CREATE, MODIFY, DELETE

    @Column(name = "original_content", columnDefinition = "TEXT")
    private String originalContent;

    @Column(name = "proposed_content", nullable = false, columnDefinition = "TEXT")
    private String proposedContent;

    @Column(name = "diff_unified", columnDefinition = "TEXT")
    private String diffUnified;

    @Column(name = "additions_count", nullable = false)
    @Builder.Default
    private Integer additionsCount = 0;

    @Column(name = "deletions_count", nullable = false)
    @Builder.Default
    private Integer deletionsCount = 0;

    @Column(name = "business_rules_preserved", columnDefinition = "TEXT")
    private String businessRulesPreserved;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED
}
