package com.aiproject.architect.modules.security.hardening;

import com.aiproject.architect.config.CorrelationIdFilter;
import com.aiproject.architect.modules.security.hardening.entity.SecurityAuditEvent;
import com.aiproject.architect.modules.security.hardening.repository.SecurityAuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final SecurityAuditEventRepository auditRepository;

    @Transactional
    public SecurityAuditEvent logEvent(String projectId, String eventType, String actor, String detailsJson) {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);

        SecurityAuditEvent event = SecurityAuditEvent.builder()
                .projectId(projectId)
                .correlationId(correlationId)
                .eventType(eventType)
                .actor(actor != null ? actor : "SYSTEM")
                .detailsJson(detailsJson)
                .build();

        log.info("[AUDIT] Event='{}' Project='{}' Actor='{}' CorrelationId='{}'",
                eventType, projectId, actor, correlationId);

        return auditRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<SecurityAuditEvent> getEventsForProject(String projectId) {
        return auditRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }
}
