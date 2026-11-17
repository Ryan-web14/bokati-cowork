package com.sni.bokaticowork.features.document.kyc.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.document.kyc.enums.KycCrossRuleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "kyc_cross_validation_rule")
public class KycCrossValidationRule {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    /** Code stable (regles preenregistrees). */
    @Column(name = "code", length = 80)
    private String code;

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "description")
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 40)
    private KycCrossRuleType ruleType = KycCrossRuleType.FIELD_MATCH;

    /** Type(s) de piece vises, separes par des virgules ; vide = tous. */
    @Column(name = "document_type_code1", length = 1000)
    private String documentTypeCode1;

    /** Seconde piece (FIELD_MATCH seulement). */
    @Column(name = "document_type_code2", length = 1000)
    private String documentTypeCode2;

    @Column(name = "field_to_compare", length = 80)
    private String fieldToCompare;

    /** Parametre du type (MIN_VALIDITY : nombre de jours). */
    @Column(name = "parameter", length = 200)
    private String parameter;

    @Builder.Default
    @Column(name = "blocking", nullable = false)
    private Boolean blocking = Boolean.TRUE;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    /** Regle preenregistree : modifiable et desactivable, pas supprimable. */
    @Builder.Default
    @Column(name = "system_rule", nullable = false)
    private Boolean systemRule = Boolean.FALSE;
}
