package com.sni.bokaticowork.features.client.customer.model;

import com.sni.bokaticowork.features.client.customer.enums.CustomerStatus;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.core.baseClasses.model.Address;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "customer")
@SQLDelete(sql = "UPDATE customer SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class Customer {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "customer_id", nullable = false, unique = true, length = 300)
    private String customerId;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private CustomerType type;

    @Column(name = "firstname")
    private String firstname;

    @Column(name = "lastname")
    private String lastname;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "billing_email")
    private String billingEmail;

    @Column(name = "phone")
    private String phone;

    @Column(name = "whatsapp_phone")
    private String whatsappPhone;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "address_id", foreignKey = @ForeignKey(name = "customer_address_fk"))
    private Address address;

    @Column(name = "is_member")
    private boolean isMember;

    @Column(name = "status", nullable = false)
    private CustomerStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PostUpdate
    public void postUpdate() {
        updatedAt = Instant.now();
    }


}

