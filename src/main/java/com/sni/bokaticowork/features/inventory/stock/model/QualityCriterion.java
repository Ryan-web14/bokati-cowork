package com.sni.bokaticowork.features.inventory.stock.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Un critere mesurable d un plan de controle.
 *
 * <p>Deux natures : une valeur numerique encadree par une plage, ou une observation binaire. Le
 * caractere bloquant distingue ce qui condamne le lot de ce qui se note sans le condamner.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "quality_criterion", uniqueConstraints = {
        @UniqueConstraint(name = "uk_quality_criterion_plan_code", columnNames = {"plan_id", "criterion_code"})
})
public class QualityCriterion {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private QualityControlPlan plan;

    @Column(name = "criterion_code", nullable = false, length = 40)
    private String criterionCode;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    /** Unite de la mesure, par exemple pourcentage ou millimetre. */
    @Column(name = "unit", length = 40)
    private String unit;

    /** Borne basse acceptable, ou null pour un critere non numerique. */
    @Column(name = "min_value", precision = 19, scale = 4)
    private BigDecimal minValue;

    /** Borne haute acceptable, ou null. */
    @Column(name = "max_value", precision = 19, scale = 4)
    private BigDecimal maxValue;

    /** Vrai pour un critere d observation, sans mesure numerique. */
    @Builder.Default
    @Column(name = "boolean_expected", nullable = false)
    private Boolean booleanExpected = Boolean.FALSE;

    /** Un critere bloquant en echec condamne le lot, un critere non bloquant se note seulement. */
    @Builder.Default
    @Column(name = "blocking", nullable = false)
    private Boolean blocking = Boolean.TRUE;

    @Builder.Default
    @Column(name = "position", nullable = false)
    private Integer position = 1;

    @PrePersist
    void prePersist() {
        if (booleanExpected == null) booleanExpected = Boolean.FALSE;
        if (blocking == null) blocking = Boolean.TRUE;
        if (position == null) position = 1;
    }

    /**
     * Vrai lorsque la valeur mesuree tombe dans les bornes.
     *
     * <p>Un critere sans borne est considere conforme : il sert alors a consigner une mesure, pas a
     * la juger.</p>
     */
    public boolean accepts(BigDecimal measured) {
        if (measured == null) {
            return false;
        }
        if (minValue != null && measured.compareTo(minValue) < 0) {
            return false;
        }
        return maxValue == null || measured.compareTo(maxValue) <= 0;
    }
}
