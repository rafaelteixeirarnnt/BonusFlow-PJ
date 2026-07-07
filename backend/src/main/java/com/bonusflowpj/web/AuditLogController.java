package com.bonusflowpj.web;

import com.bonusflowpj.domain.AuditLog;
import com.bonusflowpj.service.AuditLogService;
import com.bonusflowpj.web.dto.AuditLogResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/search")
    public List<AuditLogResponse> search(
        @RequestParam(required = false) String entityName,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) Long performedByUserId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startAt,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endAt
    ) {
        return auditLogService.search(entityName, action, performedByUserId, startAt, endAt)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @GetMapping("/{id}")
    public AuditLogResponse get(@PathVariable Long id) {
        return toResponse(auditLogService.find(id));
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
            auditLog.getId(),
            auditLog.getEntityName(),
            auditLog.getEntityId(),
            auditLog.getAction(),
            auditLog.getPreviousValue(),
            auditLog.getNewValue(),
            auditLog.getJustification(),
            auditLog.getPerformedByUserId(),
            auditLog.getPerformedByUserName(),
            auditLog.getPerformedAt(),
            auditLog.getIpAddress()
        );
    }
}
