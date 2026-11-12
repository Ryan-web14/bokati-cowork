package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Bordereau de remise en banque : especes et cheques. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "bank_deposit_slip")
public class BankDepositSlip {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "number", nullable = false)
    private String number;

    @Column(name = "bank_account_code", nullable = false)
    private String bankAccountCode;

    @Column(name = "register_code")
    private String registerCode;

    @Builder.Default
    @Column(name = "cash_amount", nullable = false)
    private java.math.BigDecimal cashAmount = java.math.BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "cheque_total", nullable = false)
    private java.math.BigDecimal chequeTotal = java.math.BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "PREPARED";

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private java.time.Instant createdAt;

    @Column(name = "deposited_by")
    private String depositedBy;

    @Column(name = "deposited_at")
    private java.time.Instant depositedAt;

    @Column(name = "credited_at")
    private java.time.Instant creditedAt;

    @Column(name = "credit_entry")
    private String creditEntry;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
    }
}
