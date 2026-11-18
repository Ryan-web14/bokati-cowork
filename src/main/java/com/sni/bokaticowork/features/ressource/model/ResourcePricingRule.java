package com.sni.bokaticowork.features.ressource.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.ressource.enums.ResourcePriceAdjustmentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "resource_pricing_rule")
public class ResourcePricingRule {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    /**
     * Le numero court de la regle · c'est lui que l'API expose sous le nom {@code id}.
     *
     * <p>L'identifiant genere depasse 2^53 et JavaScript l'arrondit · l'interface renvoyait ensuite
     * un identifiant qui ne designait aucune regle. Celui-ci vient d'une sequence de la base et
     * reste petit. Attribue par la base a l'insertion, jamais par le code.</p>
     */
    @org.hibernate.annotations.Generated
    @Column(name = "short_id", insertable = false, updatable = false)
    private Long shortId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false, foreignKey = @ForeignKey(name = "resource_pricing_rule_resource_fk"))
    private Resource resource;

    @Column(name = "unit", nullable = false)
    @Enumerated(EnumType.STRING)
    private ResourceBookingUnit resourceBookingUnit;

    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Min(0)
    @Column(name = "price")
    private Integer price;

    @Column(name = "label")
    private String label;

    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    @Column(name = "starts_at")
    private LocalTime startsAt;

    @Column(name = "ends_at")
    private LocalTime endsAt;

    @Column(name = "adjustment_type")
    @Enumerated(EnumType.STRING)
    private ResourcePriceAdjustmentType adjustmentType;

    @jakarta.validation.constraints.Min(0)
    @Column(name = "adjustment_value")
    private Integer adjustmentValue;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "last_minute_minutes")
    private Integer lastMinuteMinutes;


    /** Lot R1 : jours d'application (1 = lundi ... 7 = dimanche), plusieurs separes par des virgules ; remplace dayOfWeek. */
    @Column(name = "days_of_week", length = 20)
    private String daysOfWeek;

    /** Lot R1 : segment de client vise : PUBLIC, MEMBER, SUBSCRIBER, COMPANY, PARTNER ; vide = tous. */
    @Column(name = "customer_segment", length = 30)
    private String customerSegment;

    /** Lot R1 : plan d'abonnement vise (code) ; vide = tous. */
    @Column(name = "plan_code", length = 60)
    private String planCode;

    @Column(name = "min_participants")
    private Integer minParticipants;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "min_duration_minutes")
    private Integer minDurationMinutes;

    @Column(name = "max_duration_minutes")
    private Integer maxDurationMinutes;

    /** Lot R1 : delai minimal avant le debut (reservation anticipee), en minutes. */
    @Column(name = "min_lead_minutes")
    private Integer minLeadMinutes;

    /** Lot R1 : canal vise : PORTAL ou TEAM ; vide = tous. */
    @Column(name = "channel", length = 20)
    private String channel;

    /** Lot R5a : ONLY = jours feries seulement, EXCLUDED = hors jours feries, vide = tous les jours. */
    @Column(name = "holiday_mode", length = 10)
    private String holidayMode;

    /** Lot R1 : ajustement cumulable avec les autres (sinon, seul le meilleur s'applique). */
    @Builder.Default
    @Column(name = "stackable", nullable = false)
    private Boolean stackable = Boolean.FALSE;

    /** Lot R1 : prix par participant, meme pour une ressource louee entiere. */
    @Builder.Default
    @Column(name = "per_participant", nullable = false)
    private Boolean perParticipant = Boolean.FALSE;

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = Boolean.FALSE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

}
