package com.aiproject.architect.modules.security.hardening;

import com.aiproject.architect.common.ApiResponse;
import com.aiproject.architect.modules.security.hardening.entity.SecurityAuditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogService auditService;

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<SecurityAuditEvent>>> getAuditEvents(@PathVariable String projectId) {
        return ResponseEntity.ok(ApiResponse.ok("Audit events retrieved", auditService.getEventsForProject(projectId)));
    }
}
