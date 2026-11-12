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
import java.time.LocalDate;

/** Modele d'etat financier, versionne (lot F12). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_statement_template")
public class AccStatementTemplate {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code")
    private String code;

    @Column(name = "kind")
    private String kind;

    @Builder.Default
    @Column(name = "version")
    private Integer version = 1;

    @Column(name = "label")
    private String label;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Builder.Default
    @Column(name = "active")
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_by")
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
