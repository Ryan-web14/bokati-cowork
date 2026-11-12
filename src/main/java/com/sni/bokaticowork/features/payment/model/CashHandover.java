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

/** Passation (releve) entre deux caissiers : PV PVP signe par les deux. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_handover")
public class CashHandover {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "handover_number", nullable = false, unique = true, length = 100)
    private String handoverNumber;

    @Column(name = "register_code", nullable = false, length = 100)
    private String registerCode;

    @Column(name = "from_session", nullable = false, length = 100)
    private String fromSession;

    @Column(name = "to_session", length = 100)
    private String toSession;

    @Column(name = "outgoing", nullable = false, length = 180)
    private String outgoing;

    @Column(name = "incoming", nullable = false, length = 180)
    private String incoming;

    @Column(name = "outgoing_count", nullable = false, length = 100)
    private String outgoingCount;

    @Column(name = "incoming_count", length = 100)
    private String incomingCount;

    @Column(name = "outgoing_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal outgoingAmount;

    @Column(name = "incoming_amount", precision = 19, scale = 4)
    private BigDecimal incomingAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private com.sni.bokaticowork.features.payment.enums.CashHandoverStatus status;

    @Column(name = "dispute_reason", length = 1000)
    private String disputeReason;

    @Column(name = "outgoing_signed_at")
    private Instant outgoingSignedAt;

    @Column(name = "incoming_signed_at")
    private Instant incomingSignedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
