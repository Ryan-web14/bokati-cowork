package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Poste d'un etat financier : rubrique, libelle, niveau, formule des totaux (lot F12). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_statement_line")
public class AccStatementLine {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "code")
    private String code;

    @Column(name = "label")
    private String label;

    @Column(name = "position")
    private Integer position;

    @Builder.Default
    @Column(name = "level")
    private Integer level = 1;

    @Builder.Default
    @Column(name = "line_type")
    private String lineType = "DETAIL";

    @Column(name = "section")
    private String section;

    @Column(name = "formula")
    private String formula;

    @Column(name = "note_ref")
    private String noteRef;
}
