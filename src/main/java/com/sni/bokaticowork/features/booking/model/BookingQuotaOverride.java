package com.sni.bokaticowork.features.booking.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "booking_quota_override", indexes = {
        @Index(name = "idx_booking_quota_override_owner", columnList = "owner_type,owner_code"),
        @Index(name = "idx_booking_quota_override_active", columnList = "active,valid_from,valid_until")
})
public class BookingQuotaOverride {
    @Id
    @IdGeneration
    private Long id;

    @Column(name = "override_number", nullable = false, unique = true, length = 80)
    private String overrideNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 60)
    private SubscriberType ownerType;

    @Column(name = "owner_code", nullable = false, length = 120)
    private String ownerCode;

    @Column(name = "resource_code", length = 100)
    private String resourceCode;

    @Column(name = "extra_active_bookings")
    private Integer extraActiveBookings;

    @Column(name = "extra_bookings_per_day")
    private Integer extraBookingsPerDay;

    @Column(name = "extra_bookings_per_week")
    private Integer extraBookingsPerWeek;

    @Column(name = "extra_bookings_per_month")
    private Integer extraBookingsPerMonth;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "approved_by", length = 120)
    private String approvedBy;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
