package com.sni.bokaticowork.features.document.template.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Valeur d'un champ ajoute saisi dans l'application (lot D3), rattachee a la piece metier
 * (mouvement de caisse, etat de besoin, demande de fonds...) et non a une impression : elle sert a
 * chaque reimpression.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "document_field_value")
public class DocumentFieldValue {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "template_code", nullable = false, length = 80)
    private String templateCode;

    @Column(name = "source_code", nullable = false, length = 120)
    private String sourceCode;

    @Column(name = "field_key", nullable = false, length = 40)
    private String fieldKey;

    @Column(name = "field_value", nullable = false, length = 500)
    private String value;

    @Column(name = "entered_by", length = 150)
    private String enteredBy;

    @Column(name = "entered_at", nullable = false)
    private Instant enteredAt;
}
