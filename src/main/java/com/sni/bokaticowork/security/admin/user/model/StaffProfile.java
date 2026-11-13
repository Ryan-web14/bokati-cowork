package com.sni.bokaticowork.security.admin.user.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Fiche collaborateur d'un utilisateur du back-office (lot B12) : fonction imprimee sous son nom,
 * signature enregistree pour le visa des pieces, superieur direct (N+1) pour les circuits
 * d'approbation.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "user_staff_profile")
public class StaffProfile {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "job_title", length = 160)
    private String jobTitle;

    /** Image de signature en data URI (PNG ou JPEG). */
    @Column(name = "signature_image_base64", columnDefinition = "text")
    private String signatureImageBase64;

    @Column(name = "signature_updated_at")
    private Instant signatureUpdatedAt;

    /** Superieur direct (N+1), identifiant technique de l'utilisateur. */
    @Column(name = "manager_user_id")
    private Long managerUserId;

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
