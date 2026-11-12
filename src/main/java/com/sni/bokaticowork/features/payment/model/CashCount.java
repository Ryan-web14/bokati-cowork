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

/** Un comptage : jamais modifie, un nouveau comptage le remplace et l'ancien reste visible. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_count")
public class CashCount {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "count_number", nullable = false, unique = true, length = 100)
    private String countNumber;

    @Column(name = "register_code", nullable = false, length = 100)
    private String registerCode;

    @Column(name = "session_number", length = 100)
    private String sessionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "moment", nullable = false, length = 30)
    private com.sni.bokaticowork.features.payment.enums.CashCountMoment moment;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "total", nullable = false, precision = 19, scale = 4)
    private BigDecimal total;

    @Builder.Default
    @Column(name = "damaged_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal damagedTotal = BigDecimal.ZERO;

    @Column(name = "with_lines", nullable = false)
    private Boolean withLines;

    @Column(name = "expected_amount", precision = 19, scale = 4)
    private BigDecimal expectedAmount;

    @Column(name = "variance", precision = 19, scale = 4)
    private BigDecimal variance;

    @Column(name = "counted_by", nullable = false, length = 180)
    private String countedBy;

    @Column(name = "second_counter", length = 180)
    private String secondCounter;

    @Column(name = "witness", length = 180)
    private String witness;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "VALID";

    @Column(name = "superseded_by", length = 100)
    private String supersededBy;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
