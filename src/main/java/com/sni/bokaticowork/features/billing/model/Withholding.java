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
import java.time.LocalDate;

/**
 * Lot C6 : retenue a la source operee par un client sur le reglement d'une facture. Elle solde la
 * facture a hauteur de son montant ; l'attestation de retenue est attendue puis rattachee.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "billing_withholding")
public class Withholding {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "withholding_number", nullable = false, unique = true, length = 40)
    private String withholdingNumber;

    @Column(name = "document_number", nullable = false, length = 80)
    private String documentNumber;

    @Column(name = "type_code", nullable = false, length = 40)
    private String typeCode;

    @Column(name = "account", nullable = false, length = 20)
    private String account;

    @Column(name = "base_amount", precision = 19, scale = 4)
    private BigDecimal baseAmount;

    @Column(name = "rate", precision = 9, scale = 4)
    private BigDecimal rate;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "customer_type", length = 60)
    private String customerType;

    @Column(name = "customer_code", length = 120)
    private String customerCode;

    @Column(name = "certificate_reference", length = 120)
    private String certificateReference;

    /** Reference du fichier de l'attestation deposee en GED. */
    @Column(name = "certificate_document", length = 120)
    private String certificateDocument;

    @Column(name = "certificate_at")
    private Instant certificateAt;

    /** RECORDED ou CANCELLED. */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "withheld_on", nullable = false)
    private LocalDate withheldOn;

    @Column(name = "recorded_by", length = 180)
    private String recordedBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by", length = 180)
    private String cancelledBy;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

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
