package com.sni.bokaticowork.features.booking.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Un lot de reservations faites d'un seul geste par l'equipe : un ou plusieurs membres, une ou
 * plusieurs ressources. Les reservations du lot portent son numero ({@code booking.batch_number}) ;
 * chaque membre recoit une seule facture pour l'ensemble de ses reservations du lot.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "booking_batch")
public class BookingBatch {
    @Id
    @IdGeneration
    private Long id;

    @Column(name = "batch_number", nullable = false, unique = true, length = 80)
    private String batchNumber;

    @Column(name = "idempotency_key", unique = true, length = 180)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 40)
    private BookingPaymentMode paymentMode;

    @Column(name = "billing_company_code", length = 120)
    private String billingCompanyCode;

    @Column(name = "booking_count", nullable = false)
    private Integer bookingCount;

    @Column(name = "member_count", nullable = false)
    private Integer memberCount;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "created_by", length = 180)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
