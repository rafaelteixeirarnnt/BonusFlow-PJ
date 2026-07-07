package com.bonusflowpj.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bonusflowpj.domain.AuditAction;
import com.bonusflowpj.domain.AuditLog;
import com.bonusflowpj.domain.Professional;
import com.bonusflowpj.domain.User;
import com.bonusflowpj.domain.UserRole;
import com.bonusflowpj.repository.AuditLogRepository;
import com.bonusflowpj.repository.ProfessionalRepository;
import com.bonusflowpj.repository.UserRepository;
import com.bonusflowpj.service.dto.CreateUserCommand;
import com.bonusflowpj.service.dto.UpdateUserCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuditLogServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void changingUserEmailRequiresJustification() {
        User actor = userService.ensureSuperAdmin();
        User target = commonUser();

        assertThatThrownBy(() -> userService.update(target.getId(), new UpdateUserCommand(
            target.getName(),
            "new-email@example.com",
            target.getRole(),
            target.getProfessional().getId(),
            true,
            null,
            actor.getId(),
            "127.0.0.1"
        ))).isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Justificativa");
    }

    @Test
    void changingUserEmailCreatesAuditLog() {
        User actor = userService.ensureSuperAdmin();
        User target = commonUser();

        userService.update(target.getId(), new UpdateUserCommand(
            target.getName(),
            "changed-" + System.nanoTime() + "@example.com",
            target.getRole(),
            target.getProfessional().getId(),
            true,
            "Correcao solicitada pelo administrativo",
            actor.getId(),
            "127.0.0.1"
        ));

        AuditLog auditLog = auditLogRepository.findAll().stream()
            .filter(log -> log.getAction().equals(AuditAction.CHANGE_USER_EMAIL.name()))
            .findFirst()
            .orElseThrow();

        assertThat(auditLog.getEntityName()).isEqualTo("User");
        assertThat(auditLog.getEntityId()).isEqualTo(target.getId());
        assertThat(auditLog.getPreviousValue()).contains(target.getEmail());
        assertThat(auditLog.getNewValue()).contains("changed-");
        assertThat(auditLog.getJustification()).contains("Correcao");
        assertThat(auditLog.getPerformedByUserId()).isEqualTo(actor.getId());
        assertThat(auditLog.getIpAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    void changingUserEmailRequiresSuperAdminActor() {
        User actor = commonUser();
        User target = commonUser();

        assertThatThrownBy(() -> userService.update(target.getId(), new UpdateUserCommand(
            target.getName(),
            "blocked-" + System.nanoTime() + "@example.com",
            target.getRole(),
            target.getProfessional().getId(),
            true,
            "Tentativa indevida",
            actor.getId(),
            "127.0.0.1"
        ))).isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Apenas SUPER_ADMIN pode alterar e-mail");
    }

    @Test
    void blockedSuperAdminDeactivationCreatesAuditLog() {
        User actor = userService.ensureSuperAdmin();

        assertThatThrownBy(() -> userService.deactivate(actor.getId(), "Teste de bloqueio", actor.getId(), "10.0.0.1"))
            .isInstanceOf(BusinessRuleException.class);

        assertThat(auditLogRepository.findAll()).anySatisfy(log -> {
            assertThat(log.getAction()).isEqualTo(AuditAction.SUPER_ADMIN_DEACTIVATION_BLOCKED.name());
            assertThat(log.getEntityName()).isEqualTo("User");
            assertThat(log.getEntityId()).isEqualTo(actor.getId());
            assertThat(log.getJustification()).isEqualTo("Teste de bloqueio");
        });
    }

    private User commonUser() {
        Professional professional = professionalRepository.save(new Professional(
            "Audit Pessoa " + System.nanoTime(),
            "audit-pessoa" + System.nanoTime() + "@example.com",
            "DOC-AUDIT-" + System.nanoTime(),
            "Produto",
            true
        ));
        User user = userService.create(new CreateUserCommand(
            "Audit User",
            "audit-user" + System.nanoTime() + "@example.com",
            null,
            UserRole.PROFESSIONAL,
            professional.getId(),
            true
        ));
        return userRepository.findWithProfessionalById(user.getId()).orElseThrow();
    }
}
