package com.aiproject.architect.modules.security.repository;

import com.aiproject.architect.modules.security.entity.SecurityScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SecurityScanRepository extends JpaRepository<SecurityScan, String> {
    List<SecurityScan> findByProjectIdOrderByCreatedAtDesc(String projectId);
    Optional<SecurityScan> findTopByProjectIdOrderByCreatedAtDesc(String projectId);
}
