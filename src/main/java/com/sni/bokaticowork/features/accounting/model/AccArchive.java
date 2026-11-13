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

/** Archive annuelle de la comptabilite : fichier, empreinte et sommaire (lot F14). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_archive")
public class AccArchive {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "number")
    private String number;

    @Column(name = "fiscal_year_code")
    private String fiscalYearCode;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "content_hash")
    private String contentHash;

    @Column(name = "manifest")
    private String manifest;

    @Column(name = "created_by")
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
