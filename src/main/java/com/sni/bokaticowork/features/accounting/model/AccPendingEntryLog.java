package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Mise en attente d'une piece, ses relances et sa resolution. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_pending_entry_log")
public class AccPendingEntryLog {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 40)
    private com.sni.bokaticowork.features.accounting.enums.PendingReason reason;

    @Column(name = "difference", precision = 19, scale = 4)
    private BigDecimal difference;

    @Column(name = "detail", length = 1000)
    private String detail;

    @Column(name = "notified_at")
    private Instant notifiedAt;

    @Builder.Default
    @Column(name = "reminders", nullable = false)
    private Integer reminders = 0;

    @Column(name = "last_reminder_at")
    private Instant lastReminderAt;

    @Column(name = "resolved_by", length = 180)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution", length = 20)
    private String resolution;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
