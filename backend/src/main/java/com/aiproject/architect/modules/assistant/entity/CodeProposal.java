package com.aiproject.architect.modules.assistant.entity;

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
@Table(name = "code_proposals")
public class CodeProposal extends BaseEntity {

    @Column(name = "project_id", nullable = false, length = 36)
    private String projectId;

    @Column(name = "analysis_id", length = 36)
    private String analysisId;

    @Column(name = "module_name", nullable = false, length = 100)
    private String moduleName;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING_REVIEW"; // DRAFT, PENDING_REVIEW, APPROVED, REJECTED, APPLIED

    @Column(name = "target_language", nullable = false, length = 50)
    private String targetLanguage; // JAVA, TYPESCRIPT, PYTHON, SQL

    @Column(name = "prompt_used", columnDefinition = "TEXT")
    private String promptUsed;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "applied_by", length = 100)
    private String appliedBy;
}
