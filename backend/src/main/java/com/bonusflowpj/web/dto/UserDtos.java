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
        @NotNull UserRole role,
        boolean active
    ) {
    }

    public record UserResponse(Long id, String name, String email, UserRole role, boolean active) {
    }
}
