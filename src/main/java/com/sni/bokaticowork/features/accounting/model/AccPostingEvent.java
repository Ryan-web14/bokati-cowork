package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.accounting.enums.PostingEventStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/** Evenement comptable : registre d'idempotence (type + reference) et file du moteur (lot F2). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_posting_event")
public class AccPostingEvent {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "reference", nullable = false, length = 150)
    private String reference;

    @Column(name = "family", length = 40)
    private String family;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private PostingEventStatus status = PostingEventStatus.PENDING;

    @Builder.Default
    @Column(name = "replay", nullable = false)
    private Boolean replay = Boolean.FALSE;

    @Column(name = "failure_reason", length = 2000)
    private String failureReason;

    @Column(name = "rule_code", length = 80)
    private String ruleCode;

    @Column(name = "rule_version")
    private Integer ruleVersion;

    @Column(name = "entry_number", length = 40)
    private String entryNumber;

    @Column(name = "provisional_number", length = 40)
    private String provisionalNumber;

    @Column(name = "shifted_note", length = 500)
    private String shiftedNote;

    @Builder.Default
    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "ignored_by", length = 180)
    private String ignoredBy;

    @Column(name = "ignored_reason", length = 1000)
    private String ignoredReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
