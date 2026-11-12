package com.sni.bokaticowork.features.payment.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

/** Plage d'ouverture d'une caisse pour un jour de la semaine (1 = lundi, 7 = dimanche). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_register_schedule")
public class CashRegisterSchedule {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "register_code", nullable = false, length = 100)
    private String registerCode;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Column(name = "opens_at", nullable = false)
    private LocalTime opensAt;

    @Column(name = "closes_at", nullable = false)
    private LocalTime closesAt;
}
