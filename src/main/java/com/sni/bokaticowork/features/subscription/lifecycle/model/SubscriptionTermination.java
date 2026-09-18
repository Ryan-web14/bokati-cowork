package com.sni.bokaticowork.features.subscription.lifecycle.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import java.util.ArrayList;
import java.util.List;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Une resiliation · un preavis qui court, des frais eventuels, une liste de sortie.
 *
 * <p>Elle est demandee, acceptee, parfois retractee, puis achevee a sa date d'effet quand la liste
 * de sortie est vide. Elle ne supprime rien : l'abonnement est annule par elle, pas a sa place.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "subscription_termination")
public class SubscriptionTermination {

    public enum Status {
        REQUESTED, ACCEPTED, RETRACTED, COMPLETED;

        public boolean open() {
            return this == REQUESTED || this == ACCEPTED;
        }
    }

    public enum Channel {
        STAFF, PORTAL, LETTER, EMAIL
    }

    public enum ReasonCategory {
        RELOCATION, COST, SERVICE_QUALITY, BUSINESS_CLOSURE, NO_LONGER_NEEDED, COMPETITOR, NON_PAYMENT, OTHER
    }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "termination_code", nullable = false, unique = true, length = 40)
    private String terminationCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "requested_by", nullable = false, length = 120)
    private String requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    @Builder.Default
    private Channel channel = Channel.STAFF;

    @Column(name = "notice_period_days", nullable = false)
    private Integer noticePeriodDays;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_category", nullable = false, length = 40)
    private ReasonCategory reasonCategory;

    @Column(name = "early_termination", nullable = false)
    @Builder.Default
    private Boolean earlyTermination = Boolean.FALSE;

    @Column(name = "remaining_commitment_months", nullable = false)
    @Builder.Default
    private Integer remainingCommitmentMonths = 0;

    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "fee_waived", nullable = false)
    @Builder.Default
    private Boolean feeWaived = Boolean.FALSE;

    @Column(name = "fee_waived_by", length = 120)
    private String feeWaivedBy;

    @Column(name = "fee_waived_reason", columnDefinition = "TEXT")
    private String feeWaivedReason;

    @Column(name = "fee_billable_number", length = 100)
    private String feeBillableNumber;

    /** Les jours entre la fin de la periode en cours et la date d'effet · factures au prorata. */
    @Column(name = "bridging_billable_number", length = 100)
    private String bridgingBillableNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.REQUESTED;

    @Column(name = "accepted_by", length = 120)
    private String acceptedBy;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "retracted_by", length = 120)
    private String retractedBy;

    @Column(name = "retracted_at")
    private Instant retractedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "termination", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<SubscriptionExitItem> exitItems = new ArrayList<>();

    /** Ce qui est encore du au titre de la rupture · zero si dispense. */
    public BigDecimal feeDue() {
        return Boolean.TRUE.equals(feeWaived) ? BigDecimal.ZERO : feeAmount;
    }

    public boolean exitChecklistClear() {
        return exitItems.stream().noneMatch(i -> Boolean.TRUE.equals(i.getMandatory()) && i.getStatus() == SubscriptionExitItem.Status.PENDING);
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
