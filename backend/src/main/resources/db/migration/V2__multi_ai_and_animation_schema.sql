-- Flyway Migration V2: Multi-AI Intelligence Tracing and Animation Model

CREATE TABLE ai_provider_configs (
    id VARCHAR(36) PRIMARY KEY,
    provider_type VARCHAR(50) NOT NULL, -- OPENAI, GEMINI, CLAUDE
    model_name VARCHAR(100) NOT NULL,
    api_endpoint VARCHAR(500),
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    cost_per_1k_input DECIMAL(10, 6) NOT NULL DEFAULT 0.0,
    cost_per_1k_output DECIMAL(10, 6) NOT NULL DEFAULT 0.0,
    max_tokens INT NOT NULL DEFAULT 4096,
    timeout_seconds INT NOT NULL DEFAULT 30,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE ai_interaction_traces (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    snapshot_id VARCHAR(36),
    provider_type VARCHAR(50) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    task_type VARCHAR(100) NOT NULL, -- ARCHITECTURE_SYNTHESIS, CODE_REVIEW, CONSENSUS_SYNTHESIS
    prompt_version VARCHAR(50) NOT NULL,
    input_tokens INT NOT NULL DEFAULT 0,
    output_tokens INT NOT NULL DEFAULT 0,
    estimated_cost_usd DECIMAL(10, 6) NOT NULL DEFAULT 0.0,
    execution_duration_ms BIGINT NOT NULL DEFAULT 0,
    validation_state VARCHAR(50) NOT NULL, -- VALID, INVALID_SCHEMA, TIMEOUT, RATE_LIMITED
    request_payload_json TEXT,
    response_payload_json TEXT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_traces_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);
CREATE INDEX idx_traces_project ON ai_interaction_traces(project_id);
CREATE INDEX idx_traces_provider ON ai_interaction_traces(provider_type);

CREATE TABLE multi_model_reviews (
    id VARCHAR(36) PRIMARY KEY,
    analysis_id VARCHAR(36) NOT NULL,
    primary_version_id VARCHAR(36) NOT NULL,
    primary_model VARCHAR(100) NOT NULL,
    reviewer_models_json TEXT NOT NULL,
    agreements_json TEXT NOT NULL,
    disagreements_json TEXT NOT NULL,
    unsupported_claims_json TEXT NOT NULL,
    unresolved_risks_json TEXT NOT NULL,
    synthesis_summary TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_reviews_analysis FOREIGN KEY (analysis_id) REFERENCES project_analyses(id) ON DELETE CASCADE
);
CREATE INDEX idx_reviews_analysis ON multi_model_reviews(analysis_id);

CREATE TABLE animation_scenes (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    analysis_id VARCHAR(36) NOT NULL,
    animation_format VARCHAR(50) NOT NULL, -- MODULE_TO_MODULE, INTERNAL_EXPLODED, EXECUTION_TRACE
    target_identifier VARCHAR(255) NOT NULL, -- e.g. moduleId or operation path
    title VARCHAR(255) NOT NULL,
    total_steps INT NOT NULL DEFAULT 0,
    total_duration_ms INT NOT NULL DEFAULT 10000,
    scene_model_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_scenes_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);
CREATE INDEX idx_scenes_project ON animation_scenes(project_id);
