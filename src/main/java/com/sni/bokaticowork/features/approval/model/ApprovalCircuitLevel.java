package com.sni.bokaticowork.features.approval.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Niveau d'un circuit : requis a partir d'un montant, decide selon une regle. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "approval_circuit_level")
public class ApprovalCircuitLevel {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "circuit_id", nullable = false)
    private Long circuitId;

    @Column(name = "level_order", nullable = false)
    private Integer levelOrder;

    @Column(name = "label", nullable = false, length = 120)
    private String label;

    /** Le niveau est requis quand le montant atteint ce seuil ; vide : toujours requis. */
    @Column(name = "min_amount", precision = 19, scale = 4)
    private BigDecimal minAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "approver_rule", nullable = false, length = 20)
    private com.sni.bokaticowork.features.approval.enums.ApproverRule approverRule;

    /** Regle MANAGER : 1 pour le N+1, 2 pour le N+2... */
    @Column(name = "hierarchy_depth")
    private Integer hierarchyDepth;

    /** Droit qui permet aussi de decider de ce niveau (forme MODULE:ACTION). */
    @Column(name = "permission", length = 80)
    private String permission;

    /** Regle USER : l'identifiant de connexion du decideur. */
    @Column(name = "user_login", length = 180)
    private String userLogin;
}
