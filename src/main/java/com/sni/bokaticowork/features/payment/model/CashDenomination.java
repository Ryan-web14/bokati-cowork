package com.sni.bokaticowork.features.payment.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Coupure (billet ou piece) d'une devise ; une coupure retiree se desactive sans perdre l'historique. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_denomination")
public class CashDenomination {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "value", nullable = false, precision = 19, scale = 4)
    private BigDecimal value;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 10)
    private com.sni.bokaticowork.features.payment.enums.CashDenominationKind kind;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "active_from", nullable = false)
    private LocalDate activeFrom;

    @Column(name = "active_to")
    private LocalDate activeTo;
}
