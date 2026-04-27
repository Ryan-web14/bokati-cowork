package com.sni.bokaticowork.features.booking.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.booking.enums.BookingRecurrenceFrequency;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "booking_recurrence_group", indexes = {
        @Index(name = "idx_booking_recurrence_group_number", columnList = "group_number")
})
public class BookingRecurrenceGroup {
    @Id
    @IdGeneration
    private Long id;

    @Column(name = "group_number", nullable = false, unique = true, length = 80)
    private String groupNumber;

    @Column(name = "resource_code", nullable = false, length = 100)
    private String resourceCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 60)
    private SubscriberType ownerType;

    @Column(name = "owner_code", nullable = false, length = 120)
    private String ownerCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 40)
    private BookingRecurrenceFrequency frequency;

    @Column(name = "interval_value", nullable = false)
    private Integer intervalValue;

    @Column(name = "occurrences", nullable = false)
    private Integer occurrences;

    @Column(name = "first_start_at", nullable = false)
    private LocalDateTime firstStartAt;

    @Column(name = "first_end_at", nullable = false)
    private LocalDateTime firstEndAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
