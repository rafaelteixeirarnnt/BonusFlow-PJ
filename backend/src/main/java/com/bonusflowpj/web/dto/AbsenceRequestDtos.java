package com.bonusflowpj.web.dto;

import com.bonusflowpj.domain.AbsenceStatus;
import com.bonusflowpj.domain.AbsenceType;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;

public final class AbsenceRequestDtos {

    private AbsenceRequestDtos() {
    }

    public record CreateAbsenceRequest(
        @NotNull Long professionalId,
        @NotNull Long createdById,
        @NotNull AbsenceType absenceType,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String reason
    ) {
    }

    public record TransitionRequest(@NotNull Long userId, String comment) {
    }

    public record AbsenceRequestResponse(
        Long id,
        Long professionalId,
        String professionalName,
        Long createdById,
        String createdByName,
        AbsenceType absenceType,
        LocalDate startDate,
        LocalDate endDate,
        int requestedDays,
        AbsenceStatus status,
        String reason,
        Instant createdAt
    ) {
    }
}
