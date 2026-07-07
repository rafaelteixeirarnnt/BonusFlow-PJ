package com.bonusflowpj.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String entityName;

    @Column(nullable = false)
    private Long entityId;

    @Column(nullable = false)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String previousValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String justification;

    @Column(nullable = false)
    private Long performedByUserId;

    @Column(nullable = false)
    private String performedByUserName;

    @Column(nullable = false)
    private Instant performedAt = Instant.now();

    private String ipAddress;

    protected AuditLog() {
    }

    public AuditLog(
        String entityName,
        Long entityId,
        AuditAction action,
        String previousValue,
        String newValue,
        String justification,
        Long performedByUserId,
        String performedByUserName,
        String ipAddress
    ) {
        this.entityName = entityName;
        this.entityId = entityId;
        this.action = action.name();
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.justification = justification;
        this.performedByUserId = performedByUserId;
        this.performedByUserName = performedByUserName;
        this.ipAddress = ipAddress;
    }

    public Long getId() {
        return id;
    }

    public String getEntityName() {
        return entityName;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getAction() {
        return action;
    }

    public String getPreviousValue() {
        return previousValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getJustification() {
        return justification;
    }

    public Long getPerformedByUserId() {
        return performedByUserId;
    }

    public String getPerformedByUserName() {
        return performedByUserName;
    }

    public Instant getPerformedAt() {
        return performedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }
}
