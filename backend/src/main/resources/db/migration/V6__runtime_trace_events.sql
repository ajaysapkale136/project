CREATE TABLE runtime_trace_events (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    operation VARCHAR(500) NOT NULL,
    component_name VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    duration_ms BIGINT,
    outcome VARCHAR(50) NOT NULL,
    attributes_json TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_runtime_trace_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);
CREATE INDEX idx_runtime_trace_project_operation ON runtime_trace_events(project_id, operation, occurred_at);
