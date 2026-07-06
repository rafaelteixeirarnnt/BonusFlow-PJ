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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
    name = "contract_rules",
    uniqueConstraints = @UniqueConstraint(name = "uk_contract_rule_professional_type", columnNames = {"professional_id", "absence_type"})
)
public class ContractRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professional_id", nullable = false)
    private Professional professional;

    @Enumerated(EnumType.STRING)
    @Column(name = "absence_type", nullable = false)
    private AbsenceType absenceType;

    @Column(name = "days_allowed", nullable = false)
    private int daysAllowed;

    @Column(nullable = false)
    private LocalDate validFrom;

    private LocalDate validTo;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected ContractRule() {
    }

    public ContractRule(Professional professional, AbsenceType absenceType, int daysAllowed, LocalDate validFrom, LocalDate validTo) {
        this.professional = professional;
        this.absenceType = absenceType;
        this.daysAllowed = daysAllowed;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public Long getId() {
        return id;
    }

    public Professional getProfessional() {
        return professional;
    }

    public AbsenceType getAbsenceType() {
        return absenceType;
    }

    public int getDaysAllowed() {
        return daysAllowed;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
