package com.bonusflowpj.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bonusflowpj.domain.Professional;
import com.bonusflowpj.domain.User;
import com.bonusflowpj.domain.UserRole;
import com.bonusflowpj.repository.ProfessionalRepository;
import com.bonusflowpj.repository.UserRepository;
import com.bonusflowpj.service.dto.CreateUserCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void rejectsCommonUserWithoutProfessional() {
        assertThatThrownBy(() -> userService.create(new CreateUserCommand(
            "Ana", "ana.user@example.com", "Senha@123", UserRole.ADMIN, null, true
        ))).isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("profissional");
    }

    @Test
    void createsCommonUserLinkedToProfessionalWithEncryptedPassword() {
        Professional professional = professional();

        User user = userService.create(new CreateUserCommand(
            "Ana", "ana.access@example.com", "Senha@123", UserRole.PROFESSIONAL, professional.getId(), true
        ));

        assertThat(user.getProfessional().getId()).isEqualTo(professional.getId());
        assertThat(user.getPassword()).isNotEqualTo("Senha@123");
        assertThat(passwordEncoder.matches("Senha@123", user.getPassword())).isTrue();
    }

    @Test
    void rejectsSecondUserForSameProfessional() {
        Professional professional = professional();
        userService.create(new CreateUserCommand(
            "Ana", "ana.first@example.com", "Senha@123", UserRole.PROFESSIONAL, professional.getId(), true
        ));

        assertThatThrownBy(() -> userService.create(new CreateUserCommand(
            "Ana Dois", "ana.second@example.com", "Senha@123", UserRole.VIEWER, professional.getId(), true
        ))).isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("profissional");
    }

    @Test
    void rejectsDuplicateEmail() {
        Professional first = professional();
        Professional second = professional();
        userService.create(new CreateUserCommand(
            "Ana", "duplicado@example.com", "Senha@123", UserRole.PROFESSIONAL, first.getId(), true
        ));

        assertThatThrownBy(() -> userService.create(new CreateUserCommand(
            "Bia", "duplicado@example.com", "Senha@123", UserRole.VIEWER, second.getId(), true
        ))).isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("e-mail");
    }

    @Test
    void rejectsDeactivationOfSystemUser() {
        User superAdmin = userService.ensureSuperAdmin();

        assertThatThrownBy(() -> userService.deactivate(superAdmin.getId()))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("sistema");
    }

    @Test
    void rejectsInactiveUserAuthentication() {
        Professional professional = professional();
        User user = userService.create(new CreateUserCommand(
            "Ana", "inactive@example.com", "Senha@123", UserRole.PROFESSIONAL, professional.getId(), true
        ));
        userService.deactivate(user.getId());

        assertThatThrownBy(() -> userService.authenticate("inactive@example.com", "Senha@123"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("inativo");
    }

    private Professional professional() {
        return professionalRepository.save(new Professional(
            "Pessoa " + System.nanoTime(),
            "pessoa" + System.nanoTime() + "@example.com",
            "DOC" + System.nanoTime(),
            "Produto",
            true
        ));
    }
}
