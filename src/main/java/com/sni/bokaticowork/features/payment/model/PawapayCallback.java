package com.sni.bokaticowork.features.payment.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Un rappel de l'operateur, ecrit avant d'etre traite.
 *
 * <p>Ce qui arrive sur le webhook est garde tel quel : le corps, la presence d'une signature, la
 * maniere dont on l'a verifie, et ce qu'on en a fait. Une notification perdue dans un incident
 * se retrouve ; une notification forgee se voit.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "pawapay_callback")
public class PawapayCallback {

    public enum Kind {
        DEPOSIT, REFUND
    }

    public enum VerifiedVia {
        /** La signature HMAC etait presente et juste. */
        SIGNATURE,
        /** Pas de signature exploitable · le statut a ete relu aupres de l'operateur avant d'agir. */
        PROVIDER_STATUS,
        /** Rien n'a pu etre verifie · le rappel n'a produit aucun effet. */
        NONE
    }

    public enum Outcome {
        PROCESSED, DEFERRED, IGNORED, FAILED
    }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private Kind kind;

    @Column(name = "reference_id", length = 80)
    private String referenceId;

    @Column(name = "reported_status", length = 40)
    private String reportedStatus;

    @Column(name = "raw_body", nullable = false, columnDefinition = "TEXT")
    private String rawBody;

    @Column(name = "signature_present", nullable = false)
    private boolean signaturePresent;

    @Enumerated(EnumType.STRING)
    @Column(name = "verified_via", nullable = false, length = 30)
    private VerifiedVia verifiedVia;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20)
    private Outcome outcome;

    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @Column(name = "remote_address", length = 80)
    private String remoteAddress;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @PrePersist
    public void prePersist() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }
}
