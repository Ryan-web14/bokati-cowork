package com.sni.bokaticowork.features.booking.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.booking.enums.BookingLineType;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "booking_line", indexes = {
        @Index(name = "idx_booking_line_booking", columnList = "booking_id")
})
public class BookingLine {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, foreignKey = @ForeignKey(name = "fk_booking_line_booking"))
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_type", nullable = false, length = 40)
    private BookingLineType lineType;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "quantity", precision = 19, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 40)
    private ResourceBookingUnit unit;

    @Column(name = "unit_price", precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "amount", precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "entitlement_code", length = 100)
    private String entitlementCode;

    @Column(name = "billable_number", length = 80)
    private String billableNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
