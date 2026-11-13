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

/** Evenement d'un echeancier : gel, decalage, resiliation, annulation, expiration, report (lot F20). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_deferral_schedule_event")
public class AccDeferralScheduleEvent {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "type")
    private String type;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "before_value")
    private String beforeValue;

    @Column(name = "after_value")
    private String afterValue;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_by")
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
