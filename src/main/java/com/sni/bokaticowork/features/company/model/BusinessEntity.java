package com.sni.bokaticowork.features.company.model;


import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.company.enums.Status;
import com.sni.bokaticowork.core.baseClasses.model.Address;
import com.sni.bokaticowork.core.baseClasses.model.Currency;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

//TODO: add in the sql script a delete column
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "business_entity")
@SQLDelete(sql = "UPDATE business_entity SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class BusinessEntity {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "entity_code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "legal_form", nullable = false)
    @NotBlank(message = "the legal form of the business is required")
    private String legalForm;

    @Column(name = "niu_number", nullable = false)
    @Size(max = 18, message = "Niu number must be 12 or 13 digits")
    private String niuNumber;

    @Column(name = "rccm_number", nullable = false)
    private String rccmNumber;

    @Column(name = "tax_id")
    private String taxId;

    @Column(name = "activity")
    private String activity;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", foreignKey = @ForeignKey(name = "business_entity_address_fk"))
    private Address address;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "email")
    @Email(message = "Email is not valid")
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_currency_id", nullable = false,
            foreignKey = @ForeignKey(name = "base_currency_id_fk"))
    private Currency baseCurrency;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Builder.Default
    @Column(name = "deleted")
    private boolean deleted = Boolean.FALSE;

    @Column(name = "created_by", nullable = false)
    private String created_by;

    @Column(name = "updated_by")
    private String updated_by;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }



}
