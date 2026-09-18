package com.sni.bokaticowork.features.payment.control.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Une action administrative sur un portefeuille, tracee.
 *
 * <p>Deux principes, tenus par la base plutot que par la consigne : aucune action sans motif, et un
 * second visa sur les operations sensibles. Le premier est un {@code NOT NULL} ; le second est un
 * statut qui ne devient {@code EXECUTED} que sous la main d'un autre que celui qui a demande.</p>
 *
 * <p>Une action ne se reecrit pas. Une erreur se corrige par une nouvelle action, qui dit qu'elle
 * corrige la precedente · c'est ainsi que la trace reste une trace.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "wallet_admin_action")
public class WalletAdminAction {

    public enum Type {
        TOPUP, DEBIT, ADJUST, REVERSE, SUSPEND, UNSUSPEND, RESET_PIN, RAISE_LIMIT, CLOSE, REVIEW_FLAG
    }

    public enum Status {
        /** Attend le second visa · rien n'a ete execute. */
        PENDING_APPROVAL,
        EXECUTED,
        REJECTED
    }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "action_number", nullable = false, unique = true, length = 100)
    private String actionNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private WalletAccount wallet;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private Type actionType;

    @Column(name = "amount", precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    /** Ecriture visee par une contre-passation, signalement vise par une revue, etc. */
    @Column(name = "reference", length = 180)
    private String reference;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private Status status = Status.EXECUTED;

    @Column(name = "requested_by", nullable = false, length = 120)
    private String requestedBy;

    @Column(name = "approved_by", length = 120)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_by", length = 120)
    private String rejectedBy;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "executed_at")
    private Instant executedAt;

    /** Ce que l'action a produit · numero d'ecriture, de signalement, etc. */
    @Column(name = "result_reference", length = 180)
    private String resultReference;

    @Column(name = "payload_json", columnDefinition = "text")
    private String payloadJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}
