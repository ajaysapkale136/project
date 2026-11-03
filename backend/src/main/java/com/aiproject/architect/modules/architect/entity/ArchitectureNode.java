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
@Table(name = "architecture_nodes")
public class ArchitectureNode extends BaseEntity {

    @Column(name = "version_id", nullable = false, length = 36)
    private String versionId;

    @Column(name = "node_key", nullable = false, length = 100)
    private String nodeKey;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "node_type", nullable = false, length = 100)
    private String nodeType; // CLIENT, GATEWAY, CONTROLLER, SERVICE, REPOSITORY, DATABASE, QUEUE, EXTERNAL

    @Column(name = "category", nullable = false, length = 100)
    private String category; // FRONTEND, BACKEND, PERSISTENCE, INFRASTRUCTURE

    @Column(name = "pos_x", nullable = false)
    @Builder.Default
    private Integer posX = 0;

    @Column(name = "pos_y", nullable = false)
    @Builder.Default
    private Integer posY = 0;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;
}
