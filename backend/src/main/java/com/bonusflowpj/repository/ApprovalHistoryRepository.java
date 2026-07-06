package com.bonusflowpj.repository;

import com.bonusflowpj.domain.ApprovalHistory;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, Long> {

    @EntityGraph(attributePaths = {"absenceRequest", "changedBy"})
    List<ApprovalHistory> findByAbsenceRequestIdOrderByChangedAtDesc(Long absenceRequestId);
}
