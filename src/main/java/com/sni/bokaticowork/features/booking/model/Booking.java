package com.sni.bokaticowork.features.booking.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "booking", indexes = {
        @Index(name = "idx_booking_number", columnList = "booking_number"),
        @Index(name = "idx_booking_resource_time", columnList = "resource_id,started_at,ended_at"),
        @Index(name = "idx_booking_owner", columnList = "owner_type,owner_code"),
        @Index(name = "idx_booking_status", columnList = "status")
})
@SQLDelete(sql = "UPDATE booking SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class Booking {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "booking_number", nullable = false, unique = true, length = 80)
    private String bookingNumber;

    @Column(name = "idempotency_key", unique = true, length = 180)
    private String idempotencyKey;

    @Column(name = "hold_number", length = 80)
    private String holdNumber;

    @Column(name = "recurrence_group_number", length = 80)
    private String recurrenceGroupNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false, foreignKey = @ForeignKey(name = "fk_booking_resource"))
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private BookingStatus status = BookingStatus.DRAFT;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private LocalDateTime endedAt;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_unit", nullable = false, length = 40)
    private ResourceBookingUnit bookingUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 40)
    private BookingPaymentMode paymentMode;

    @Column(name = "subscription_number", length = 80)
    private String subscriptionNumber;

    @Column(name = "pass_number", length = 80)
    private String passNumber;

    @Column(name = "entitlement_code", length = 100)
    private String entitlementCode;

    @Column(name = "billable_number", length = 100)
    private String billableNumber;

    @Column(name = "unit_price", precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "subtotal_amount", precision = 19, scale = 4)
    private BigDecimal subtotalAmount;

    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "cancellation_reason", columnDefinition = "text")
    private String cancellationReason;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "started_event_at")
    private Instant startedEventAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "approval_required", nullable = false)
    @Builder.Default
    private Boolean approvalRequired = Boolean.FALSE;

    @Column(name = "approved_by", length = 120)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_by", length = 120)
    private String rejectedBy;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "checked_out_at")
    private Instant checkedOutAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = Boolean.FALSE;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
