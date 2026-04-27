package com.sni.bokaticowork.features.booking.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
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
@Table(name = "booking_status_history", indexes = {
        @Index(name = "idx_booking_status_history_booking", columnList = "booking_id")
})
public class BookingStatusHistory {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, foreignKey = @ForeignKey(name = "fk_booking_status_history_booking"))
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 40)
    private BookingStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 40)
    private BookingStatus toStatus;

    @Column(name = "changed_by", length = 120)
    private String changedBy;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @PrePersist
    void prePersist() {
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }
}
