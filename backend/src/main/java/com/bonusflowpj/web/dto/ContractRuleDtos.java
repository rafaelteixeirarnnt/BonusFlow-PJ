package com.bonusflowpj.web.dto;

import com.bonusflowpj.domain.AbsenceType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public final class ContractRuleDtos {

    private ContractRuleDtos() {
    }

    public record CreateContractRuleRequest(
        @NotNull Long professionalId,
        @NotNull AbsenceType absenceType,
        @Min(0) int daysAllowed,
        @NotNull LocalDate validFrom,
        LocalDate validTo
    ) {
    }

    public record ContractRuleResponse(
        Long id,
        Long professionalId,
        String professionalName,
        AbsenceType absenceType,
        int daysAllowed,
        LocalDate validFrom,
        LocalDate validTo
    ) {
    }
}
