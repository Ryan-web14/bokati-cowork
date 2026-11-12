package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Taux de change du jour d'une devise en XAF (lot C5). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_exchange_rate")
public class CashExchangeRate {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "rate", nullable = false)
    private java.math.BigDecimal rate;

    @Column(name = "valid_on", nullable = false)
    private java.time.LocalDate validOn;

    @Column(name = "set_by", nullable = false)
    private String setBy;

    @Column(name = "created_at", nullable = false)
    private java.time.Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
    }
}
