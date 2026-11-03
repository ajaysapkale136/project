-- Flyway Migration V4: Isolated Worktree, Patch Change-Sets, Testing Engine & Audit Model

CREATE TABLE isolated_workspaces (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    snapshot_id VARCHAR(36) NOT NULL,
    workspace_path VARCHAR(1000) NOT NULL,
    status VARCHAR(50) NOT NULL, -- ACTIVE, COMMITTED, ROLLED_BACK, STALE
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_workspaces_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);
CREATE INDEX idx_workspaces_project ON isolated_workspaces(project_id);

CREATE TABLE change_sets (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    workspace_id VARCHAR(36) NOT NULL,
    source_snapshot_id VARCHAR(36) NOT NULL,
    architecture_version_id VARCHAR(36),
    target_type VARCHAR(50) NOT NULL, -- MODULE, COMPONENT, BUG, FEATURE, REFACTOR, API_OPERATION
    target_identifier VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    technical_plan_json TEXT NOT NULL,
    status VARCHAR(50) NOT NULL, -- PENDING_APPROVAL, APPROVED, REJECTED, APPLIED, ROLLED_BACK, STALE_CONFLICT
    risk_level VARCHAR(50) NOT NULL DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, CRITICAL
    business_rule_impact_json TEXT,
    api_compatibility_impact_json TEXT,
    migration_impact_json TEXT,
    rollback_instructions TEXT,
    applied_at TIMESTAMP,
    rolled_back_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_changesets_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_changesets_workspace FOREIGN KEY (workspace_id) REFERENCES isolated_workspaces(id) ON DELETE CASCADE
);
CREATE INDEX idx_changesets_project ON change_sets(project_id);
CREATE INDEX idx_changesets_workspace ON change_sets(workspace_id);

CREATE TABLE change_set_files (
    id VARCHAR(36) PRIMARY KEY,
    change_set_id VARCHAR(36) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    change_type VARCHAR(50) NOT NULL, -- CREATE, UPDATE, DELETE
    original_content TEXT,
    proposed_content TEXT NOT NULL,
    diff_unified TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, ACCEPTED, REJECTED
    explanation TEXT,
    additions_count INT NOT NULL DEFAULT 0,
    deletions_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_csfiles_changeset FOREIGN KEY (change_set_id) REFERENCES change_sets(id) ON DELETE CASCADE
);
CREATE INDEX idx_csfiles_changeset ON change_set_files(change_set_id);

CREATE TABLE test_executions (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    change_set_id VARCHAR(36),
    workspace_id VARCHAR(36),
    command_executed VARCHAR(500) NOT NULL,
    exit_code INT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL, -- PASSED, FAILED, TIMEOUT, ERROR, UNAVAILABLE
    passed_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    skipped_count INT NOT NULL DEFAULT 0,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    console_output TEXT,
    test_type VARCHAR(50) NOT NULL DEFAULT 'ALL', -- UNIT, INTEGRATION, REGRESSION, SECURITY, ALL
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_testexec_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);
CREATE INDEX idx_testexec_project ON test_executions(project_id);

CREATE TABLE security_audit_events (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    correlation_id VARCHAR(100),
    event_type VARCHAR(50) NOT NULL, -- IMPORT, ANALYSIS, PROPOSAL_GEN, CHANGE_APPROVAL, CHANGE_APPLIED, ROLLBACK, EXPORT, SECURITY_ALERT
    actor VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    details_json TEXT,
    ip_address VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_audit_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);
CREATE INDEX idx_sec_audit_project ON security_audit_events(project_id);
CREATE INDEX idx_sec_audit_correlation ON security_audit_events(correlation_id);
