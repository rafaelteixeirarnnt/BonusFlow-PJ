package com.bonusflowpj.service;

import com.bonusflowpj.domain.AbsenceStatus;
import com.bonusflowpj.repository.AbsenceRequestRepository;
import com.bonusflowpj.repository.ProfessionalRepository;
import com.bonusflowpj.web.dto.DashboardResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final ProfessionalRepository professionalRepository;
    private final AbsenceRequestRepository absenceRequestRepository;

    public DashboardService(ProfessionalRepository professionalRepository, AbsenceRequestRepository absenceRequestRepository) {
        this.professionalRepository = professionalRepository;
        this.absenceRequestRepository = absenceRequestRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse summary() {
        long professionals = professionalRepository.count();
        long pending = absenceRequestRepository.findAll().stream()
            .filter(request -> request.getStatus() == AbsenceStatus.PENDING)
            .count();
        int approvedDays = absenceRequestRepository.findAll().stream()
            .filter(request -> request.getStatus() == AbsenceStatus.APPROVED)
            .mapToInt(request -> request.getRequestedDays())
            .sum();
        return new DashboardResponse(professionals, pending, approvedDays);
    }
}
