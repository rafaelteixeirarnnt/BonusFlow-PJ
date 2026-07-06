package com.bonusflowpj.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "approval_history")
public class ApprovalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "absence_request_id", nullable = false)
    private AbsenceRequest absenceRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private User changedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AbsenceStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AbsenceStatus toStatus;

    @Column(length = 1000)
    private String comment;

    @Column(nullable = false)
    private Instant changedAt = Instant.now();

    protected ApprovalHistory() {
    }

    public ApprovalHistory(AbsenceRequest absenceRequest, User changedBy, AbsenceStatus fromStatus, AbsenceStatus toStatus, String comment) {
        this.absenceRequest = absenceRequest;
        this.changedBy = changedBy;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.comment = comment;
    }

    public Long getId() {
        return id;
    }

    public AbsenceRequest getAbsenceRequest() {
        return absenceRequest;
    }

    public User getChangedBy() {
        return changedBy;
    }

    public AbsenceStatus getFromStatus() {
        return fromStatus;
    }

    public AbsenceStatus getToStatus() {
        return toStatus;
    }

    public String getComment() {
        return comment;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
