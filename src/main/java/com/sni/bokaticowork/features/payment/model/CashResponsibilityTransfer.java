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

/**
 * Transfert de responsabilite d'une caisse : la caisse change de titulaire, pour une periode ou
 * definitivement. C'est le PV de remise de caisse ({@code PRC-}).
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_responsibility_transfer")
public class CashResponsibilityTransfer {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "transfer_number", nullable = false, unique = true, length = 100)
    private String transferNumber;

    @Column(name = "register_code", nullable = false, length = 100)
    private String registerCode;

    @Column(name = "from_user", length = 180)
    private String fromUser;

    @Column(name = "to_user", nullable = false, length = 180)
    private String toUser;

    @Column(name = "reason_code", nullable = false, length = 60)
    private String reasonCode;

    @Column(name = "reason_text", length = 500)
    private String reasonText;

    @Column(name = "permanent", nullable = false)
    private Boolean permanent;

    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "counted_amount", precision = 19, scale = 4)
    private BigDecimal countedAmount;

    @Column(name = "expected_amount", precision = 19, scale = 4)
    private BigDecimal expectedAmount;

    @Column(name = "witness", length = 180)
    private String witness;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "pending_items", columnDefinition = "jsonb")
    private String pendingItems;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private com.sni.bokaticowork.features.payment.enums.CashResponsibilityTransferStatus status;

    @Column(name = "outgoing_signed_by", length = 180)
    private String outgoingSignedBy;

    @Column(name = "outgoing_signed_at")
    private Instant outgoingSignedAt;

    @Column(name = "incoming_signed_by", length = 180)
    private String incomingSignedBy;

    @Column(name = "incoming_signed_at")
    private Instant incomingSignedAt;

    @Column(name = "created_by", nullable = false, length = 180)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
