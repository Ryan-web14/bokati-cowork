package com.sni.bokaticowork.features.payment.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Nature d'operation de caisse : ce que le caissier choisit (« Achat de fournitures de bureau »).
 * Le type technique, le sens, le compte, le justificatif exige et les plafonds en decoulent.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_operation_nature")
public class CashOperationNature {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 60)
    private String code;

    @Column(name = "label", nullable = false, length = 255)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "family", nullable = false, length = 30)
    private com.sni.bokaticowork.features.payment.enums.CashNatureFamily family;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 3)
    private com.sni.bokaticowork.features.payment.enums.CashFlowDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 40)
    private com.sni.bokaticowork.features.payment.enums.CashMovementType movementType;

    /** Compte de charge ou de produit (comptabilite, lot F3). */
    @Column(name = "account_number", length = 30)
    private String accountNumber;

    @Column(name = "vat_code", length = 30)
    private String vatCode;

    @Builder.Default
    @Column(name = "document_required", nullable = false)
    private Boolean documentRequired = Boolean.FALSE;

    /** Delai de remise du justificatif, en jours. */
    @Column(name = "justification_days")
    private Integer justificationDays;

    @Column(name = "default_section", length = 60)
    private String defaultSection;

    /** Au-dela, l'operation passe par une demande de fonds (decision 7b : 50 000 XAF). */
    @Column(name = "max_per_operation", precision = 19, scale = 4)
    private BigDecimal maxPerOperation;

    @Builder.Default
    @Column(name = "prior_request_required", nullable = false)
    private Boolean priorRequestRequired = Boolean.FALSE;

    /** Au-dela, l'operation attend un second visa ; zero : toujours ; vide : seuil de la caisse. */
    @Column(name = "double_visa_threshold", precision = 19, scale = 4)
    private BigDecimal doubleVisaThreshold;

    @Column(name = "keywords", length = 500)
    private String keywords;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Builder.Default
    @Column(name = "usage_count", nullable = false)
    private Long usageCount = 0L;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Builder.Default
    @Column(name = "system_value", nullable = false)
    private Boolean systemValue = Boolean.FALSE;

    @Column(name = "updated_by", length = 180)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Lot F20 : nature a periode (loyer, assurance, licence...), sa duree et sa methode proposees. */
    @Builder.Default
    @Column(name = "periodic", nullable = false)
    private Boolean periodic = Boolean.FALSE;

    @Column(name = "default_duration_months")
    private Integer defaultDurationMonths;

    @Column(name = "default_method", length = 20)
    private String defaultMethod;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
