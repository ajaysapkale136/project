package com.aiproject.architect.modules.assistant.repository;

import com.aiproject.architect.modules.assistant.entity.CodeChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeChangeRepository extends JpaRepository<CodeChange, String> {
    List<CodeChange> findByProposalId(String proposalId);
}
