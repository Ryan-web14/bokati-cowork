package com.sni.bokaticowork.features.document.documentMaster.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
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
@Table(name = "document_requirement")
public class DocumentRequirement {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "owner_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private DocumentOwnerType ownerType;

    @Column(name = "document_type_code", nullable = false)
    private String documentTypeCode;

    @Column(name = "document_type_name")
    private String documentTypeName;

    @Column(name = "customer_type")
    private String customerType;

    @Column(name = "business_legal_form")
    private String businessLegalForm;

    @Builder.Default
    @Column(name = "required")
    private Boolean required = Boolean.TRUE;

    @Builder.Default
    @Column(name = "active")
    private Boolean active = Boolean.TRUE;
}
