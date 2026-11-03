package com.aiproject.architect.modules.ai.repository;

import com.aiproject.architect.modules.ai.entity.AiInteractionTrace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiInteractionTraceRepository extends JpaRepository<AiInteractionTrace, String> {
    List<AiInteractionTrace> findAllByProjectIdOrderByCreatedAtDesc(String projectId);
    List<AiInteractionTrace> findAllByProviderType(String providerType);
}
