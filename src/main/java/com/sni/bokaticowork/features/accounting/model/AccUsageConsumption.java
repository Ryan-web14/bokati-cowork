package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Consommation de credits d'un pass, pour la reconnaissance a l'usage (lot F20). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_usage_consumption")
public class AccUsageConsumption {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "usage_date")
    private LocalDate usageDate;

    @Builder.Default
    @Column(name = "units")
    private BigDecimal units = BigDecimal.ZERO;

    @Column(name = "source_ref")
    private String sourceRef;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
