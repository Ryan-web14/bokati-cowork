package com.sni.bokaticowork.features.accounting.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Type d'engagement hors bilan, parametrable (lot F17). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_commitment_type")
public class AccCommitmentType {
    @Id
    @Column(name = "code")
    private String code;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "default_direction", nullable = false)
    private String defaultDirection;

    @Builder.Default
    @Column(name = "lease", nullable = false)
    private Boolean lease = Boolean.FALSE;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;
}
