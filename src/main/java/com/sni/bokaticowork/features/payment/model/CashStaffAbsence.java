package com.sni.bokaticowork.features.payment.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Absence d'un caissier (conge, maladie, mission, depart) et son remplacement. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_staff_absence")
public class CashStaffAbsence {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "absence_number", nullable = false, unique = true, length = 100)
    private String absenceNumber;

    @Column(name = "user_email", nullable = false, length = 180)
    private String userEmail;

    @Column(name = "kind_code", nullable = false, length = 60)
    private String kindCode;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    @Column(name = "ends_on")
    private LocalDate endsOn;

    @Column(name = "replacement_user", length = 180)
    private String replacementUser;

    @Column(name = "register_codes", length = 2000)
    private String registerCodes;

    @Enumerated(EnumType.STRING)
    @Column(name = "no_replacement_policy", length = 30)
    private com.sni.bokaticowork.features.payment.enums.CashNoReplacementPolicy noReplacementPolicy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private com.sni.bokaticowork.features.payment.enums.CashAbsenceStatus status;

    @Builder.Default
    @Column(name = "unplanned", nullable = false)
    private Boolean unplanned = Boolean.FALSE;

    @Column(name = "declared_by", nullable = false, length = 180)
    private String declaredBy;

    @Column(name = "approved_by", length = 180)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Builder.Default
    @Column(name = "forced", nullable = false)
    private Boolean forced = Boolean.FALSE;

    @Column(name = "forced_note", length = 1000)
    private String forcedNote;

    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
