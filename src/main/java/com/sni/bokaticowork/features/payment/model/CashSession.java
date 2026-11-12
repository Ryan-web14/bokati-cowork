package com.sni.bokaticowork.features.payment.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_session")
public class CashSession {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "session_number", nullable = false, unique = true, length = 100)
    private String sessionNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cash_register_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cash_session_register"))
    private CashRegister cashRegister;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private CashSessionStatus status;

    /** Poste depuis lequel la session a ete ouverte (lot C1). */
    @Column(name = "opened_from_device", length = 120)
    private String openedFromDevice;

    /** Motif d'une ouverture hors des horaires de la caisse (lot C1). */
    @Column(name = "out_of_hours_reason", length = 500)
    private String outOfHoursReason;

    // ---- Cycle de vie complet (lot C2) ----

    /** Fonds attendu a l'ouverture : le dernier comptage de la caisse. */
    @Column(name = "expected_opening_amount", precision = 19, scale = 4)
    private BigDecimal expectedOpeningAmount;

    /** Compte a l'ouverture moins attendu. */
    @Column(name = "opening_variance", precision = 19, scale = 4)
    private BigDecimal openingVariance;

    @Column(name = "opening_variance_reason", length = 500)
    private String openingVarianceReason;

    @Column(name = "paused_at")
    private Instant pausedAt;

    @Builder.Default
    @Column(name = "pause_seconds", nullable = false)
    private Long pauseSeconds = 0L;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "suspended_by", length = 180)
    private String suspendedBy;

    @Column(name = "suspension_reason", length = 500)
    private String suspensionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_before_suspension", length = 30)
    private com.sni.bokaticowork.features.payment.enums.CashSessionStatus statusBeforeSuspension;

    /** Fonds garde pour la session suivante ; le reste est a remettre a la caisse mere. */
    @Column(name = "kept_float", precision = 19, scale = 4)
    private BigDecimal keptFloat;

    @Column(name = "amount_to_remit", precision = 19, scale = 4)
    private BigDecimal amountToRemit;

    @Column(name = "closing_count_number", length = 100)
    private String closingCountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "variance_decision", length = 30)
    private com.sni.bokaticowork.features.payment.enums.CashVarianceDecision varianceDecision;

    @Column(name = "variance_decided_by", length = 180)
    private String varianceDecidedBy;

    @Builder.Default
    @Column(name = "forced_close", nullable = false)
    private Boolean forcedClose = Boolean.FALSE;

    @Column(name = "handover_number", length = 100)
    private String handoverNumber;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "max_duration_alerted_at")
    private Instant maxDurationAlertedAt;

    @Column(name = "opened_by", nullable = false, length = 120)
    private String openedBy;

    @Column(name = "closed_by", length = 120)
    private String closedBy;

    @Column(name = "reviewed_by", length = 120)
    private String reviewedBy;

    @Column(name = "opening_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal openingAmount;

    @Column(name = "closing_amount", precision = 19, scale = 4)
    private BigDecimal closingAmount;

    @Column(name = "expected_closing_amount", precision = 19, scale = 4)
    private BigDecimal expectedClosingAmount;

    @Column(name = "counted_closing_amount", precision = 19, scale = 4)
    private BigDecimal countedClosingAmount;

    @Column(name = "variance_amount", precision = 19, scale = 4)
    private BigDecimal varianceAmount;

    @Column(name = "variance_reason")
    private String varianceReason;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closing_requested_at")
    private Instant closingRequestedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @PrePersist
    public void prePersist() {
        openedAt = Instant.now();
    }
}
