package com.sni.bokaticowork.core.communication.whatsapp;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Modele de message WhatsApp, cree chez Infobip et approuve par Meta. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "whatsapp_template")
public class WhatsAppTemplate {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    /** Objet du message : un seul modele approuve sert par objet et par langue. */
    @Column(name = "purpose")
    private String purpose;

    @Column(name = "name")
    private String name;

    @Builder.Default
    @Column(name = "language")
    private String language = "fr";

    @Builder.Default
    @Column(name = "category")
    private String category = "UTILITY";

    /** Texte avec ses variables {{1}}, {{2}}... */
    @Column(name = "body_text")
    private String bodyText;

    /** Exemples des variables, separes par des barres verticales. */
    @Column(name = "examples")
    private String examples;

    @Builder.Default
    @Column(name = "placeholders")
    private Integer placeholders = 0;

    /** DRAFT, PENDING, APPROVED, REJECTED, PAUSED, DISABLED (statut Infobip). */
    @Builder.Default
    @Column(name = "status")
    private String status = "DRAFT";

    @Column(name = "status_detail")
    private String statusDetail;

    @Column(name = "created_by")
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "synced_at")
    private Instant syncedAt;
}
