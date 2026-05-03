package com.sni.bokaticowork.features.booking.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.enums.BookingWaitlistStatus;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "booking_waitlist_entry", indexes = {
        @Index(name = "idx_booking_waitlist_slot", columnList = "resource_id,started_at,ended_at,status"),
        @Index(name = "idx_booking_waitlist_owner", columnList = "owner_type,owner_code")
})
public class BookingWaitlistEntry {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false, foreignKey = @ForeignKey(name = "fk_booking_waitlist_resource"))
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

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private LocalDateTime endedAt;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 40)
    private BookingPaymentMode paymentMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private BookingWaitlistStatus status;

    @Column(name = "offered_at")
    private Instant offeredAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        status = status == null ? BookingWaitlistStatus.WAITING : status;
        quantity = quantity == null ? 1 : quantity;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
