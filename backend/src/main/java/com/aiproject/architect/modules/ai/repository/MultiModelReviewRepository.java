package com.aiproject.architect.modules.ai.repository;

import com.aiproject.architect.modules.ai.entity.MultiModelReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MultiModelReviewRepository extends JpaRepository<MultiModelReview, String> {
    Optional<MultiModelReview> findTopByAnalysisIdOrderByCreatedAtDesc(String analysisId);
    List<MultiModelReview> findAllByAnalysisId(String analysisId);
}
