package com.aiproject.architect.modules.anatomy.entity;

import com.aiproject.architect.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "runtime_trace_events")
public class RuntimeTraceEvent extends BaseEntity {
    @Column(name = "project_id", nullable = false, length = 36) private String projectId;
    @Column(nullable = false, length = 500) private String operation;
    @Column(name = "component_name", nullable = false) private String componentName;
    @Column(name = "occurred_at", nullable = false) private LocalDateTime occurredAt;
    @Column(name = "duration_ms") private Long durationMs;
    @Column(nullable = false, length = 50) private String outcome;
    @Column(name = "attributes_json", columnDefinition = "TEXT") private String attributesJson;
}
