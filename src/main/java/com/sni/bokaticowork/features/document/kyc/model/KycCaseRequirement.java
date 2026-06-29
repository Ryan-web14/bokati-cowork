package com.sni.bokaticowork.features.document.kyc.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "kyc_case_requirement", uniqueConstraints = {
        @UniqueConstraint(name = "uk_kyc_case_requirement_case_type",
                columnNames = {"kyc_case_id", "document_type_code"})
})
public class KycCaseRequirement {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kyc_case_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_kyc_case_requirement_case"))
    private KycCase kycCase;

    @Column(name = "document_type_code", nullable = false, length = 150)
    private String documentTypeCode;

    @Column(name = "document_type_name", length = 200)
    private String documentTypeName;

    @Builder.Default
    @Column(name = "required", nullable = false)
    private Boolean required = Boolean.TRUE;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;
}
