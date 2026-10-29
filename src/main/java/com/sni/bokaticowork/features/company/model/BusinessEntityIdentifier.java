package com.sni.bokaticowork.features.company.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.company.enums.IdentifierType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Un identifiant d'une organisation · NIU, RCCM, recepisse, agrement, inscription a l'ordre...
 *
 * <p>Chaque type d'organisation a les siens ; aucun n'est obligatoire, et la facture imprime ceux
 * qui sont saisis, avec leur libelle.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "business_entity_identifier")
public class BusinessEntityIdentifier {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_entity_id", nullable = false)
    private BusinessEntity businessEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "identifier_type", nullable = false, length = 40)
    private IdentifierType type;

    /** Le libelle imprime · celui du type par defaut, saisi pour {@code AUTRE}. */
    @Column(name = "label", length = 120)
    private String label;

    @Column(name = "identifier_value", nullable = false, length = 150)
    private String value;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
