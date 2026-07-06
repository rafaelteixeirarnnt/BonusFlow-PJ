package com.bonusflowpj.web.dto;

import com.bonusflowpj.domain.AbsenceStatus;
import java.time.Instant;

public record ApprovalHistoryResponse(
    Long id,
    Long absenceRequestId,
    Long changedById,
    String changedByName,
    AbsenceStatus fromStatus,
    AbsenceStatus toStatus,
    String comment,
    Instant changedAt
) {
}
