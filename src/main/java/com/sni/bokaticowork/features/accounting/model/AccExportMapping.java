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

/** Correspondance d'un compte ou d'un journal vers le logiciel cible d'un format d'export (lot F14). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_export_mapping")
public class AccExportMapping {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "format")
    private String format;

    @Column(name = "kind")
    private String kind;

    @Column(name = "source")
    private String source;

    @Column(name = "target")
    private String target;

    @Column(name = "created_by")
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
