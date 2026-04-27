package com.sni.bokaticowork.features.payment.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_register")
public class CashRegister {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "register_code", nullable = false, unique = true, length = 100)
    private String registerCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "location_code", length = 120)
    private String locationCode;

    @Column(name = "business_entity_code", length = 120)
    private String businessEntityCode;

    @Column(name = "device_code", length = 120)
    private String deviceCode;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "cash_control_enabled", nullable = false)
    private Boolean cashControlEnabled;

    @Column(name = "max_cash_amount", precision = 19, scale = 4)
    private java.math.BigDecimal maxCashAmount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (active == null) {
            active = true;
        }
        if (cashControlEnabled == null) {
            cashControlEnabled = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
