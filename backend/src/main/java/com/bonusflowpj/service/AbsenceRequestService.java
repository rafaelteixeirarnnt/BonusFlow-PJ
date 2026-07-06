package com.bonusflowpj.service;

import com.bonusflowpj.domain.AbsenceRequest;
import com.bonusflowpj.domain.AbsenceStatus;
import com.bonusflowpj.domain.AbsenceType;
import com.bonusflowpj.domain.ApprovalHistory;
import com.bonusflowpj.domain.ContractRule;
import com.bonusflowpj.domain.Professional;
import com.bonusflowpj.domain.User;
import com.bonusflowpj.repository.AbsenceRequestRepository;
import com.bonusflowpj.repository.ApprovalHistoryRepository;
import com.bonusflowpj.repository.ContractRuleRepository;
import com.bonusflowpj.repository.ProfessionalRepository;
import com.bonusflowpj.repository.UserRepository;
import com.bonusflowpj.service.dto.BalanceSummary;
import com.bonusflowpj.service.dto.CreateAbsenceRequestCommand;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AbsenceRequestService {

    private static final List<AbsenceStatus> BLOCKING_STATUSES = List.of(AbsenceStatus.PENDING, AbsenceStatus.APPROVED);

    private final AbsenceRequestRepository absenceRequestRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final ContractRuleRepository contractRuleRepository;
    private final ProfessionalRepository professionalRepository;
    private final UserRepository userRepository;

    public AbsenceRequestService(
        AbsenceRequestRepository absenceRequestRepository,
        ApprovalHistoryRepository approvalHistoryRepository,
        ContractRuleRepository contractRuleRepository,
        ProfessionalRepository professionalRepository,
        UserRepository userRepository
    ) {
        this.absenceRequestRepository = absenceRequestRepository;
        this.approvalHistoryRepository = approvalHistoryRepository;
        this.contractRuleRepository = contractRuleRepository;
        this.professionalRepository = professionalRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AbsenceRequest create(CreateAbsenceRequestCommand command) {
        validatePeriod(command.startDate(), command.endDate());
        Professional professional = professionalRepository.findById(command.professionalId())
            .orElseThrow(() -> new BusinessRuleException("Profissional nao encontrado."));
        if (!professional.isActive()) {
            throw new BusinessRuleException("Nao e permitido lancar afastamento para profissional inativo.");
        }

        User createdBy = userRepository.findById(command.createdById())
            .orElseThrow(() -> new BusinessRuleException("Usuario responsavel nao encontrado."));
        ContractRule rule = contractRuleRepository.findByProfessionalIdAndAbsenceType(professional.getId(), command.absenceType())
            .orElseThrow(() -> new BusinessRuleException("Regra contratual nao encontrada para este tipo de afastamento."));

        int requestedDays = calculateRequestedDays(command.startDate(), command.endDate());
        BalanceSummary balance = getBalance(professional.getId(), command.absenceType());
        if (requestedDays > balance.availableDays()) {
            throw new BusinessRuleException("Profissional nao possui saldo disponivel para este lancamento.");
        }
        if (command.startDate().isBefore(rule.getValidFrom()) || (rule.getValidTo() != null && command.endDate().isAfter(rule.getValidTo()))) {
            throw new BusinessRuleException("Periodo solicitado esta fora da vigencia da regra contratual.");
        }
        if (absenceRequestRepository.existsByProfessionalIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            professional.getId(), BLOCKING_STATUSES, command.endDate(), command.startDate())) {
            throw new BusinessRuleException("Ja existe lancamento em periodo sobreposto para este profissional.");
        }

        return absenceRequestRepository.save(new AbsenceRequest(
            professional,
            createdBy,
            command.absenceType(),
            command.startDate(),
            command.endDate(),
            requestedDays,
            command.reason()
        ));
    }

    @Transactional(readOnly = true)
    public BalanceSummary getBalance(Long professionalId, AbsenceType absenceType) {
        ContractRule rule = contractRuleRepository.findByProfessionalIdAndAbsenceType(professionalId, absenceType)
            .orElseThrow(() -> new BusinessRuleException("Regra contratual nao encontrada para este tipo de afastamento."));
        int approvedDays = absenceRequestRepository.sumRequestedDaysByProfessionalTypeAndStatus(
            professionalId, absenceType, AbsenceStatus.APPROVED);
        return new BalanceSummary(professionalId, absenceType, rule.getDaysAllowed(), approvedDays, rule.getDaysAllowed() - approvedDays);
    }

    @Transactional
    public AbsenceRequest approve(Long requestId, Long userId, String comment) {
        AbsenceRequest request = findRequest(requestId);
        if (request.getStatus() != AbsenceStatus.PENDING) {
            throw new BusinessRuleException("Apenas lancamentos pendentes podem ser aprovados.");
        }
        BalanceSummary balance = getBalance(request.getProfessional().getId(), request.getAbsenceType());
        if (request.getRequestedDays() > balance.availableDays()) {
            throw new BusinessRuleException("Profissional nao possui saldo disponivel para aprovacao.");
        }
        return transition(request, userId, AbsenceStatus.APPROVED, comment);
    }

    @Transactional
    public AbsenceRequest reject(Long requestId, Long userId, String comment) {
        AbsenceRequest request = findRequest(requestId);
        if (request.getStatus() != AbsenceStatus.PENDING) {
            throw new BusinessRuleException("Apenas lancamentos pendentes podem ser reprovados.");
        }
        return transition(request, userId, AbsenceStatus.REJECTED, comment);
    }

    @Transactional
    public AbsenceRequest cancel(Long requestId, Long userId, String comment) {
        AbsenceRequest request = findRequest(requestId);
        if (request.getStatus() == AbsenceStatus.CANCELLED) {
            throw new BusinessRuleException("Lancamento ja cancelado.");
        }
        return transition(request, userId, AbsenceStatus.CANCELLED, comment);
    }

    @Transactional(readOnly = true)
    public List<AbsenceRequest> report(LocalDate monthStart, LocalDate monthEnd, Long professionalId, AbsenceType absenceType) {
        return absenceRequestRepository.report(monthStart, monthEnd, professionalId, absenceType);
    }

    private AbsenceRequest transition(AbsenceRequest request, Long userId, AbsenceStatus toStatus, String comment) {
        User changedBy = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessRuleException("Usuario responsavel nao encontrado."));
        AbsenceStatus fromStatus = request.getStatus();
        switch (toStatus) {
            case APPROVED -> request.approve();
            case REJECTED -> request.reject();
            case CANCELLED -> request.cancel();
            default -> throw new BusinessRuleException("Transicao de status invalida.");
        }
        approvalHistoryRepository.save(new ApprovalHistory(request, changedBy, fromStatus, toStatus, comment));
        return request;
    }

    private AbsenceRequest findRequest(Long requestId) {
        return absenceRequestRepository.findWithAssociationsById(requestId)
            .orElseThrow(() -> new BusinessRuleException("Lancamento nao encontrado."));
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessRuleException("Periodo deve informar data inicial e data final.");
        }
        if (endDate.isBefore(startDate)) {
            throw new BusinessRuleException("A data final nao pode ser menor que a data inicial.");
        }
    }

    private int calculateRequestedDays(LocalDate startDate, LocalDate endDate) {
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
}
