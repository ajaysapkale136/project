package com.aiproject.architect.modules.ai.entity;

import com.aiproject.architect.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "multi_model_reviews")
public class MultiModelReview extends BaseEntity {

    @Column(name = "analysis_id", nullable = false, length = 36)
    private String analysisId;

    @Column(name = "primary_version_id", nullable = false, length = 36)
    private String primaryVersionId;

    @Column(name = "primary_model", nullable = false, length = 100)
    private String primaryModel;

    @Column(name = "reviewer_models_json", nullable = false, columnDefinition = "TEXT")
    private String reviewerModelsJson;

    @Column(name = "agreements_json", nullable = false, columnDefinition = "TEXT")
    private String agreementsJson;

    @Column(name = "disagreements_json", nullable = false, columnDefinition = "TEXT")
    private String disagreementsJson;

    @Column(name = "unsupported_claims_json", nullable = false, columnDefinition = "TEXT")
    private String unsupportedClaimsJson;

    @Column(name = "unresolved_risks_json", nullable = false, columnDefinition = "TEXT")
    private String unresolvedRisksJson;

    @Column(name = "synthesis_summary", nullable = false, columnDefinition = "TEXT")
    private String synthesisSummary;
}
