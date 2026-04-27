package com.sni.bokaticowork.features.booking.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.booking.enums.BookingNotificationStatus;
import com.sni.bokaticowork.features.booking.enums.BookingNotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "booking_notification_outbox", indexes = {
        @Index(name = "idx_booking_notification_number", columnList = "notification_number"),
        @Index(name = "idx_booking_notification_due", columnList = "status,scheduled_at")
})
public class BookingNotificationOutbox {
    @Id
    @IdGeneration
    private Long id;

    @Column(name = "notification_number", nullable = false, unique = true, length = 80)
    private String notificationNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, foreignKey = @ForeignKey(name = "fk_booking_notification_booking"))
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 60)
    private BookingNotificationType notificationType;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "body", columnDefinition = "text")
    private String body;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private BookingNotificationStatus status;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
