package com.sni.bokaticowork.features.inventory.stock.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryCategory;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.stock.enums.QualityControlStage;
import com.sni.bokaticowork.features.inventory.stock.enums.QualityDecision;
import com.sni.bokaticowork.features.inventory.stock.enums.QualitySamplingMode;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Plan de controle applique a un article ou a toute une categorie.
 *
 * <p>Definit quand controler, sur quel echantillon, selon quels criteres, et ce qui se passe en cas
 * d echec. Sans plan, aucune mise en quarantaine automatique : le module continue de fonctionner
 * comme avant pour tout ce qui n en declare pas.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "quality_control_plan", uniqueConstraints = {
        @UniqueConstraint(name = "uk_quality_plan_code", columnNames = "plan_code")
})
public class QualityControlPlan {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "plan_code", nullable = false, length = 80)
    private String planCode;

    @Column(name = "name", nullable = false, length = 220)
    private String name;

    /** Article vise, ou null si le plan porte sur une categorie entiere. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private InventoryItem item;

    /** Categorie visee, ou null si le plan porte sur un article precis. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private InventoryCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "control_stage", nullable = false, length = 40)
    private QualityControlStage controlStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "sampling_mode", nullable = false, length = 40)
    private QualitySamplingMode samplingMode;

    /** Quantite ou pourcentage a controler, selon le mode d echantillonnage. */
    @Column(name = "sampling_parameter", precision = 19, scale = 4)
    private BigDecimal samplingParameter;

    /**
     * Ce qu il advient du lot quand le controle echoue. Volontairement paramatrable : refuser
     * d office convient a un produit dangereux, la quarantaine convient a un ecart discutable.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "decision_on_fail", nullable = false, length = 40)
    private QualityDecision decisionOnFail = QualityDecision.QUARANTINED;

    /**
     * Vrai pour mettre le lot en quarantaine des sa reception, avant meme le controle.
     *
     * <p>C est la posture prudente : la marchandise n est disponible qu une fois verifiee.</p>
     */
    @Builder.Default
    @Column(name = "quarantine_on_receipt", nullable = false)
    private Boolean quarantineOnReceipt = Boolean.FALSE;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Builder.Default
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<QualityCriterion> criteria = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        applyDefaults();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
        applyDefaults();
    }

    private void applyDefaults() {
        if (decisionOnFail == null) decisionOnFail = QualityDecision.QUARANTINED;
        if (quarantineOnReceipt == null) quarantineOnReceipt = Boolean.FALSE;
        if (active == null) active = Boolean.TRUE;
    }
}
