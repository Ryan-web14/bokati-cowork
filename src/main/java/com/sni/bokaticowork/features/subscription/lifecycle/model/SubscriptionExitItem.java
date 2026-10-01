package com.sni.bokaticowork.features.subscription.lifecycle.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Une ligne de la liste de sortie · badge rendu, courrier retire, solde regle.
 *
 * <p>C'est ce qui evite les fins d'abonnement baclees. Une ligne obligatoire en attente bloque
 * l'achevement · on la coche, ou on la dispense en disant pourquoi.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "subscription_exit_item")
public class SubscriptionExitItem {

    public enum Status {
        PENDING, DONE, WAIVED
    }

    public static final String BADGE_RETURN = "BADGE_RETURN";
    public static final String KEYS_RETURN = "KEYS_RETURN";
    public static final String MAIL_PENDING = "MAIL_PENDING";
    public static final String BALANCE_DUE = "BALANCE_DUE";
    public static final String DEPOSIT_SETTLEMENT = "DEPOSIT_SETTLEMENT";
    public static final String DOMICILIATION_END = "DOMICILIATION_END";
    public static final String TERMINATION_FEE = "TERMINATION_FEE";

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "termination_id", nullable = false)
    private SubscriptionTermination termination;

    @Column(name = "item_code", nullable = false, length = 40)
    private String itemCode;

    @Column(name = "label", nullable = false, length = 200)
    private String label;

    @Column(name = "mandatory", nullable = false)
    @Builder.Default
    private Boolean mandatory = Boolean.TRUE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @Column(name = "done_by", length = 120)
    private String doneBy;

    @Column(name = "done_at")
    private Instant doneAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}
