package com.sni.bokaticowork.features.approval.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Demande d'approbation d'un centre, sur un objet (facture, demande de fonds...) (lot B15). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "approval_request")
public class ApprovalRequest {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "number", nullable = false, length = 60)
    private String number;

    @Column(name = "center", nullable = false, length = 30)
    private String center;

    @Column(name = "request_type", nullable = false, length = 60)
    private String requestType;

    @Column(name = "subject_type", nullable = false, length = 40)
    private String subjectType;

    /** Reference de l'objet approuve (numero de facture, de demande de fonds...). */
    @Column(name = "subject_ref", nullable = false, length = 80)
    private String subjectRef;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "amount", precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "requester", nullable = false, length = 180)
    private String requester;

    @Column(name = "requester_name")
    private String requesterName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private com.sni.bokaticowork.features.approval.enums.ApprovalStatus status;

    @Column(name = "required_levels", nullable = false)
    private Integer requiredLevels;

    @Column(name = "current_level", nullable = false)
    private Integer currentLevel;

    /** Depuis quand le niveau courant attend. */
    @Column(name = "level_since")
    private Instant levelSince;

    @Column(name = "reminded_at")
    private Instant remindedAt;

    @Column(name = "escalated_at")
    private Instant escalatedAt;

    /** Avant et apres de la modification demandee, libre pour chaque centre. */
    @Column(name = "details_json", columnDefinition = "text")
    private String detailsJson;

    @Column(name = "last_comment", length = 1000)
    private String lastComment;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decided_by", length = 180)
    private String decidedBy;

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
