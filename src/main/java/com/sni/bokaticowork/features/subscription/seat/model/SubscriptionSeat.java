package com.sni.bokaticowork.features.subscription.seat.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.seat.enums.SeatRole;
import com.sni.bokaticowork.features.subscription.seat.enums.SeatStatus;
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
@Table(name = "subscription_seat", indexes = {
        @Index(name = "idx_subscription_seat_subscription", columnList = "subscription_id,status"),
        @Index(name = "idx_subscription_seat_member", columnList = "member_id,status")
})
public class SubscriptionSeat {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false, foreignKey = @ForeignKey(name = "fk_subscription_seat_subscription"))
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_subscription_seat_member"))
    private Member member;

    @Column(name = "member_code", nullable = false, length = 120)
    private String memberCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 40)
    private SeatRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private SeatStatus status = SeatStatus.ACTIVE;

    @Column(name = "invited_at")
    private Instant invitedAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "removed_at")
    private Instant removedAt;

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
