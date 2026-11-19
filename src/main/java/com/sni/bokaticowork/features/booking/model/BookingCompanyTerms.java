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

/**
 * Lot B9e : conditions de reservation d'une entreprise. Budget mensuel (toutes les reservations
 * facturees a l'entreprise), releve mensuel groupe (une facture par mois au lieu d'une par
 * reservation), forfait d'heures mensuel partage entre ses collaborateurs.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "booking_company_terms")
public class BookingCompanyTerms {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "business_code", nullable = false, length = 300, unique = true)
    private String businessCode;

    @Builder.Default
    @Column(name = "monthly_statement", nullable = false)
    private Boolean monthlyStatement = Boolean.FALSE;

    @Column(name = "monthly_budget", precision = 19, scale = 2)
    private BigDecimal monthlyBudget;

    @Column(name = "monthly_hours")
    private Integer monthlyHours;

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
