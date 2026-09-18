package com.sni.bokaticowork.features.payment.compliance.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Une entree d'une liste de personnes exposees ou sanctionnees.
 *
 * <p>Le nom normalise · sans accents, sans casse, sans ponctuation · est ce sur quoi on compare.
 * Une liste porte une version : c'est elle qui figure dans la preuve d'un controle, parce qu'un
 * nom absent hier peut etre present aujourd'hui, et il faut pouvoir dire lequel des deux on a
 * consulte.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "screening_list_entry")
public class ScreeningListEntry {

    public enum Type {
        SANCTION, PEP
    }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "list_code", nullable = false, length = 60)
    private String listCode;

    @Column(name = "list_version", nullable = false, length = 60)
    private String listVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private Type entryType;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    /** Autres graphies, separees par des points-virgules · normalisees aussi. */
    @Column(name = "aliases", columnDefinition = "text")
    private String aliases;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Column(name = "nationality", length = 3)
    private String nationality;

    @Column(name = "reference")
    private String reference;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(name = "loaded_at", nullable = false)
    private Instant loadedAt;

    @Column(name = "loaded_by", length = 120)
    private String loadedBy;

    @PrePersist
    public void prePersist() {
        loadedAt = loadedAt == null ? Instant.now() : loadedAt;
    }
}
