package com.bonusflowpj.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bonusflowpj.domain.Professional;
import com.bonusflowpj.domain.User;
import com.bonusflowpj.domain.UserRole;
import com.bonusflowpj.domain.AuditAction;
import com.bonusflowpj.repository.AuditLogRepository;
import com.bonusflowpj.repository.ProfessionalRepository;
import com.bonusflowpj.repository.UserRepository;
import com.bonusflowpj.service.dto.CreateUserCommand;
import com.bonusflowpj.service.dto.UpdateUserCommand;
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
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createsCommonUserWithoutProfessionalButBlocksLogin() {
        User user = userService.create(new CreateUserCommand(
            "Ana", "ana.user@example.com", "Senha@123", UserRole.ADMIN, null, true
        ));

        assertThat(user.getProfessional()).isNull();
        assertThatThrownBy(() -> userService.authenticate("ana.user@example.com", "Senha@123"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Usuário sem profissional vinculado. Entre em contato com o administrador do sistema.");

        assertThat(auditLogRepository.findAll()).anySatisfy(log -> {
            assertThat(log.getAction()).isEqualTo(AuditAction.USER_LOGIN_WITHOUT_PROFESSIONAL_BLOCKED.name());
            assertThat(log.getEntityId()).isEqualTo(user.getId());
        });
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
    void createsCommonUserWithoutAutoLinkingProfessionalByEmail() {
        Professional professional = professional();

        User user = userService.create(new CreateUserCommand(
            "Ana", professional.getEmail(), null, UserRole.PROFESSIONAL, null, true
        ));

        assertThat(user.getProfessional()).isNull();
        assertThat(user.getPassword()).isNotBlank();
    }

    @Test
    void linksProfessionalToUserAndAllowsLogin() {
        User actor = userService.ensureSuperAdmin();
        User user = userService.create(new CreateUserCommand(
            "Ana", "ana.link@example.com", "Senha@123", UserRole.PROFESSIONAL, null, true
        ));
        Professional professional = professional();

        User linked = userService.linkProfessional(user.getId(), professional.getId(), actor.getId(), "127.0.0.1");

        assertThat(linked.getProfessional().getId()).isEqualTo(professional.getId());
        assertThat(userService.authenticate("ana.link@example.com", "Senha@123").getId()).isEqualTo(user.getId());
        assertThat(auditLogRepository.findAll()).anySatisfy(log -> {
            assertThat(log.getAction()).isEqualTo(AuditAction.USER_PROFESSIONAL_LINKED.name());
            assertThat(log.getEntityId()).isEqualTo(user.getId());
            assertThat(log.getNewValue()).isEqualTo(String.valueOf(professional.getId()));
        });
    }

    @Test
    void rejectsLinkingInactiveProfessional() {
        User actor = userService.ensureSuperAdmin();
        User user = userService.create(new CreateUserCommand(
            "Ana", "ana.inactive-professional@example.com", "Senha@123", UserRole.PROFESSIONAL, null, true
        ));
        Professional professional = professional();
        professional.deactivate();
        professionalRepository.save(professional);

        assertThatThrownBy(() -> userService.linkProfessional(user.getId(), professional.getId(), actor.getId(), "127.0.0.1"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Não é permitido vincular profissional inativo.");
    }

    @Test
    void rejectsLinkingProfessionalAlreadyLinkedToActiveUser() {
        User actor = userService.ensureSuperAdmin();
        Professional professional = professional();
        userService.create(new CreateUserCommand(
            "Ana", "ana.owner@example.com", "Senha@123", UserRole.PROFESSIONAL, professional.getId(), true
        ));
        User second = userService.create(new CreateUserCommand(
            "Bia", "bia.owner@example.com", "Senha@123", UserRole.PROFESSIONAL, null, true
        ));

        assertThatThrownBy(() -> userService.linkProfessional(second.getId(), professional.getId(), actor.getId(), "127.0.0.1"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Profissional já vinculado a outro usuário ativo.");
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
            .hasMessageContaining("Profissional já vinculado a outro usuário ativo.");
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
    void rejectsCreatingUserWithRoleAboveActorAndAuditsBlockedAttempt() {
        User actor = userService.create(new CreateUserCommand(
            "Gestor", "gestor" + System.nanoTime() + "@example.com", "Senha@123", UserRole.MANAGER, professional().getId(), true
        ));

        assertThatThrownBy(() -> userService.create(new CreateUserCommand(
            "Admin Bloqueado",
            "admin-bloqueado" + System.nanoTime() + "@example.com",
            null,
            UserRole.ADMIN,
            professional().getId(),
            true,
            actor.getId(),
            "127.0.0.1"
        ))).isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Você não possui permissão para conceder este perfil.");

        assertThat(auditLogRepository.findAll()).anySatisfy(log -> {
            assertThat(log.getAction()).isEqualTo(AuditAction.USER_ROLE_GRANT_BLOCKED.name());
            assertThat(log.getPerformedByUserId()).isEqualTo(actor.getId());
            assertThat(log.getNewValue()).isEqualTo(UserRole.ADMIN.name());
        });
    }

    @Test
    void rejectsUpdatingUserToRoleAboveActorAndAuditsBlockedAttempt() {
        User actor = userService.create(new CreateUserCommand(
            "Gestor", "gestor-update" + System.nanoTime() + "@example.com", "Senha@123", UserRole.MANAGER, professional().getId(), true
        ));
        User target = userService.create(new CreateUserCommand(
            "Profissional", "profissional-update" + System.nanoTime() + "@example.com", "Senha@123", UserRole.PROFESSIONAL, professional().getId(), true
        ));

        assertThatThrownBy(() -> userService.update(target.getId(), new UpdateUserCommand(
            target.getName(),
            target.getEmail(),
            UserRole.ADMIN,
            target.getProfessional().getId(),
            true,
            "Promocao indevida",
            actor.getId(),
            "127.0.0.1"
        ))).isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Você não possui permissão para conceder este perfil.");

        assertThat(auditLogRepository.findAll()).anySatisfy(log -> {
            assertThat(log.getAction()).isEqualTo(AuditAction.USER_ROLE_GRANT_BLOCKED.name());
            assertThat(log.getEntityId()).isEqualTo(target.getId());
            assertThat(log.getPreviousValue()).isEqualTo(UserRole.PROFESSIONAL.name());
            assertThat(log.getNewValue()).isEqualTo(UserRole.ADMIN.name());
        });
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
