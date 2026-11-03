package com.aiproject.architect.modules.assistant.repository;

import com.aiproject.architect.modules.assistant.entity.GeneratedTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeneratedTestRepository extends JpaRepository<GeneratedTest, String> {
    List<GeneratedTest> findByProjectIdOrderByCreatedAtDesc(String projectId);
    List<GeneratedTest> findByProposalId(String proposalId);
}
