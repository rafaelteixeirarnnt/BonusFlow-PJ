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
import java.time.LocalDate;

@Entity
@Table(name = "absence_requests")
public class AbsenceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professional_id", nullable = false)
    private Professional professional;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "absence_type", nullable = false)
    private AbsenceType absenceType;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private int requestedDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AbsenceStatus status = AbsenceStatus.PENDING;

    @Column(length = 1000)
    private String reason;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected AbsenceRequest() {
    }

    public AbsenceRequest(Professional professional, User createdBy, AbsenceType absenceType, LocalDate startDate, LocalDate endDate, int requestedDays, String reason) {
        this.professional = professional;
        this.createdBy = createdBy;
        this.absenceType = absenceType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.requestedDays = requestedDays;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public Professional getProfessional() {
        return professional;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public AbsenceType getAbsenceType() {
        return absenceType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public int getRequestedDays() {
        return requestedDays;
    }

    public AbsenceStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void approve() {
        this.status = AbsenceStatus.APPROVED;
    }

    public void reject() {
        this.status = AbsenceStatus.REJECTED;
    }

    public void cancel() {
        this.status = AbsenceStatus.CANCELLED;
    }
}
