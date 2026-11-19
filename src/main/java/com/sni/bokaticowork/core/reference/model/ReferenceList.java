package com.sni.bokaticowork.core.reference.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Definition d'une liste de l'administrateur (lot L0) : son code, son module, son libelle, et son
 * mode. Une liste <b>stricte</b> refuse une valeur inconnue ; une liste <b>souple</b> l'ajoute,
 * marquee « ajoutee d'elle-meme », pour que l'administrateur la relise.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "reference_list")
public class ReferenceList {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, length = 60, unique = true)
    private String code;

    @Column(name = "module", nullable = false, length = 40)
    private String module;

    @Column(name = "label", nullable = false, length = 160)
    private String label;

    @Column(name = "description", length = 500)
    private String description;

    @Builder.Default
    @Column(name = "strict", nullable = false)
    private Boolean strict = Boolean.TRUE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void created() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
