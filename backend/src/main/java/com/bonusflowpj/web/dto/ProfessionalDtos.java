package com.bonusflowpj.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class ProfessionalDtos {

    private ProfessionalDtos() {
    }

    public record CreateProfessionalRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @NotBlank String document,
        @NotBlank String team,
        boolean active
    ) {
    }

    public record UpdateProfessionalRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @NotBlank String document,
        @NotBlank String team,
        boolean active,
        String justification
    ) {
    }

    public record ProfessionalStatusRequest(String justification) {
    }

    public record ProfessionalResponse(
        Long id,
        String name,
        String email,
        String document,
        String team,
        boolean active,
        java.time.Instant createdAt,
        java.time.Instant updatedAt
    ) {
    }
}
