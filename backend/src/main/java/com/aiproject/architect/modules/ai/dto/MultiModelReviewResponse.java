package com.aiproject.architect.modules.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiModelReviewResponse {
    private String reviewId;
    private String primaryModel;
    private List<String> reviewerModels;
    private List<String> agreements;
    private List<String> disagreements;
    private List<String> unsupportedClaims;
    private List<String> unresolvedRisks;
    private String synthesisSummary;
    private String consensusStatus; // CONSENSUS_REACHED, DISPUTE_FLAGGED
}
