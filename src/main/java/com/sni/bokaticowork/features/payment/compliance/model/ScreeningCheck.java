package com.sni.bokaticowork.features.payment.compliance.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * La preuve qu'un controle de liste a eu lieu.
 *
 * <p>Quelle liste, quelle version, quelle date, quel resultat. Un controle dont on ne peut pas
 * montrer qu'il a eu lieu n'a, en pratique, pas eu lieu · d'ou une ligne par controle, que la base
 * interdit de reecrire, et sur laquelle seule la revue humaine vient s'ajouter.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "screening_check")
public class ScreeningCheck {

    public enum Result {
        CLEAR, POSSIBLE_MATCH, MATCH
    }

    public enum Trigger {
        ONBOARDING, PERIODIC, PHONE_CHANGE, MANUAL, CASE_REVIEW
    }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "check_number", nullable = false, unique = true, length = 100)
    private String checkNumber;

    @Column(name = "subject_type", nullable = false, length = 40)
    private String subjectType;

    @Column(name = "subject_code", nullable = false, length = 120)
    private String subjectCode;

    @Column(name = "subject_name", nullable = false)
    private String subjectName;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_reason", nullable = false, length = 60)
    private Trigger triggerReason;

    /** Codes et versions consultes, tels qu'au moment du controle · c'est la preuve. */
    @Column(name = "lists_checked", nullable = false, columnDefinition = "text")
    private String listsChecked;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private Result result;

    @Column(name = "matched_entry_ids", columnDefinition = "text")
    private String matchedEntryIds;

    @Column(name = "match_details", columnDefinition = "text")
    private String matchDetails;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;

    @Column(name = "checked_by", nullable = false, length = 120)
    @Builder.Default
    private String checkedBy = "SYSTEM";

    @Column(name = "case_number", length = 100)
    private String caseNumber;

    @Column(name = "reviewed_by", length = 120)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    /** TRUE_MATCH ou FALSE_MATCH · ce que l'humain a conclu du rapprochement. */
    @Column(name = "review_outcome", length = 40)
    private String reviewOutcome;

    @PrePersist
    public void prePersist() {
        checkedAt = checkedAt == null ? Instant.now() : checkedAt;
    }
}
