package com.bonusflowpj.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bonusflowpj.domain.AbsenceRequest;
import com.bonusflowpj.domain.AbsenceStatus;
import com.bonusflowpj.domain.AbsenceType;
import com.bonusflowpj.domain.ContractRule;
import com.bonusflowpj.domain.Professional;
import com.bonusflowpj.domain.User;
import com.bonusflowpj.domain.UserRole;
import com.bonusflowpj.repository.AbsenceRequestRepository;
import com.bonusflowpj.repository.ApprovalHistoryRepository;
import com.bonusflowpj.repository.ContractRuleRepository;
import com.bonusflowpj.repository.ProfessionalRepository;
import com.bonusflowpj.repository.UserRepository;
import com.bonusflowpj.service.dto.CreateAbsenceRequestCommand;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AbsenceRequestServiceTest {

    @Autowired
    private AbsenceRequestService service;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContractRuleRepository contractRuleRepository;

    @Autowired
    private AbsenceRequestRepository absenceRequestRepository;

    @Autowired
    private ApprovalHistoryRepository approvalHistoryRepository;

    @Test
    void createsPendingRequestWithInclusiveRequestedDays() {
        Fixtures fixtures = fixtures(true, 10);

        AbsenceRequest request = service.create(new CreateAbsenceRequestCommand(
            fixtures.professional().getId(),
            fixtures.user().getId(),
            AbsenceType.VACATION,
            LocalDate.of(2026, 7, 6),
            LocalDate.of(2026, 7, 8),
            "Descanso"
        ));

        assertThat(request.getStatus()).isEqualTo(AbsenceStatus.PENDING);
        assertThat(request.getRequestedDays()).isEqualTo(3);
    }

    @Test
    void rejectsInactiveProfessional() {
        Fixtures fixtures = fixtures(false, 10);

        assertThatThrownBy(() -> service.create(command(fixtures.professional(), fixtures.user(), 1, 2)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("inativo");
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        Fixtures fixtures = fixtures(true, 10);

        assertThatThrownBy(() -> service.create(new CreateAbsenceRequestCommand(
            fixtures.professional().getId(),
            fixtures.user().getId(),
            AbsenceType.VACATION,
            LocalDate.of(2026, 7, 10),
            LocalDate.of(2026, 7, 9),
            null
        ))).isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("data final");
    }

    @Test
    void rejectsOverlappingPeriodsForSameProfessional() {
        Fixtures fixtures = fixtures(true, 10);
        service.create(command(fixtures.professional(), fixtures.user(), 10, 12));

        assertThatThrownBy(() -> service.create(command(fixtures.professional(), fixtures.user(), 12, 14)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("sobreposto");
    }

    @Test
    void approvalConsumesBalanceAndCreatesHistory() {
        Fixtures fixtures = fixtures(true, 5);
        AbsenceRequest request = service.create(command(fixtures.professional(), fixtures.user(), 1, 3));

        service.approve(request.getId(), fixtures.user().getId(), "Ok");

        assertThat(service.getBalance(fixtures.professional().getId(), AbsenceType.VACATION).availableDays()).isEqualTo(2);
        assertThat(approvalHistoryRepository.findByAbsenceRequestIdOrderByChangedAtDesc(request.getId())).hasSize(1);
    }

    @Test
    void pendingRequestsDoNotConsumeBalanceButCannotExceedAvailableBalance() {
        Fixtures fixtures = fixtures(true, 2);
        AbsenceRequest request = service.create(command(fixtures.professional(), fixtures.user(), 1, 2));

        assertThat(service.getBalance(fixtures.professional().getId(), AbsenceType.VACATION).availableDays()).isEqualTo(2);
        assertThat(request.getRequestedDays()).isEqualTo(2);

        service.approve(request.getId(), fixtures.user().getId(), "Ok");

        assertThatThrownBy(() -> service.create(command(fixtures.professional(), fixtures.user(), 5, 7)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("saldo");
    }

    private CreateAbsenceRequestCommand command(Professional professional, User user, int startDay, int endDay) {
        return new CreateAbsenceRequestCommand(
            professional.getId(),
            user.getId(),
            AbsenceType.VACATION,
            LocalDate.of(2026, 7, startDay),
            LocalDate.of(2026, 7, endDay),
            null
        );
    }

    private Fixtures fixtures(boolean activeProfessional, int daysAllowed) {
        Professional professional = professionalRepository.save(
            new Professional("Ana Silva " + System.nanoTime(), "ana" + System.nanoTime() + "@example.com", "123", "Produto", activeProfessional)
        );
        User user = userRepository.save(
            new User("Rafa Gestor " + System.nanoTime(), "rafa" + System.nanoTime() + "@example.com", UserRole.ADMIN, true)
        );
        contractRuleRepository.save(
            new ContractRule(professional, AbsenceType.VACATION, daysAllowed, LocalDate.of(2026, 1, 1), null)
        );
        return new Fixtures(professional, user);
    }

    private record Fixtures(Professional professional, User user) {
    }
}
