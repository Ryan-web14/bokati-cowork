package com.sni.bokaticowork.features.billing.element.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Lien pose par l'equipe entre une facture et l'element qu'elle regle (abonnement, pass, reservation,
 * domiciliation). Les factures produites par l'element lui-meme n'ont pas besoin de ce lien : elles se
 * retrouvent par leurs lignes facturables.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "billing_document_element")
public class BillingDocumentElement {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "document_number", nullable = false, length = 80)
    private String documentNumber;

    @Column(name = "element_type", nullable = false, length = 20)
    private String elementType;

    @Column(name = "element_code", nullable = false, length = 80)
    private String elementCode;

    /** {@code FULL_SETTLEMENT} : effets au solde ; {@code FIRST_PAYMENT} : effets des le premier paiement. */
    @Column(name = "activation_mode", nullable = false, length = 20)
    private String activationMode;

    @Column(name = "linked_by", length = 150)
    private String linkedBy;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    @Column(name = "removed_by", length = 150)
    private String removedBy;

    @Column(name = "removed_at")
    private Instant removedAt;

    @PrePersist
    void prePersist() {
        if (linkedAt == null) {
            linkedAt = Instant.now();
        }
        if (activationMode == null) {
            activationMode = ElementRef.FULL_SETTLEMENT;
        }
    }
}
