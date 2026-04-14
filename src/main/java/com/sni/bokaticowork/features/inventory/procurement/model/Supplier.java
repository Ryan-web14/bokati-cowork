package com.sni.bokaticowork.features.inventory.procurement.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.procurement.enums.SupplierStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_supplier")
public class Supplier {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "supplier_code", nullable = false, length = 100, unique = true)
    private String supplierCode;

    @Column(name = "name", nullable = false, length = 220)
    private String name;

    @Column(name = "email", length = 180)
    private String email;

    @Column(name = "phone", length = 80)
    private String phone;

    @Column(name = "tax_id", length = 120)
    private String taxId;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private SupplierStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = SupplierStatus.ACTIVE;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
