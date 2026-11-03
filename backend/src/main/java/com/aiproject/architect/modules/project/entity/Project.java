package com.aiproject.architect.modules.project.entity;

import com.aiproject.architect.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "projects")
public class Project extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "repository_url", length = 500)
    private String repositoryUrl;

    @Column(name = "default_branch", length = 100)
    private String defaultBranch;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "CREATED"; // CREATED, INGESTED, ANALYZED, ARCHITECTED, ERROR
}
