package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Envoi d'une piece : canal, destinataires, auteur, statut (lot B4). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "billing_document_sending")
public class BillingDocumentSending {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "document_number", nullable = false, length = 60)
    private String documentNumber;

    /** EMAIL ou WHATSAPP. */
    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    @Column(name = "recipients_to", columnDefinition = "text")
    private String recipientsTo;

    @Column(name = "recipients_cc", columnDefinition = "text")
    private String recipientsCc;

    @Column(name = "recipients_bcc", columnDefinition = "text")
    private String recipientsBcc;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "message", columnDefinition = "text")
    private String message;

    /** PDF envoye en duplicata (destinataire corrige apres scellement). */
    @Builder.Default
    @Column(name = "duplicate", nullable = false)
    private Boolean duplicate = Boolean.FALSE;

    /** QUEUED (courriel en file), SENT, FAILED ; WhatsApp : statut de l'operateur au retour. */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** Identifiant du message WhatsApp. */
    @Column(name = "provider_reference", length = 120)
    private String providerReference;

    @Column(name = "error", length = 1000)
    private String error;

    @Column(name = "sent_by", nullable = false, length = 180)
    private String sentBy;

    @Column(name = "sent_by_name")
    private String sentByName;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;
}
