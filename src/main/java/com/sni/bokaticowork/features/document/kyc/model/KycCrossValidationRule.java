package com.sni.bokaticowork.features.document.kyc.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "document_type_code1", nullable = false, length = 150)
    private String documentTypeCode1;

    @Column(name = "document_type_code2", nullable = false, length = 150)
    private String documentTypeCode2;

    @Column(name = "field_to_compare", nullable = false, length = 80)
    private String fieldToCompare;

    @Builder.Default
    @Column(name = "blocking", nullable = false)
    private Boolean blocking = Boolean.TRUE;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;
}
