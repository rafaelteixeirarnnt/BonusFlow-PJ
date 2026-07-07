package com.bonusflowpj.web.dto;

import java.time.Instant;

public record AuditLogResponse(
    Long id,
    String entityName,
    Long entityId,
    String action,
    String previousValue,
    String newValue,
    String justification,
    Long performedByUserId,
    String performedByUserName,
    Instant performedAt,
    String ipAddress
) {
}
