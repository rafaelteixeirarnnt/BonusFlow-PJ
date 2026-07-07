package com.bonusflowpj.domain;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, length = 11)
    private String cpf;

    private LocalDate birthDate;

    private String motherName;

    private String fatherName;

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

    @ElementCollection
    @CollectionTable(name = "user_contacts", joinColumns = @JoinColumn(name = "user_id"))
    private List<UserContact> contacts = new ArrayList<>();

    @Embedded
    private UserAddress address;

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

    public User(
        String name,
        String cpf,
        LocalDate birthDate,
        String motherName,
        String fatherName,
        String email,
        String password,
        UserRole role,
        boolean active,
        Professional professional,
        boolean systemUser,
        List<UserContact> contacts,
        UserAddress address
    ) {
        this(name, email, password, role, active, professional, systemUser);
        this.cpf = cpf;
        this.birthDate = birthDate;
        this.motherName = motherName;
        this.fatherName = fatherName;
        replaceContacts(contacts);
        this.address = address;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCpf() {
        return cpf;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getMotherName() {
        return motherName;
    }

    public String getFatherName() {
        return fatherName;
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

    public List<UserContact> getContacts() {
        return List.copyOf(contacts);
    }

    public UserAddress getAddress() {
        return address;
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

    public void updateProfile(
        String name,
        String cpf,
        LocalDate birthDate,
        String motherName,
        String fatherName,
        String email,
        UserRole role,
        Professional professional,
        boolean active,
        List<UserContact> contacts,
        UserAddress address
    ) {
        update(name, email, role, professional, active);
        this.cpf = cpf;
        this.birthDate = birthDate;
        this.motherName = motherName;
        this.fatherName = fatherName;
        replaceContacts(contacts);
        this.address = address;
    }

    private void replaceContacts(List<UserContact> contacts) {
        this.contacts.clear();
        if (contacts != null) {
            this.contacts.addAll(contacts);
        }
    }

    public void linkProfessional(Professional professional) {
        this.professional = professional;
    }

    public void unlinkProfessional() {
        this.professional = null;
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
