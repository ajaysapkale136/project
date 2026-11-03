package com.aiproject.architect.modules.security.hardening.repository;

import com.aiproject.architect.modules.security.hardening.entity.SecurityAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SecurityAuditEventRepository extends JpaRepository<SecurityAuditEvent, String> {
    List<SecurityAuditEvent> findByProjectIdOrderByCreatedAtDesc(String projectId);
    List<SecurityAuditEvent> findByCorrelationIdOrderByCreatedAtDesc(String correlationId);
}
