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

/** Mise sous scelles du fonds d'une caisse (PV PVS) et son ouverture. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_seal")
public class CashSeal {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "seal_number", nullable = false, unique = true, length = 100)
    private String sealNumber;

    @Column(name = "register_code", nullable = false, length = 100)
    private String registerCode;

    @Column(name = "envelope_number", nullable = false, length = 100)
    private String envelopeNumber;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "count_number", nullable = false, length = 100)
    private String countNumber;

    @Column(name = "sealed_by", nullable = false, length = 180)
    private String sealedBy;

    @Column(name = "witness", nullable = false, length = 180)
    private String witness;

    @Column(name = "deposited_in", length = 255)
    private String depositedIn;

    @Column(name = "unsealed_at")
    private Instant unsealedAt;

    @Column(name = "unsealed_by", length = 180)
    private String unsealedBy;

    @Column(name = "unseal_witness", length = 180)
    private String unsealWitness;

    @Column(name = "unseal_count", length = 100)
    private String unsealCount;

    @Column(name = "unseal_amount", precision = 19, scale = 4)
    private BigDecimal unsealAmount;

    @Column(name = "absence_number", length = 100)
    private String absenceNumber;

    @Column(name = "sealed_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
