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

/** Une modification de la fiche d'une caisse : qui, quand, ancienne et nouvelle valeur. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_register_history")
public class CashRegisterHistory {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "register_code", nullable = false, length = 100)
    private String registerCode;

    @Column(name = "field", nullable = false, length = 80)
    private String field;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "changed_by", length = 180)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "effective_at")
    private Instant effectiveAt;

    @Column(name = "reason", length = 500)
    private String reason;

    @PrePersist
    void prePersist() {
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }
}
