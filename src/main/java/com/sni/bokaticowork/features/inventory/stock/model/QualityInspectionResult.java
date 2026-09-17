package com.sni.bokaticowork.features.inventory.stock.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Resultat d un critere pour une inspection donnee.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "quality_inspection_result")
public class QualityInspectionResult {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id", nullable = false)
    private QualityInspection inspection;

    @Column(name = "criterion_code", nullable = false, length = 40)
    private String criterionCode;

    @Column(name = "criterion_name", length = 160)
    private String criterionName;

    @Column(name = "measured_value", precision = 19, scale = 4)
    private BigDecimal measuredValue;

    @Column(name = "measured_flag")
    private Boolean measuredFlag;

    @Builder.Default
    @Column(name = "conform", nullable = false)
    private Boolean conform = Boolean.FALSE;

    /** Recopie du caractere bloquant du critere, pour que le resultat reste lisible seul. */
    @Builder.Default
    @Column(name = "blocking", nullable = false)
    private Boolean blocking = Boolean.TRUE;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    void prePersist() {
        if (blocking == null) blocking = Boolean.TRUE;
        if (conform == null) conform = Boolean.FALSE;
    }

    /** Vrai lorsque ce resultat condamne le lot. */
    public boolean isBlockingFailure() {
        return Boolean.TRUE.equals(blocking) && !Boolean.TRUE.equals(conform);
    }
}
