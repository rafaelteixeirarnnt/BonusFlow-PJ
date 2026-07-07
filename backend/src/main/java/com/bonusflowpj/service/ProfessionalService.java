package com.bonusflowpj.service;

import com.bonusflowpj.domain.AuditAction;
import com.bonusflowpj.domain.Professional;
import com.bonusflowpj.repository.ProfessionalRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfessionalService {

    private final ProfessionalRepository professionalRepository;
    private final AuditLogService auditLogService;

    public ProfessionalService(ProfessionalRepository professionalRepository, AuditLogService auditLogService) {
        this.professionalRepository = professionalRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<Professional> list() {
        return professionalRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Professional find(Long id) {
        return professionalRepository.findById(id)
            .orElseThrow(() -> new BusinessRuleException("Profissional nao encontrado."));
    }

    @Transactional
    public Professional create(String name, String email, String document, String team, boolean active) {
        return professionalRepository.save(new Professional(name, email, document, team, active));
    }

    @Transactional
    public Professional update(
        Long id,
        String name,
        String email,
        String document,
        String team,
        boolean active,
        String justification,
        Long performedByUserId,
        String ipAddress
    ) {
        Professional professional = find(id);
        boolean documentChanged = !professional.getDocument().equals(document);
        if (documentChanged) {
            auditLogService.requireJustification(justification);
        }
        String previousDocument = professional.getDocument();
        professional.update(name, email, document, team, active);
        if (documentChanged && performedByUserId != null) {
            auditLogService.record(
                "Professional",
                professional.getId(),
                AuditAction.PROFESSIONAL_DOCUMENT_CHANGED,
                previousDocument,
                professional.getDocument(),
                justification,
                performedByUserId,
                ipAddress
            );
        }
        return professional;
    }

    @Transactional
    public Professional deactivate(Long id, String justification, Long performedByUserId, String ipAddress) {
        auditLogService.requireJustification(justification);
        Professional professional = find(id);
        professional.deactivate();
        if (performedByUserId != null) {
            auditLogService.record(
                "Professional",
                professional.getId(),
                AuditAction.PROFESSIONAL_DEACTIVATED,
                "true",
                "false",
                justification,
                performedByUserId,
                ipAddress
            );
        }
        return professional;
    }

    @Transactional
    public Professional activate(Long id, String justification, Long performedByUserId, String ipAddress) {
        auditLogService.requireJustification(justification);
        Professional professional = find(id);
        professional.update(
            professional.getName(),
            professional.getEmail(),
            professional.getDocument(),
            professional.getTeam(),
            true
        );
        if (performedByUserId != null) {
            auditLogService.record(
                "Professional",
                professional.getId(),
                AuditAction.PROFESSIONAL_REACTIVATED,
                "false",
                "true",
                justification,
                performedByUserId,
                ipAddress
            );
        }
        return professional;
    }
}
