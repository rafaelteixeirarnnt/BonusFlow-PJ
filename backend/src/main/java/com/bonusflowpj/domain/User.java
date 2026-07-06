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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id")
    private Professional professional;

    @Column(name = "system_user_flag", nullable = false)
    private boolean systemUser;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    private Instant lastLoginAt;

    protected User() {
    }

    public User(String name, String email, UserRole role, boolean active) {
        this(name, email, "{noop}test-password", role, active, null, false);
    }

    public User(String name, String email, String password, UserRole role, boolean active, Professional professional, boolean systemUser) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.active = active;
        this.professional = professional;
        this.systemUser = systemUser;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public UserRole getRole() {
        return role;
    }

    public Professional getProfessional() {
        return professional;
    }

    public boolean isSystemUser() {
        return systemUser;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void update(String name, String email, UserRole role, Professional professional, boolean active) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.professional = professional;
        this.active = active;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public void registerLogin() {
        this.lastLoginAt = Instant.now();
    }

    @PreUpdate
    public void touch() {
        this.updatedAt = Instant.now();
    }
}
