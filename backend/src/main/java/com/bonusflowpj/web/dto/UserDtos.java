package com.bonusflowpj.web.dto;

import com.bonusflowpj.domain.ContactType;
import com.bonusflowpj.domain.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public final class UserDtos {

    private UserDtos() {
    }

    public record CreateUserRequest(
        @NotBlank(message = "Não deve estar em branco.") String fullName,
        @NotBlank(message = "Não deve estar em branco.") String cpf,
        @NotNull(message = "Deve ser informada.") LocalDate birthDate,
        @NotBlank(message = "Não deve estar em branco.") String motherName,
        String fatherName,
        @Email(message = "Deve ser um e-mail válido.") @NotBlank(message = "Não deve estar em branco.") String email,
        @NotNull(message = "Deve ser informado.") UserRole role,
        Long professionalId,
        @Valid List<ContactRequest> contacts,
        @Valid AddressRequest address
    ) {
    }

    public record UpdateUserRequest(
        @NotBlank(message = "Não deve estar em branco.") String fullName,
        @NotBlank(message = "Não deve estar em branco.") String cpf,
        @NotNull(message = "Deve ser informada.") LocalDate birthDate,
        @NotBlank(message = "Não deve estar em branco.") String motherName,
        String fatherName,
        @Email(message = "Deve ser um e-mail válido.") @NotBlank(message = "Não deve estar em branco.") String email,
        @NotNull(message = "Deve ser informado.") UserRole role,
        Long professionalId,
        boolean active,
        @Valid List<ContactRequest> contacts,
        @Valid AddressRequest address,
        String justification
    ) {
    }

    public record ContactRequest(
        @NotNull(message = "Deve ser informado.") ContactType type,
        @NotBlank(message = "Não deve estar em branco.") String ddi,
        String ddd,
        @NotBlank(message = "Não deve estar em branco.") @Size(max = 9, message = "Deve ter no máximo 9 caracteres.") String phone
    ) {
    }

    public record AddressRequest(
        String zipCode,
        String street,
        @NotBlank(message = "Não deve estar em branco.") String number,
        String complement,
        String neighborhood,
        String city,
        String state
    ) {
    }

    public record UserStatusRequest(String justification) {
    }

    public record LinkProfessionalRequest(
        @NotNull(message = "Deve ser informado.") Long professionalId
    ) {
    }

    public record UserResponse(
        Long id,
        String name,
        String fullName,
        String cpf,
        java.time.LocalDate birthDate,
        String motherName,
        String fatherName,
        String email,
        UserRole role,
        boolean active,
        Long professionalId,
        String professionalName,
        List<ContactResponse> contacts,
        AddressResponse address,
        boolean systemUser,
        java.time.Instant createdAt,
        java.time.Instant updatedAt,
        java.time.Instant lastLoginAt
    ) {
    }

    public record ContactResponse(ContactType type, String ddi, String ddd, String phone) {
    }

    public record AddressResponse(String zipCode, String street, String number, String complement, String neighborhood, String city, String state) {
    }

    public record PageResponse<T>(List<T> content, long totalElements, int totalPages, int number, int size) {
    }
}
