package com.sni.bokaticowork.features.payment.compliance.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Un dossier de conformite · pas seulement une alerte.
 *
 * <p>Un dossier a un responsable, une echeance et une decision motivee. Une decision sans motif
 * ecrit n'est pas une decision, c'est un classement · la base refuse la cloture sans motif, et le
 * code ne cherche pas a la contourner.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "compliance_case")
public class ComplianceCase {

    public enum Status {
        OPEN, IN_REVIEW, ESCALATED, CLOSED_CLEARED, CLOSED_CONFIRMED, CLOSED_REPORTED;

        public boolean open() {
            return this == OPEN || this == IN_REVIEW || this == ESCALATED;
        }
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    /** Ce que la revue a conclu · CLEARED ecarte, CONFIRMED confirme, REPORTED confirme et declare. */
    public enum Decision {
        CLEARED, CONFIRMED, REPORTED
    }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "case_number", nullable = false, unique = true, length = 100)
    private String caseNumber;

    @Column(name = "subject_type", nullable = false, length = 40)
    private String subjectType;

    @Column(name = "subject_code", nullable = false, length = 120)
    private String subjectCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private WalletAccount wallet;

    @Column(name = "title", nullable = false)
    private String title;

    /** Numeros des signalements a l'origine, separes par des virgules. */
    @Column(name = "triggered_by_flags", columnDefinition = "text")
    private String triggeredByFlags;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private Status status = Status.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Column(name = "assigned_to", length = 120)
    private String assignedTo;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    /** Delai de traitement · tenu et mesure. */
    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "findings", columnDefinition = "text")
    private String findings;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", length = 40)
    private Decision decision;

    @Column(name = "decision_rationale", columnDefinition = "text")
    private String decisionRationale;

    @Column(name = "decided_by", length = 120)
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "escalated_to", length = 120)
    private String escalatedTo;

    @Column(name = "escalated_at")
    private Instant escalatedAt;

    @Column(name = "opened_by", nullable = false, length = 120)
    @Builder.Default
    private String openedBy = "SYSTEM";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean overdueAt(Instant moment) {
        return status.open() && dueAt != null && moment.isAfter(dueAt);
    }

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
