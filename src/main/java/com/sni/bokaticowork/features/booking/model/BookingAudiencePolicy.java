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
@Table(name = "booking_audience_policy", indexes = {
        @Index(name = "idx_booking_audience_policy_audience", columnList = "audience_type"),
        @Index(name = "idx_booking_audience_policy_resource_type", columnList = "resource_type_code")
})
public class BookingAudiencePolicy {
    @Id
    @IdGeneration
    private Long id;

    @Column(name = "policy_number", nullable = false, unique = true, length = 80)
    private String policyNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false, length = 60)
    private SubscriberType audienceType;

    @Column(name = "resource_type_code", length = 100)
    private String resourceTypeCode;

    @Column(name = "resource_group_code", length = 100)
    private String resourceGroupCode;

    @Column(name = "approval_required", nullable = false)
    @Builder.Default
    private Boolean approvalRequired = Boolean.FALSE;

    @Column(name = "max_active_bookings")
    private Integer maxActiveBookings;

    @Column(name = "max_bookings_per_day")
    private Integer maxBookingsPerDay;

    @Column(name = "max_bookings_per_week")
    private Integer maxBookingsPerWeek;

    @Column(name = "max_bookings_per_month")
    private Integer maxBookingsPerMonth;

    @Column(name = "max_no_shows_per_month")
    private Integer maxNoShowsPerMonth;

    @Column(name = "min_booking_notice_minutes")
    private Integer minBookingNoticeMinutes;

    @Column(name = "max_booking_duration_minutes")
    private Integer maxBookingDurationMinutes;

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
