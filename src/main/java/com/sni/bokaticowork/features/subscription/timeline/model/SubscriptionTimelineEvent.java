package com.sni.bokaticowork.features.subscription.timeline.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.timeline.enums.SubscriptionTimelineEventType;
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
@Table(name = "subscription_timeline_event", indexes = {
        @Index(name = "idx_subscription_timeline_subscription", columnList = "subscription_id,occurred_at"),
        @Index(name = "idx_subscription_timeline_owner", columnList = "owner_type,owner_code,occurred_at"),
        @Index(name = "idx_subscription_timeline_type", columnList = "event_type,occurred_at"),
        @Index(name = "idx_subscription_timeline_source", columnList = "source_type,source_id")
})
public class SubscriptionTimelineEvent {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "event_number", nullable = false, unique = true, length = 100)
    private String eventNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", foreignKey = @ForeignKey(name = "fk_subscription_timeline_subscription"))
    private Subscription subscription;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", length = 60)
    private SubscriberType ownerType;

    @Column(name = "owner_code", length = 120)
    private String ownerCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 80)
    private SubscriptionTimelineEventType eventType;

    @Column(name = "source_type", length = 80)
    private String sourceType;

    @Column(name = "source_id", length = 120)
    private String sourceId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "payload_json", columnDefinition = "jsonb")
    private String payloadJson;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        createdAt = Instant.now();
    }
}
