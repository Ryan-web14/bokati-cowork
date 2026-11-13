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

import java.time.Instant;

/** Texte d'une note annexe redigee, par exercice (lot F12). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_note_text")
public class AccNoteText {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "fiscal_year_code")
    private String fiscalYearCode;

    @Column(name = "note_code")
    private String noteCode;

    @Column(name = "body")
    private String body;

    @Builder.Default
    @Column(name = "not_applicable")
    private Boolean notApplicable = Boolean.FALSE;

    @Builder.Default
    @Column(name = "status")
    private String status = "DRAFT";

    @Column(name = "author")
    private String author;

    @Column(name = "validated_by")
    private String validatedBy;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Builder.Default
    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();
}
