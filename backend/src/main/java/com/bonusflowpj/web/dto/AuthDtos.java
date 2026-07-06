package com.bonusflowpj.web.dto;

import com.bonusflowpj.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
    }

    public record LoginResponse(String token, AuthUserResponse user) {
    }

    public record AuthUserResponse(
        Long id,
        String name,
        String email,
        UserRole role,
        Long professionalId,
        boolean systemUser,
        Instant lastLoginAt
    ) {
    }
}
