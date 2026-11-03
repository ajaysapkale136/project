package com.aiproject.architect.modules.security.repository;

import com.aiproject.architect.modules.security.entity.ContractVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractVerificationRepository extends JpaRepository<ContractVerification, String> {
    List<ContractVerification> findByProjectIdOrderByCreatedAtDesc(String projectId);
}
