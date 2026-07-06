package com.bonusflowpj.service.dto;

import com.bonusflowpj.domain.UserRole;

public record UpdateUserCommand(
    String name,
    String email,
    UserRole role,
    Long professionalId,
    boolean active
) {
}
