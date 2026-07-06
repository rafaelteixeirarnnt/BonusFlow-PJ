package com.bonusflowpj.service.dto;

import com.bonusflowpj.domain.AbsenceType;
import java.time.LocalDate;

public record CreateAbsenceRequestCommand(
    Long professionalId,
    Long createdById,
    AbsenceType absenceType,
    LocalDate startDate,
    LocalDate endDate,
    String reason
) {
}
