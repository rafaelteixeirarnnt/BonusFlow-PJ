package com.bonusflowpj.web.dto;

import com.bonusflowpj.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class UserDtos {

    private UserDtos() {
    }

    public record CreateUserRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        String password,
        @NotNull UserRole role,
        Long professionalId,
        boolean active
    ) {
    }

    public record UpdateUserRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @NotNull UserRole role,
        Long professionalId,
        boolean active
    ) {
    }

    public record UserResponse(
        Long id,
        String name,
        String email,
        UserRole role,
        boolean active,
        Long professionalId,
        String professionalName,
        boolean systemUser,
        java.time.Instant createdAt,
        java.time.Instant updatedAt,
        java.time.Instant lastLoginAt
    ) {
    }
}
