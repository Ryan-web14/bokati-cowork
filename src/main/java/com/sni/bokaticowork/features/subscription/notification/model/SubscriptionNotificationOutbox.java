package com.sni.bokaticowork.features.subscription.notification.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.notification.enums.SubscriptionNotificationChannel;
import com.sni.bokaticowork.features.subscription.notification.enums.SubscriptionNotificationStatus;
import com.sni.bokaticowork.features.subscription.notification.enums.SubscriptionNotificationType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "subscription_notification_outbox", indexes = {
        @Index(name = "idx_subscription_notification_status_schedule", columnList = "status,scheduled_at"),
        @Index(name = "idx_subscription_notification_subscription", columnList = "subscription_id,status"),
        @Index(name = "idx_subscription_notification_owner", columnList = "owner_type,owner_code,status")
})
public class SubscriptionNotificationOutbox {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "notification_number", nullable = false, unique = true, length = 100)
    private String notificationNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", foreignKey = @ForeignKey(name = "fk_subscription_notification_subscription"))
    private Subscription subscription;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", length = 60)
    private SubscriberType ownerType;

    @Column(name = "owner_code", length = 120)
    private String ownerCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 80)
    private SubscriptionNotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 40)
    private SubscriptionNotificationChannel channel;

    @Column(name = "recipient", nullable = false, length = 255)
    private String recipient;

    @Column(name = "subject", length = 255)
    private String subject;

    @Column(name = "body", columnDefinition = "text")
    private String body;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "payload_json", columnDefinition = "jsonb")
    private String payloadJson;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private SubscriptionNotificationStatus status = SubscriptionNotificationStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (scheduledAt == null) {
            scheduledAt = Instant.now();
        }
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
