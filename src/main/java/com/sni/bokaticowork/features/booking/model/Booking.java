package com.sni.bokaticowork.features.booking.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "booking", indexes = {
        @Index(name = "idx_booking_number", columnList = "booking_number"),
        @Index(name = "idx_booking_resource_time", columnList = "resource_id,started_at,ended_at"),
        @Index(name = "idx_booking_owner", columnList = "owner_type,owner_code"),
        @Index(name = "idx_booking_status", columnList = "status")
})
@SQLDelete(sql = "UPDATE booking SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class Booking {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "booking_number", nullable = false, unique = true, length = 80)
    private String bookingNumber;

    @Column(name = "idempotency_key", unique = true, length = 180)
    private String idempotencyKey;

    @Column(name = "hold_number", length = 80)
    private String holdNumber;

    @Column(name = "recurrence_group_number", length = 80)
    private String recurrenceGroupNumber;

    /** Le lot multi-ressources dont la reservation fait partie · vide pour une reservation seule. */
    @Column(name = "batch_number", length = 80)
    private String batchNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false, foreignKey = @ForeignKey(name = "fk_booking_resource"))
    private Resource resource;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 60)
    private SubscriberType ownerType;

    @Column(name = "owner_code", nullable = false, length = 120)
    private String ownerCode;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone", length = 60)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private BookingStatus status = BookingStatus.DRAFT;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private LocalDateTime endedAt;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_unit", nullable = false, length = 40)
    private ResourceBookingUnit bookingUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 40)
    private BookingPaymentMode paymentMode;

    @Column(name = "subscription_number", length = 80)
    private String subscriptionNumber;

    @Column(name = "pass_number", length = 80)
    private String passNumber;

    @Column(name = "entitlement_code", length = 100)
    private String entitlementCode;

    @Column(name = "billable_number", length = 100)
    private String billableNumber;

    @Column(name = "unit_price", precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "subtotal_amount", precision = 19, scale = 4)
    private BigDecimal subtotalAmount;

    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount;

    /** Lot R1 : detail du calcul du prix (unite, tranches, regles et ajustements appliques), en JSON. */
    @Column(name = "price_trace", columnDefinition = "TEXT")
    private String priceTrace;

    /** Lot R5d : reglement d'usage accepte (quand, et quelle version). */
    @Column(name = "usage_rules_accepted_at")
    private java.time.Instant usageRulesAcceptedAt;

    @Column(name = "usage_rules_version", length = 20)
    private String usageRulesVersion;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "cancellation_reason", columnDefinition = "text")
    private String cancellationReason;

    /** Lot B7 : code du motif d'annulation (liste BOOKING_CANCEL_REASON), nul pour un texte libre. */
    @Column(name = "cancellation_reason_code", length = 60)
    private String cancellationReasonCode;

    /** Lot B7 : code du motif de refus (liste BOOKING_REJECT_REASON), nul pour un texte libre. */
    @Column(name = "rejection_reason_code", length = 60)
    private String rejectionReasonCode;

    /** Lot B3 : rendez-vous sans paiement, regle sur place (DIRECT, en attente de paiement jusqu'a l'echeance). */
    @lombok.Builder.Default
    @Column(name = "pay_on_site", nullable = false)
    private Boolean payOnSite = Boolean.FALSE;

    /** Lot B3 : rappel avant l'echeance de paiement deja envoye. */
    @Column(name = "payment_reminder_sent_at")
    private Instant paymentReminderSentAt;

    /** Lot B4 : prix calcule avant l'ajustement manuel. */
    @Column(name = "original_amount", precision = 19, scale = 2)
    private BigDecimal originalAmount;

    /** Lot B4 : APPLIED, PENDING (approbation) ou REJECTED ; nul sans ajustement. */
    @Column(name = "price_adjustment_status", length = 20)
    private String priceAdjustmentStatus;

    @Column(name = "price_adjustment_amount", precision = 19, scale = 2)
    private BigDecimal priceAdjustmentAmount;

    @Column(name = "price_adjustment_rate", precision = 7, scale = 2)
    private BigDecimal priceAdjustmentRate;

    @Column(name = "price_adjustment_reason", length = 600)
    private String priceAdjustmentReason;

    @Column(name = "price_adjustment_reason_code", length = 60)
    private String priceAdjustmentReasonCode;

    @Column(name = "price_adjustment_by", length = 180)
    private String priceAdjustmentBy;

    @Column(name = "price_adjustment_approval_number", length = 60)
    private String priceAdjustmentApprovalNumber;

    /** Lot B4 : la reservation n'attend l'approbation que pour son ajustement de prix. */
    @lombok.Builder.Default
    @Column(name = "approval_for_price_only", nullable = false)
    private Boolean approvalForPriceOnly = Boolean.FALSE;

    /** Lot B5 : prospect du CRM relie (appelant inconnu au telephone). */
    @Column(name = "lead_number", length = 80)
    private String leadNumber;

    /** Lot B5 : note de l'appel qui a donne lieu a la reservation. */
    @Column(name = "call_note", length = 1000)
    private String callNote;

    /** Lot B9a : reponse du client au rappel (COMING, CANCELLED). */
    @Column(name = "attendance_answer", length = 20)
    private String attendanceAnswer;

    @Column(name = "attendance_answered_at")
    private Instant attendanceAnsweredAt;

    /** Lot B9a : creneau libere faute d'arrivee. */
    @Column(name = "released_early_at")
    private Instant releasedEarlyAt;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;

    /**
     * L'organisation pour le compte de laquelle le membre reserve · choisie a la reservation en
     * mode « au choix ». Vide : la facture suit le mode du lien (automatique ou pour le membre).
     */
    @Column(name = "billing_company_code", length = 300)
    private String billingCompanyCode;

    /** Le membre de l'equipe qui a reserve hors preavis · vide pour une reservation ordinaire. */
    @Column(name = "policy_override_by", length = 150)
    private String policyOverrideBy;

    /** Lot B0 : qui a cree la reservation (utilisateur de la session ; vide pour le systeme). */
    @Column(name = "created_by", length = 150)
    private String createdBy;

    /** Lot B0 : canal de la reservation (portail, equipe, telephone, accueil, courriel, WhatsApp). */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20)
    private com.sni.bokaticowork.features.booking.enums.BookingChannel channel;

    /**
     * Titulaire du droit consomme quand ce n'est pas le titulaire de la reservation · l'organisation
     * dont le collaborateur occupe une place. Vide = le droit est celui de la reservation.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "entitlement_owner_type", length = 60)
    private com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType entitlementOwnerType;

    @Column(name = "entitlement_owner_code", length = 120)
    private String entitlementOwnerCode;

    /** La place du collaborateur sur l'abonnement de l'organisation. */
    @Column(name = "subscription_seat_id")
    private Long subscriptionSeatId;

    @Column(name = "check_in_token", unique = true, length = 120)
    private String checkInToken;

    @Column(name = "virtual_meeting_url")
    private String virtualMeetingUrl;

    @Column(name = "csat_score")
    private Integer csatScore;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    /** Lot B2b : nombre de reports et de changements de ressource deja faits. */
    @Column(name = "modification_count", nullable = false)
    @Builder.Default
    private Integer modificationCount = 0;

    @Column(name = "last_modified_at")
    private Instant lastModifiedAt;

    /** Lot B2b : derniere modification, lisible (« Report du … au … · +5 000 XAF »). */
    @Column(name = "last_modification", length = 300)
    private String lastModification;

    /** Lot B2 (ecart Bn-1) : l'unite a ete imposee par l'equipe ; le recalcul la garde. */
    @Column(name = "unit_forced", nullable = false)
    @Builder.Default
    private Boolean unitForced = Boolean.FALSE;

    /** Lot B2 (ecart Bn-2) : quantite retenue sur le droit a la confirmation (rendue telle quelle). */
    @Column(name = "entitlement_quantity", precision = 19, scale = 4)
    private java.math.BigDecimal entitlementQuantity;

    /** Lot B2 : minutes couvertes par le droit quand il ne couvre qu'une partie ; le reste est facture. */
    @Column(name = "covered_minutes")
    private Integer coveredMinutes;

    /** Lot B1 : le creneau est retenu (des la reservation en attente, plus seulement a la confirmation). */
    @Column(name = "slots_reserved", nullable = false)
    @Builder.Default
    private Boolean slotsReserved = Boolean.FALSE;

    /** Lot B1 (D1) : echeance de paiement d'une reservation en attente de paiement. */
    @Column(name = "payment_due_at")
    private LocalDateTime paymentDueAt;

    /** Lot B1 (ecart 2) : reglement constate (paiement recu, portefeuille) ; vide si rien n'est encaisse. */
    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "started_event_at")
    private Instant startedEventAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "approval_required", nullable = false)
    @Builder.Default
    private Boolean approvalRequired = Boolean.FALSE;

    @Column(name = "approved_by", length = 120)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_by", length = 120)
    private String rejectedBy;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "checked_out_at")
    private Instant checkedOutAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = Boolean.FALSE;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
