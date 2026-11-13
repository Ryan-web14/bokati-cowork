package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Affaire : une vente a un client, du premier devis au dernier paiement (lot B11). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "billing_deal")
public class BillingDeal {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "customer_type", length = 30)
    private String customerType;

    /** Vide pour un prospect issu du CRM, pas encore client. */
    @Column(name = "customer_code", length = 80)
    private String customerCode;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    /** Responsable commercial (identifiant de connexion). */
    @Column(name = "owner", length = 180)
    private String owner;

    @Column(name = "owner_name")
    private String ownerName;

    @Column(name = "expected_amount", precision = 19, scale = 4)
    private BigDecimal expectedAmount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "probability")
    private Integer probability;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 20)
    private com.sni.bokaticowork.features.billing.enums.BillingDealStage stage;

    @Column(name = "lost_reason", length = 1000)
    private String lostReason;

    @Column(name = "expected_close_date")
    private LocalDate expectedCloseDate;

    @Column(name = "closed_at")
    private Instant closedAt;

    /** MANUAL, CRM (depuis une opportunite) ou AUTO (au premier devis du client). */
    @Column(name = "source", nullable = false, length = 20)
    private String source;

    @Column(name = "crm_opportunity_ref", length = 60)
    private String crmOpportunityRef;

    /** Section analytique de l'affaire, pour sa rentabilite (lot F9). */
    @Column(name = "analytic_section", length = 60)
    private String analyticSection;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "created_by", length = 180)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
