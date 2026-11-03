package com.aiproject.architect.modules.testing.entity;

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
@Table(name = "test_executions")
public class TestExecution extends BaseEntity {

    @Column(name = "project_id", nullable = false, length = 36)
    private String projectId;

    @Column(name = "change_set_id", length = 36)
    private String changeSetId;

    @Column(name = "workspace_id", length = 36)
    private String workspaceId;

    @Column(name = "command_executed", nullable = false, length = 500)
    private String commandExecuted;

    @Column(name = "exit_code", nullable = false)
    @Builder.Default
    private Integer exitCode = 0;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // PASSED, FAILED, TIMEOUT, ERROR, UNAVAILABLE

    @Column(name = "passed_count", nullable = false)
    @Builder.Default
    private Integer passedCount = 0;

    @Column(name = "failed_count", nullable = false)
    @Builder.Default
    private Integer failedCount = 0;

    @Column(name = "skipped_count", nullable = false)
    @Builder.Default
    private Integer skippedCount = 0;

    @Column(name = "duration_ms", nullable = false)
    @Builder.Default
    private Long durationMs = 0L;

    @Column(name = "console_output", columnDefinition = "TEXT")
    private String consoleOutput;

    @Column(name = "test_type", nullable = false, length = 50)
    @Builder.Default
    private String testType = "ALL"; // UNIT, INTEGRATION, REGRESSION, SECURITY, ALL
}
