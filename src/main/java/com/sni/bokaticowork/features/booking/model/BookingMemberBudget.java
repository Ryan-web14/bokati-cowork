package com.sni.bokaticowork.features.booking.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/** Lot B9e : budget mensuel d'un collaborateur pour les reservations facturees a son entreprise. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "booking_member_budget")
public class BookingMemberBudget {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "business_code", nullable = false, length = 300)
    private String businessCode;

    @Column(name = "member_id", nullable = false, length = 300)
    private String memberId;

    @Column(name = "monthly_budget", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyBudget;

    @Column(name = "updated_by", length = 180)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
