package com.sni.bokaticowork.features.payment.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

/** Poste (appareil) vu ou autorise sur une caisse. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_register_device")
public class CashRegisterDevice {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "register_code", nullable = false, length = 100)
    private String registerCode;

    @Column(name = "device_code", nullable = false, length = 120)
    private String deviceCode;

    @Column(name = "label", length = 255)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private com.sni.bokaticowork.features.payment.enums.CashDeviceStatus status;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "first_seen_by", length = 180)
    private String firstSeenBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decided_by", length = 180)
    private String decidedBy;

    @PrePersist
    void prePersist() {
        if (firstSeenAt == null) {
            firstSeenAt = Instant.now();
        }
    }
}
