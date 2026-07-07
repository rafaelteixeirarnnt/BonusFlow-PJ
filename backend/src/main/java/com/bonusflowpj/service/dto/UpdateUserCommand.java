package com.bonusflowpj.service.dto;

import com.bonusflowpj.domain.UserRole;
import com.bonusflowpj.web.dto.UserDtos.AddressRequest;
import com.bonusflowpj.web.dto.UserDtos.ContactRequest;
import java.time.LocalDate;
import java.util.List;

public record UpdateUserCommand(
    String name,
    String cpf,
    LocalDate birthDate,
    String motherName,
    String fatherName,
    String email,
    UserRole role,
    Long professionalId,
    boolean active,
    List<ContactRequest> contacts,
    AddressRequest address,
    String justification,
    Long performedByUserId,
    String ipAddress
) {

    public UpdateUserCommand(
        String name,
        String cpf,
        LocalDate birthDate,
        String motherName,
        String fatherName,
        String email,
        UserRole role,
        Long professionalId,
        boolean active,
        List<ContactRequest> contacts,
        AddressRequest address
    ) {
        this(name, cpf, birthDate, motherName, fatherName, email, role, professionalId, active, contacts, address, "Atualizacao administrativa", null, null);
    }

    public UpdateUserCommand(String name, String email, UserRole role, Long professionalId, boolean active) {
        this(name, null, null, null, null, email, role, professionalId, active, null, null, "Atualizacao administrativa", null, null);
    }

    public UpdateUserCommand(
        String name,
        String email,
        UserRole role,
        Long professionalId,
        boolean active,
        String justification,
        Long performedByUserId,
        String ipAddress
    ) {
        this(name, null, null, null, null, email, role, professionalId, active, null, null, justification, performedByUserId, ipAddress);
    }
}
