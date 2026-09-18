package com.sni.bokaticowork.features.subscription.subscription.pass.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassTransferStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Changement definitif de titulaire.
 *
 * <p>Le transfert exige l'acceptation du destinataire, et ce n'est pas une politesse : recevoir un
 * pass cree des obligations, et un pass pousse a quelqu'un qui n'en veut pas encombre son espace
 * sans qu'il puisse s'en defaire.</p>
 *
 * <p>Un pass n'a qu'un transfert en cours. Deux demandes simultanees vers deux destinataires
 * donneraient le pass a celui qui accepte le second, ce que l'emetteur n'a jamais voulu.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "pass_transfer", indexes = {
        @Index(name = "idx_pass_transfer_pass", columnList = "pass_id,status"),
        @Index(name = "idx_pass_transfer_recipient", columnList = "to_owner_type,to_owner_code,status")
})
public class PassTransfer {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "transfer_number", nullable = false, unique = true, length = 100)
    private String transferNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pass_id", nullable = false)
    private Pass pass;

    @Column(name = "from_owner_type", nullable = false, length = 60)
    private String fromOwnerType;

    @Column(name = "from_owner_code", nullable = false, length = 120)
    private String fromOwnerCode;

    @Column(name = "to_owner_type", nullable = false, length = 60)
    private String toOwnerType;

    @Column(name = "to_owner_code", nullable = false, length = 120)
    private String toOwnerCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private PassTransferStatus status = PassTransferStatus.PENDING_ACCEPTANCE;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "transfer_fee", precision = 19, scale = 4)
    private BigDecimal transferFee;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "requested_by", length = 120)
    private String requestedBy;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @PrePersist
    public void prePersist() {
        if (requestedAt == null) {
            requestedAt = Instant.now();
        }
    }
}
