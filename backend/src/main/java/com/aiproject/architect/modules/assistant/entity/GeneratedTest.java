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
@Table(name = "generated_tests")
public class GeneratedTest extends BaseEntity {

    @Column(name = "proposal_id", length = 36)
    private String proposalId;

    @Column(name = "project_id", nullable = false, length = 36)
    private String projectId;

    @Column(name = "target_file_path", nullable = false, length = 500)
    private String targetFilePath;

    @Column(name = "test_file_path", nullable = false, length = 500)
    private String testFilePath;

    @Column(name = "test_framework", nullable = false, length = 50)
    private String testFramework; // JUNIT5_MOCKITO, JEST_RTL, PYTEST

    @Column(name = "test_code", nullable = false, columnDefinition = "TEXT")
    private String testCode;

    @Column(name = "coverage_target", length = 255)
    private String coverageTarget;
}
