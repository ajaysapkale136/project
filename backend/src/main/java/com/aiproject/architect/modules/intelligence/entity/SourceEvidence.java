package com.aiproject.architect.modules.intelligence.entity;

import com.aiproject.architect.common.BaseEntity;
import com.aiproject.architect.common.ConfidenceState;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "source_evidence")
public class SourceEvidence extends BaseEntity {

    @Column(name = "business_rule_id", length = 36)
    private String businessRuleId;

    @Column(name = "component_id", length = 36)
    private String componentId;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "start_line")
    private Integer startLine;

    @Column(name = "end_line")
    private Integer endLine;

    @Column(name = "code_snippet", columnDefinition = "TEXT")
    private String codeSnippet;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_state", nullable = false, length = 50)
    @Builder.Default
    private ConfidenceState verificationState = ConfidenceState.VERIFIED;
}
