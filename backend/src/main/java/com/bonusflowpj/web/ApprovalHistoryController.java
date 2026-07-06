package com.bonusflowpj.web;

import com.bonusflowpj.domain.ApprovalHistory;
import com.bonusflowpj.repository.ApprovalHistoryRepository;
import com.bonusflowpj.web.dto.ApprovalHistoryResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/absence-requests/{requestId}/history")
public class ApprovalHistoryController {

    private final ApprovalHistoryRepository approvalHistoryRepository;

    public ApprovalHistoryController(ApprovalHistoryRepository approvalHistoryRepository) {
        this.approvalHistoryRepository = approvalHistoryRepository;
    }

    @GetMapping
    public List<ApprovalHistoryResponse> list(@PathVariable Long requestId) {
        return approvalHistoryRepository.findByAbsenceRequestIdOrderByChangedAtDesc(requestId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private ApprovalHistoryResponse toResponse(ApprovalHistory history) {
        return new ApprovalHistoryResponse(
            history.getId(),
            history.getAbsenceRequest().getId(),
            history.getChangedBy().getId(),
            history.getChangedBy().getName(),
            history.getFromStatus(),
            history.getToStatus(),
            history.getComment(),
            history.getChangedAt()
        );
    }
}
