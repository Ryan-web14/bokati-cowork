package com.sni.bokaticowork.features.ressource.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "resource_policy")
public class ResourcePolicy {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false,unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "min_booking_duration_minutes", nullable = false)
    private Integer minBookingDurationMinutes;

    @Column(name = "max_booking_duration_minutes", nullable = false)
    private Integer maxBookingDurationMinutes;

    @Column(name = "min_booking_notice_minutes", nullable = false)
    private Integer minBookingNoticeMinutes;

    @Column(name = "cancellation_notice_minutes", nullable = false)
    private Integer cancellationNoticeMinutes;

    /**
     * Lot B2 (ecart Bn-6) : grille de remboursement propre a la politique, « heures:pourcentage »
     * separees par des virgules (par exemple « 72:100,24:50,0:0 ») ; vide : la grille generale.
     */
    @Column(name = "refund_rules", length = 300)
    private String refundRules;

    @Column(name = "allow_cancellation", nullable = false)
    @Builder.Default
    private Boolean allowCancellation = Boolean.TRUE; // Lot R2 (D3) : annulation permise par defaut

    @Column(name = "active")
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    /**
     * Un membre de l'equipe peut-il reserver hors preavis · vrai sauf pour les politiques qui
     * l'interdisent (stricte, evenementielle). Voir {@code StaffBookingOverride}.
     */
    @Column(name = "staff_override_allowed", nullable = false)
    @Builder.Default
    private Boolean staffOverrideAllowed = Boolean.TRUE;

    /** Lot R5a : battement entre deux reservations d'une ressource louee entiere, en minutes. */
    @Column(name = "buffer_minutes")
    private Integer bufferMinutes;

    /** Lot R5a : reservation au plus tant de jours a l'avance (vide : l'horizon des creneaux). */
    @Column(name = "max_advance_days")
    private Integer maxAdvanceDays;

    /** Lot R5c : tolerance de depassement au depart, en minutes ; au-dela, facture (vide : jamais). */
    @Column(name = "overrun_grace_minutes")
    private Integer overrunGraceMinutes;

    /** Lot R5c : frais de non-presentation (vide : aucun). */
    @Column(name = "no_show_fee")
    private Integer noShowFee;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

}
