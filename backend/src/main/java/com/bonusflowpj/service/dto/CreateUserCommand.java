package com.bonusflowpj.service.dto;

import com.bonusflowpj.domain.UserRole;
import com.bonusflowpj.web.dto.UserDtos.AddressRequest;
import com.bonusflowpj.web.dto.UserDtos.ContactRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public record CreateUserCommand(
    String name,
    String cpf,
    LocalDate birthDate,
    String motherName,
    String fatherName,
    String email,
    String rawPassword,
    UserRole role,
    Long professionalId,
    boolean active,
    List<ContactRequest> contacts,
    AddressRequest address,
    Long performedByUserId,
    String ipAddress
) {

    private static final AtomicLong CPF_SEQUENCE = new AtomicLong(100_000_000L);

    public CreateUserCommand(
        String name,
        String cpf,
        LocalDate birthDate,
        String motherName,
        String fatherName,
        String email,
        String rawPassword,
        UserRole role,
        Long professionalId,
        boolean active,
        List<ContactRequest> contacts,
        AddressRequest address
    ) {
        this(name, cpf, birthDate, motherName, fatherName, email, rawPassword, role, professionalId, active, contacts, address, null, null);
    }

    public CreateUserCommand(String name, String email, String rawPassword, UserRole role, Long professionalId, boolean active) {
        this(
            name,
            generatedCpf(),
            LocalDate.of(1990, 1, 1),
            "Mae nao informada",
            null,
            email,
            rawPassword,
            role,
            professionalId,
            active,
            List.of(new ContactRequest(com.bonusflowpj.domain.ContactType.MOBILE, "+55", "11", "999999999")),
            new AddressRequest("01001000", "Rua Teste", "1", null, "Centro", "Sao Paulo", "SP"),
            null,
            null
        );
    }

    public CreateUserCommand(
        String name,
        String email,
        String rawPassword,
        UserRole role,
        Long professionalId,
        boolean active,
        Long performedByUserId,
        String ipAddress
    ) {
        this(
            name,
            generatedCpf(),
            LocalDate.of(1990, 1, 1),
            "Mae nao informada",
            null,
            email,
            rawPassword,
            role,
            professionalId,
            active,
            List.of(new ContactRequest(com.bonusflowpj.domain.ContactType.MOBILE, "+55", "11", "999999999")),
            new AddressRequest("01001000", "Rua Teste", "1", null, "Centro", "Sao Paulo", "SP"),
            performedByUserId,
            ipAddress
        );
    }

    private static String generatedCpf() {
        String base = String.format("%09d", CPF_SEQUENCE.incrementAndGet());
        int firstDigit = cpfDigit(base, 9);
        int secondDigit = cpfDigit(base + firstDigit, 10);
        return base + firstDigit + secondDigit;
    }

    private static int cpfDigit(String value, int length) {
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += Character.digit(value.charAt(index), 10) * (length + 1 - index);
        }
        int result = 11 - (sum % 11);
        return result >= 10 ? 0 : result;
    }
}
