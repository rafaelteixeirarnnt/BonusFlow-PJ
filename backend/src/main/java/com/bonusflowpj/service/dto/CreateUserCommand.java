package com.bonusflowpj.service.dto;

import com.bonusflowpj.domain.UserRole;

public record CreateUserCommand(
    String name,
    String email,
    String rawPassword,
    UserRole role,
    Long professionalId,
    boolean active
) {
}
