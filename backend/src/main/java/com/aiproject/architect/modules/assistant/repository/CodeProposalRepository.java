package com.aiproject.architect.modules.assistant.repository;

import com.aiproject.architect.modules.assistant.entity.CodeProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeProposalRepository extends JpaRepository<CodeProposal, String> {
    List<CodeProposal> findByProjectIdOrderByCreatedAtDesc(String projectId);
    List<CodeProposal> findByProjectIdAndStatus(String projectId, String status);
}
