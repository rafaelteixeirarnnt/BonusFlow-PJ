package com.bonusflowpj.service;

import com.bonusflowpj.domain.AuditAction;
import com.bonusflowpj.domain.AuditLog;
import com.bonusflowpj.domain.User;
import com.bonusflowpj.repository.AuditLogRepository;
import com.bonusflowpj.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog record(
        String entityName,
        Long entityId,
        AuditAction action,
        String previousValue,
        String newValue,
        String justification,
        Long performedByUserId,
        String ipAddress
    ) {
        requireJustification(justification);
        if (performedByUserId == null) {
            throw new BusinessRuleException("Usuario responsavel pela auditoria e obrigatorio.");
        }
        User performedBy = userRepository.findById(performedByUserId)
            .orElseThrow(() -> new BusinessRuleException("Usuario responsavel pela auditoria nao encontrado."));
        return auditLogRepository.save(new AuditLog(
            entityName,
            entityId,
            action,
            previousValue,
            newValue,
            justification,
            performedBy.getId(),
            performedBy.getName(),
            ipAddress
        ));
    }

    @Transactional(readOnly = true)
    public List<AuditLog> search(String entityName, String action, Long performedByUserId, Instant startAt, Instant endAt) {
        return auditLogRepository.search(blankToNull(entityName), blankToNull(action), performedByUserId, startAt, endAt);
    }

    @Transactional(readOnly = true)
    public AuditLog find(Long id) {
        return auditLogRepository.findById(id)
            .orElseThrow(() -> new BusinessRuleException("Registro de auditoria nao encontrado."));
    }

    public void requireJustification(String justification) {
        if (justification == null || justification.isBlank()) {
            throw new BusinessRuleException("Justificativa obrigatoria para alteracoes sensiveis.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
