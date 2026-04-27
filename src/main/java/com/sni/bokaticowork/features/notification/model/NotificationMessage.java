package com.sni.bokaticowork.features.notification.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.notification.enums.NotificationChannel;
import com.sni.bokaticowork.features.notification.enums.NotificationDeliveryStatus;
import com.sni.bokaticowork.features.notification.enums.NotificationRecipientType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
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
@Table(name = "notification_message", indexes = {
        @Index(name = "idx_notification_message_number", columnList = "notification_number"),
        @Index(name = "idx_notification_message_recipient", columnList = "recipient_type,recipient_code"),
        @Index(name = "idx_notification_message_email", columnList = "recipient_email"),
        @Index(name = "idx_notification_message_status", columnList = "status,available_at"),
        @Index(name = "idx_notification_message_event", columnList = "event_type,aggregate_type,aggregate_id")
})
public class NotificationMessage {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "notification_number", nullable = false, unique = true, length = 100)
    private String notificationNumber;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "aggregate_type", length = 80)
    private String aggregateType;

    @Column(name = "aggregate_id", length = 120)
    private String aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 40)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", length = 40)
    private NotificationRecipientType recipientType;

    @Column(name = "recipient_code", length = 120)
    private String recipientCode;

    @Column(name = "recipient_email")
    private String recipientEmail;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "subject")
    private String subject;

    @Column(name = "template_code", length = 120)
    private String templateCode;

    @Column(name = "template_name", length = 180)
    private String templateName;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "payload_json", columnDefinition = "jsonb")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private NotificationDeliveryStatus status = NotificationDeliveryStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = NotificationDeliveryStatus.PENDING;
        }
        if (attempts == null) {
            attempts = 0;
        }
        if (availableAt == null) {
            availableAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
