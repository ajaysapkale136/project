package com.aiproject.architect.modules.architect.entity;

import com.aiproject.architect.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "architecture_edges")
public class ArchitectureEdge extends BaseEntity {

    @Column(name = "version_id", nullable = false, length = 36)
    private String versionId;

    @Column(name = "source_node_key", nullable = false, length = 100)
    private String sourceNodeKey;

    @Column(name = "target_node_key", nullable = false, length = 100)
    private String targetNodeKey;

    @Column(name = "label", length = 255)
    private String label;

    @Column(name = "edge_type", nullable = false, length = 100)
    private String edgeType; // REST_HTTP, METHOD_CALL, JPA_QUERY, ASYNC_EVENT

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;
}
