package com.sni.bokaticowork.features.subscription.lifecycle.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Un devis · un abonnement promis, pas encore souscrit.
 *
 * <p>Il fige un plan, un rythme, un prix (catalogue ou negocie), un engagement et une validite. A
 * l'acceptation il devient un abonnement · et si le prix n'est pas celui du catalogue, une
 * derivation, qui garde la trace de la negociation.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "subscription_quote")
public class SubscriptionQuote {

    public enum Status {
        DRAFT, SENT, ACCEPTED, REJECTED, EXPIRED, CONVERTED;

        public boolean open() {
            return this == DRAFT || this == SENT;
        }
    }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "quote_number", nullable = false, unique = true, length = 40)
    private String quoteNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscriber_type", nullable = false, length = 40)
    private SubscriberType subscriberType;

    @Column(name = "subscriber_code", nullable = false, length = 120)
    private String subscriberCode;

    @Column(name = "subscriber_name", length = 200)
    private String subscriberName;

    @Column(name = "subscriber_email", length = 200)
    private String subscriberEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_version_id", nullable = false)
    private PlanVersion planVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 40)
    private BillingCycle billingCycle;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "catalogue_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal cataloguePrice;

    @Column(name = "quoted_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal quotedPrice;

    @Column(name = "setup_fee", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal setupFee = BigDecimal.ZERO;

    @Column(name = "commitment_months", nullable = false)
    @Builder.Default
    private Integer commitmentMonths = 0;

    @Column(name = "trial_days", nullable = false)
    @Builder.Default
    private Integer trialDays = 0;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "services_json", columnDefinition = "jsonb")
    private String servicesJson;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.DRAFT;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Column(name = "prepared_by", nullable = false, length = 120)
    private String preparedBy;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "accepted_by", length = 120)
    private String acceptedBy;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "converted_subscription_number", length = 100)
    private String convertedSubscriptionNumber;

    @Column(name = "derivation_code", length = 40)
    private String derivationCode;

    /** La proposition commerciale du CRM dont ce devis est la traduction, s'il y en a une. */
    @Column(name = "proposal_number", length = 60)
    private String proposalNumber;

    public boolean negotiated() {
        return quotedPrice != null && cataloguePrice != null && quotedPrice.compareTo(cataloguePrice) != 0;
    }

    public boolean expiredOn(LocalDate day) {
        return validUntil != null && day.isAfter(validUntil);
    }

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
