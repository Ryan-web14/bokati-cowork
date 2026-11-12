package com.sni.bokaticowork.features.payment.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/** Ordre de transfert interne a deux temps ({@code OTI-}) : envoi, puis reception comptee. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_transfer_order")
public class CashTransferOrder {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "transfer_number", nullable = false, unique = true, length = 100)
    private String transferNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 30)
    private com.sni.bokaticowork.features.payment.enums.CashTransferKind kind;

    @Column(name = "from_register", nullable = false, length = 100)
    private String fromRegister;

    @Column(name = "to_register", nullable = false, length = 100)
    private String toRegister;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "sent_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal sentAmount;

    @Column(name = "received_amount", precision = 19, scale = 4)
    private BigDecimal receivedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private com.sni.bokaticowork.features.payment.enums.CashTransferStatus status;

    @Column(name = "carrier", length = 180)
    private String carrier;

    @Column(name = "sent_by", nullable = false, length = 180)
    private String sentBy;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "from_session", length = 100)
    private String fromSession;

    @Column(name = "out_movement", length = 100)
    private String outMovement;

    @Column(name = "received_by", length = 180)
    private String receivedBy;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "to_session", length = 100)
    private String toSession;

    @Column(name = "in_movement", length = 100)
    private String inMovement;

    @Column(name = "receipt_count", length = 100)
    private String receiptCount;

    @Column(name = "review_number", length = 100)
    private String reviewNumber;

    @Column(name = "cancelled_by", length = 180)
    private String cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "transit_alerted_at")
    private Instant transitAlertedAt;

    @Column(name = "note", length = 1000)
    private String note;

    @PrePersist
    void prePersist() {
        if (sentAt == null) {
            sentAt = Instant.now();
        }
    }
}
