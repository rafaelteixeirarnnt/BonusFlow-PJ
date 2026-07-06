package com.bonusflowpj.service.dto;

import com.bonusflowpj.domain.AbsenceType;

public record BalanceSummary(
    Long professionalId,
    AbsenceType absenceType,
    int daysAllowed,
    int approvedDays,
    int availableDays
) {
}
