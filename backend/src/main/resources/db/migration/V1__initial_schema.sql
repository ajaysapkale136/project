-- Flyway Migration V1: Initial Normalized Schema for AI Project Architect

CREATE TABLE projects (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    repository_url VARCHAR(500),
    default_branch VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE source_snapshots (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    snapshot_type VARCHAR(50) NOT NULL,
    commit_hash VARCHAR(100),
    branch VARCHAR(100),
    file_count BIGINT NOT NULL DEFAULT 0,
    total_bytes BIGINT NOT NULL DEFAULT 0,
    storage_path VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_snapshots_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);
CREATE INDEX idx_snapshots_project ON source_snapshots(project_id);

CREATE TABLE project_files (
    id VARCHAR(36) PRIMARY KEY,
    snapshot_id VARCHAR(36) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    extension VARCHAR(50),
    size_bytes BIGINT NOT NULL DEFAULT 0,
    sha256_hash VARCHAR(64),
    is_binary BOOLEAN NOT NULL DEFAULT FALSE,
    is_test BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_files_snapshot FOREIGN KEY (snapshot_id) REFERENCES source_snapshots(id) ON DELETE CASCADE
);
CREATE INDEX idx_files_snapshot ON project_files(snapshot_id);
CREATE INDEX idx_files_extension ON project_files(extension);

CREATE TABLE project_analyses (
    id VARCHAR(36) PRIMARY KEY,
    snapshot_id VARCHAR(36) NOT NULL,
    status VARCHAR(50) NOT NULL,
    primary_language VARCHAR(100),
    framework_family VARCHAR(100),
    total_files INT NOT NULL DEFAULT 0,
    total_lines_of_code BIGINT NOT NULL DEFAULT 0,
    total_controllers INT NOT NULL DEFAULT 0,
    total_endpoints INT NOT NULL DEFAULT 0,
    total_services INT NOT NULL DEFAULT 0,
    total_repositories INT NOT NULL DEFAULT 0,
    total_entities INT NOT NULL DEFAULT 0,
    summary TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT fk_analyses_snapshot FOREIGN KEY (snapshot_id) REFERENCES source_snapshots(id) ON DELETE CASCADE
);
CREATE INDEX idx_analyses_snapshot ON project_analyses(snapshot_id);

CREATE TABLE detected_technologies (
    id VARCHAR(36) PRIMARY KEY,
    analysis_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(100) NOT NULL,
    version VARCHAR(100),
    detection_source VARCHAR(255),
    confidence_state VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_technologies_analysis FOREIGN KEY (analysis_id) REFERENCES project_analyses(id) ON DELETE CASCADE
);
CREATE INDEX idx_technologies_analysis ON detected_technologies(analysis_id);

CREATE TABLE analysis_modules (
    id VARCHAR(36) PRIMARY KEY,
    analysis_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    module_type VARCHAR(100) NOT NULL,
    base_path VARCHAR(1000) NOT NULL,
    description TEXT,
    component_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_modules_analysis FOREIGN KEY (analysis_id) REFERENCES project_analyses(id) ON DELETE CASCADE
);
CREATE INDEX idx_modules_analysis ON analysis_modules(analysis_id);

CREATE TABLE analysis_components (
    id VARCHAR(36) PRIMARY KEY,
    module_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    component_type VARCHAR(100) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    package_or_namespace VARCHAR(500),
    endpoint_count INT NOT NULL DEFAULT 0,
    confidence_state VARCHAR(50) NOT NULL,
    details_json TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_components_module FOREIGN KEY (module_id) REFERENCES analysis_modules(id) ON DELETE CASCADE
);
CREATE INDEX idx_components_module ON analysis_components(module_id);
CREATE INDEX idx_components_type ON analysis_components(component_type);

CREATE TABLE module_dependencies (
    id VARCHAR(36) PRIMARY KEY,
    analysis_id VARCHAR(36) NOT NULL,
    source_module_id VARCHAR(36) NOT NULL,
    target_module_id VARCHAR(36) NOT NULL,
    dependency_type VARCHAR(100) NOT NULL,
    confidence_state VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_moddep_analysis FOREIGN KEY (analysis_id) REFERENCES project_analyses(id) ON DELETE CASCADE,
    CONSTRAINT fk_moddep_source FOREIGN KEY (source_module_id) REFERENCES analysis_modules(id) ON DELETE CASCADE,
    CONSTRAINT fk_moddep_target FOREIGN KEY (target_module_id) REFERENCES analysis_modules(id) ON DELETE CASCADE
);
CREATE INDEX idx_moddep_analysis ON module_dependencies(analysis_id);

CREATE TABLE business_rules (
    id VARCHAR(36) PRIMARY KEY,
    analysis_id VARCHAR(36) NOT NULL,
    rule_code VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(100) NOT NULL,
    confidence_state VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_rules_analysis FOREIGN KEY (analysis_id) REFERENCES project_analyses(id) ON DELETE CASCADE
);
CREATE INDEX idx_rules_analysis ON business_rules(analysis_id);

CREATE TABLE source_evidence (
    id VARCHAR(36) PRIMARY KEY,
    business_rule_id VARCHAR(36),
    component_id VARCHAR(36),
    file_path VARCHAR(1000) NOT NULL,
    start_line INT,
    end_line INT,
    code_snippet TEXT,
    verification_state VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_evidence_rule FOREIGN KEY (business_rule_id) REFERENCES business_rules(id) ON DELETE CASCADE,
    CONSTRAINT fk_evidence_comp FOREIGN KEY (component_id) REFERENCES analysis_components(id) ON DELETE CASCADE
);
CREATE INDEX idx_evidence_rule ON source_evidence(business_rule_id);
CREATE INDEX idx_evidence_comp ON source_evidence(component_id);

CREATE TABLE architecture_versions (
    id VARCHAR(36) PRIMARY KEY,
    analysis_id VARCHAR(36) NOT NULL,
    version_type VARCHAR(50) NOT NULL,
    revision_number INT NOT NULL DEFAULT 1,
    summary TEXT NOT NULL,
    anti_patterns_json TEXT,
    recommendations_json TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_archver_analysis FOREIGN KEY (analysis_id) REFERENCES project_analyses(id) ON DELETE CASCADE
);
CREATE INDEX idx_archver_analysis ON architecture_versions(analysis_id);

CREATE TABLE architecture_nodes (
    id VARCHAR(36) PRIMARY KEY,
    version_id VARCHAR(36) NOT NULL,
    node_key VARCHAR(100) NOT NULL,
    label VARCHAR(255) NOT NULL,
    node_type VARCHAR(100) NOT NULL,
    category VARCHAR(100) NOT NULL,
    pos_x INT NOT NULL DEFAULT 0,
    pos_y INT NOT NULL DEFAULT 0,
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_nodes_version FOREIGN KEY (version_id) REFERENCES architecture_versions(id) ON DELETE CASCADE
);
CREATE INDEX idx_nodes_version ON architecture_nodes(version_id);

CREATE TABLE architecture_edges (
    id VARCHAR(36) PRIMARY KEY,
    version_id VARCHAR(36) NOT NULL,
    source_node_key VARCHAR(100) NOT NULL,
    target_node_key VARCHAR(100) NOT NULL,
    label VARCHAR(255),
    edge_type VARCHAR(100) NOT NULL,
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_edges_version FOREIGN KEY (version_id) REFERENCES architecture_versions(id) ON DELETE CASCADE
);
CREATE INDEX idx_edges_version ON architecture_edges(version_id);

CREATE TABLE generation_jobs (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    job_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    progress_percent INT NOT NULL DEFAULT 0,
    current_step VARCHAR(255),
    error_message TEXT,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_jobs_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);
CREATE INDEX idx_jobs_project ON generation_jobs(project_id);
CREATE INDEX idx_jobs_status ON generation_jobs(status);

CREATE TABLE audit_logs (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36),
    action VARCHAR(100) NOT NULL,
    actor VARCHAR(100) NOT NULL,
    details_json TEXT,
    ip_address VARCHAR(50),
    timestamp TIMESTAMP NOT NULL
);
CREATE INDEX idx_audit_project ON audit_logs(project_id);
