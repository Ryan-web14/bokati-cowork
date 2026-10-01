package com.sni.bokaticowork.features.billing.dunning.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Un palier · tant de jours apres l'echeance, une action, un canal, un texte. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "dunning_step")
public class DunningStep {

    public enum Action {
        /** Un rappel · le document est rappele, pas renvoye. */
        REMINDER,
        /** La mise en demeure · le dernier avertissement avant la suspension. */
        FORMAL_NOTICE,
        /** L'abonnement lie entre en tolerance. */
        GRACE_PERIOD,
        /** L'abonnement lie est suspendu. */
        SUSPEND,
        /** Le dossier passe a une personne · un e-mail a l'equipe, plus rien d'automatique. */
        HANDOVER
    }

    public enum Channel {
        EMAIL, IN_APP, STAFF
    }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private DunningPolicy policy;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "days_after_due", nullable = false)
    private Integer daysAfterDue;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private Action action;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    @Builder.Default
    private Channel channel = Channel.EMAIL;

    @Column(name = "subject_template", length = 200)
    private String subjectTemplate;

    @Column(name = "message_template", columnDefinition = "TEXT")
    private String messageTemplate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}
