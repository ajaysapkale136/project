-- Flyway Migration V3: AI Development Assistant and Security Validation Sandbox Model

CREATE TABLE code_proposals (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    analysis_id VARCHAR(36),
    module_name VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL, -- DRAFT, PENDING_REVIEW, APPROVED, REJECTED, APPLIED
    target_language VARCHAR(50) NOT NULL, -- JAVA, TYPESCRIPT, PYTHON, SQL
    prompt_used TEXT,
    applied_at TIMESTAMP,
    applied_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_proposals_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);
CREATE INDEX idx_proposals_project ON code_proposals(project_id);

CREATE TABLE code_changes (
    id VARCHAR(36) PRIMARY KEY,
    proposal_id VARCHAR(36) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    change_type VARCHAR(50) NOT NULL, -- CREATE, MODIFY, DELETE
    original_content TEXT,
    proposed_content TEXT NOT NULL,
    diff_unified TEXT,
    additions_count INT NOT NULL DEFAULT 0,
    deletions_count INT NOT NULL DEFAULT 0,
    business_rules_preserved TEXT,
    status VARCHAR(50) NOT NULL, -- PENDING, APPROVED, REJECTED
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_changes_proposal FOREIGN KEY (proposal_id) REFERENCES code_proposals(id) ON DELETE CASCADE
);
CREATE INDEX idx_changes_proposal ON code_changes(proposal_id);

CREATE TABLE generated_tests (
    id VARCHAR(36) PRIMARY KEY,
    proposal_id VARCHAR(36),
    project_id VARCHAR(36) NOT NULL,
    target_file_path VARCHAR(500) NOT NULL,
    test_file_path VARCHAR(500) NOT NULL,
    test_framework VARCHAR(50) NOT NULL, -- JUNIT5_MOCKITO, JEST_RTL, PYTEST
    test_code TEXT NOT NULL,
    coverage_target VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_tests_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);
CREATE INDEX idx_tests_project ON generated_tests(project_id);

CREATE TABLE security_scans (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    snapshot_id VARCHAR(36),
    scanned_files_count INT NOT NULL DEFAULT 0,
    critical_count INT NOT NULL DEFAULT 0,
    high_count INT NOT NULL DEFAULT 0,
    medium_count INT NOT NULL DEFAULT 0,
    low_count INT NOT NULL DEFAULT 0,
    passed BOOLEAN NOT NULL DEFAULT TRUE,
    scan_summary TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_scans_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);
CREATE INDEX idx_scans_project ON security_scans(project_id);

CREATE TABLE vulnerability_findings (
    id VARCHAR(36) PRIMARY KEY,
    scan_id VARCHAR(36) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    line_number INT NOT NULL DEFAULT 1,
    rule_id VARCHAR(100) NOT NULL,
    severity VARCHAR(50) NOT NULL, -- CRITICAL, HIGH, MEDIUM, LOW
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    code_snippet TEXT,
    remediation_recommendation TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_findings_scan FOREIGN KEY (scan_id) REFERENCES security_scans(id) ON DELETE CASCADE
);
CREATE INDEX idx_findings_scan ON vulnerability_findings(scan_id);

CREATE TABLE contract_verifications (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    snapshot_id VARCHAR(36),
    endpoint_method VARCHAR(10) NOT NULL,
    endpoint_path VARCHAR(255) NOT NULL,
    expected_contract TEXT,
    actual_contract TEXT,
    compliance_status VARCHAR(50) NOT NULL, -- COMPLIANT, BREAKING_CHANGE, MISSING_FIELD, TYPE_MISMATCH
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_contracts_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);
CREATE INDEX idx_contracts_project ON contract_verifications(project_id);
