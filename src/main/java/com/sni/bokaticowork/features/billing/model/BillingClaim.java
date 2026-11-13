package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Lot C11 : reclamation d'un client sur une facture. Statuts : OPEN, ACCEPTED, PARTIALLY_ACCEPTED,
 * REJECTED, WITHDRAWN. Canal : STAFF (saisie par l'equipe) ou PORTAL (espace client).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "billing_claim")
public class BillingClaim {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "claim_number", nullable = false, unique = true, length = 40)
    private String claimNumber;

    @Column(name = "document_number", nullable = false, length = 80)
    private String documentNumber;

    @Column(name = "customer_type", length = 60)
    private String customerType;

    @Column(name = "customer_code", length = 120)
    private String customerCode;

    @Column(name = "reason_code", nullable = false, length = 40)
    private String reasonCode;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    /** Numeros des lignes visees, separes par des virgules (vide : toute la facture). */
    @Column(name = "line_orders", length = 500)
    private String lineOrders;

    @Column(name = "requested_amount", precision = 19, scale = 4)
    private BigDecimal requestedAmount;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    @Column(name = "opened_by", length = 180)
    private String openedBy;

    @Column(name = "granted_amount", precision = 19, scale = 4)
    private BigDecimal grantedAmount;

    @Column(name = "credit_note_number", length = 80)
    private String creditNoteNumber;

    @Column(name = "decision_comment", length = 2000)
    private String decisionComment;

    @Column(name = "decided_by", length = 180)
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
