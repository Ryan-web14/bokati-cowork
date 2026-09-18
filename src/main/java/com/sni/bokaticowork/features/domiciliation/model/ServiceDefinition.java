package com.sni.bokaticowork.features.domiciliation.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Un service du catalogue, au-dessus des plans.
 *
 * <p>Le plan dit ce a quoi on a droit ; le service dit ce qu'on doit faire pour le titulaire, avec
 * ses obligations propres. Une domiciliation exige un contrat, un niveau de verification et des
 * obligations reglementaires ; une numerisation de courrier se facture a l'acte. Ces differences
 * sont des donnees du service, pas des cas particuliers du code.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "service_definition")
public class ServiceDefinition {

    public enum Category {
        DOMICILIATION, MAIL_HANDLING, PHONE_ANSWERING, STORAGE, MEETING_ROOM, COWORKING_ACCESS, PRIVATE_OFFICE,
        ADMIN_SUPPORT, LEGAL_SUPPORT, ACCOUNTING_SUPPORT, IT_SUPPORT, EVENT_SPACE, PARKING, LOCKER, OTHER
    }

    public enum DeliveryMode {
        CONTINUOUS, ON_DEMAND, SCHEDULED, METERED
    }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 60)
    private String code;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_category", nullable = false, length = 40)
    private Category serviceCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", nullable = false, length = 20)
    @Builder.Default
    private DeliveryMode deliveryMode = DeliveryMode.CONTINUOUS;

    @Column(name = "requires_contract", nullable = false)
    @Builder.Default
    private Boolean requiresContract = Boolean.FALSE;

    @Column(name = "requires_kyc_level")
    private Integer requiresKycLevel;

    @Column(name = "requires_physical_resource", nullable = false)
    @Builder.Default
    private Boolean requiresPhysicalResource = Boolean.FALSE;

    @Column(name = "has_regulatory_obligations", nullable = false)
    @Builder.Default
    private Boolean hasRegulatoryObligations = Boolean.FALSE;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_billing_cycle", length = 20)
    private BillingCycle defaultBillingCycle;

    @Column(name = "default_notice_period_days")
    private Integer defaultNoticePeriodDays;

    @Column(name = "default_commitment_months")
    private Integer defaultCommitmentMonths;

    @Column(name = "unit_price", precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "XAF";

    /** Pour un service a l'acte · le code de droit sous lequel chaque acte est enregistre et facture. */
    @Column(name = "usage_entitlement_code", length = 60)
    private String usageEntitlementCode;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
