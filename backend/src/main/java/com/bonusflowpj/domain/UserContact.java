package com.bonusflowpj.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class UserContact {

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type", nullable = false)
    private ContactType type;

    @Column(name = "ddi", nullable = false, length = 6)
    private String ddi;

    @Column(name = "ddd", length = 4)
    private String ddd;

    @Column(name = "phone", nullable = false, length = 9)
    private String phone;

    protected UserContact() {
    }

    public UserContact(ContactType type, String ddi, String ddd, String phone) {
        this.type = type;
        this.ddi = ddi;
        this.ddd = ddd;
        this.phone = phone;
    }

    public ContactType getType() {
        return type;
    }

    public String getDdi() {
        return ddi;
    }

    public String getDdd() {
        return ddd;
    }

    public String getPhone() {
        return phone;
    }
}
