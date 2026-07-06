package com.bonusflowpj.web;

import com.bonusflowpj.domain.AbsenceRequest;
import com.bonusflowpj.domain.AbsenceType;
import com.bonusflowpj.repository.AbsenceRequestRepository;
import com.bonusflowpj.service.AbsenceRequestService;
import com.bonusflowpj.service.dto.BalanceSummary;
import com.bonusflowpj.service.dto.CreateAbsenceRequestCommand;
import com.bonusflowpj.web.dto.AbsenceRequestDtos.AbsenceRequestResponse;
import com.bonusflowpj.web.dto.AbsenceRequestDtos.CreateAbsenceRequest;
import com.bonusflowpj.web.dto.AbsenceRequestDtos.TransitionRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/absence-requests")
public class AbsenceRequestController {

    private final AbsenceRequestRepository absenceRequestRepository;
    private final AbsenceRequestService absenceRequestService;

    public AbsenceRequestController(AbsenceRequestRepository absenceRequestRepository, AbsenceRequestService absenceRequestService) {
        this.absenceRequestRepository = absenceRequestRepository;
        this.absenceRequestService = absenceRequestService;
    }

    @GetMapping
    public List<AbsenceRequestResponse> list() {
        return absenceRequestRepository.findAll().stream().map(this::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AbsenceRequestResponse create(@Valid @RequestBody CreateAbsenceRequest request) {
        return toResponse(absenceRequestService.create(new CreateAbsenceRequestCommand(
            request.professionalId(),
            request.createdById(),
            request.absenceType(),
            request.startDate(),
            request.endDate(),
            request.reason()
        )));
    }

    @PatchMapping("/{id}/approve")
    public AbsenceRequestResponse approve(@PathVariable Long id, @Valid @RequestBody TransitionRequest request) {
        return toResponse(absenceRequestService.approve(id, request.userId(), request.comment()));
    }

    @PatchMapping("/{id}/reject")
    public AbsenceRequestResponse reject(@PathVariable Long id, @Valid @RequestBody TransitionRequest request) {
        return toResponse(absenceRequestService.reject(id, request.userId(), request.comment()));
    }

    @PatchMapping("/{id}/cancel")
    public AbsenceRequestResponse cancel(@PathVariable Long id, @Valid @RequestBody TransitionRequest request) {
        return toResponse(absenceRequestService.cancel(id, request.userId(), request.comment()));
    }

    @GetMapping("/balance")
    public BalanceSummary balance(@RequestParam Long professionalId, @RequestParam AbsenceType absenceType) {
        return absenceRequestService.getBalance(professionalId, absenceType);
    }

    @GetMapping("/report")
    public List<AbsenceRequestResponse> report(
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM") java.time.YearMonth month,
        @RequestParam(required = false) Long professionalId,
        @RequestParam(required = false) AbsenceType absenceType
    ) {
        return absenceRequestService.report(month.atDay(1), month.atEndOfMonth(), professionalId, absenceType)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private AbsenceRequestResponse toResponse(AbsenceRequest request) {
        return new AbsenceRequestResponse(
            request.getId(),
            request.getProfessional().getId(),
            request.getProfessional().getName(),
            request.getCreatedBy().getId(),
            request.getCreatedBy().getName(),
            request.getAbsenceType(),
            request.getStartDate(),
            request.getEndDate(),
            request.getRequestedDays(),
            request.getStatus(),
            request.getReason(),
            request.getCreatedAt()
        );
    }
}
