package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Cheque recu, suivi jusqu'a l'encaissement ou au rejet. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "bank_cheque")
public class BankCheque {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "transaction_number", nullable = false)
    private String transactionNumber;

    @Column(name = "cheque_number")
    private String chequeNumber;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "drawer")
    private String drawer;

    @Column(name = "amount", nullable = false)
    private java.math.BigDecimal amount;

    @Column(name = "register_code")
    private String registerCode;

    @Column(name = "session_number")
    private String sessionNumber;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "RECEIVED";

    @Column(name = "slip_number")
    private String slipNumber;

    @Column(name = "deposited_at")
    private java.time.Instant depositedAt;

    @Column(name = "cleared_at")
    private java.time.Instant clearedAt;

    @Column(name = "rejected_at")
    private java.time.Instant rejectedAt;

    @Column(name = "rejected_reason")
    private String rejectedReason;

    @Column(name = "received_at", nullable = false)
    private java.time.Instant receivedAt;

    @PrePersist
    void prePersist() {
    }
}
