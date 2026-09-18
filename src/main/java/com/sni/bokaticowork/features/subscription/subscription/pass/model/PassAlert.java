package com.sni.bokaticowork.features.subscription.subscription.pass.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassAlertType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Memoire de ce qui a deja ete annonce.
 *
 * <p>Sans elle, un traitement quotidien renverrait la meme alerte chaque jour jusqu'a l'expiration
 * du pass · c'est la facon la plus sure de faire ignorer les alertes suivantes.</p>
 *
 * <p>Le declencheur fait partie de la clef. Deux alertes du meme type sur des declencheurs
 * differents sont deux alertes distinctes : un pass renouvele porte une nouvelle echeance, et
 * merite d'etre annonce a nouveau.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "pass_alert", indexes = {
        @Index(name = "idx_pass_alert_type", columnList = "alert_type,detected_at")
})
public class PassAlert {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pass_id", nullable = false)
    private Pass pass;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 40)
    private PassAlertType alertType;

    @Column(name = "threshold_key", nullable = false, length = 120)
    private String thresholdKey;

    @Column(name = "notification_number", length = 100)
    private String notificationNumber;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "payload_json", columnDefinition = "jsonb")
    private String payloadJson;

    @PrePersist
    public void prePersist() {
        if (detectedAt == null) {
            detectedAt = Instant.now();
        }
    }
}
