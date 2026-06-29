package com.sni.bokaticowork.features.document.documentMaster.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentCategory;
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
@Table(name = "document_type")
public class DocumentType {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, length = 100, unique = true)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "category", nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private DocumentCategory category;

    @Column(name = "owner_type")
    @Enumerated(EnumType.STRING)
    private DocumentOwnerType ownerType;

    @Column(name = "description")
    private String description;

    @Column(name = "help_text")
    private String helpText;

    @Column(name = "document_details")
    private String documentDetails;

    @Builder.Default
    @Column(name = "required")
    private Boolean required = Boolean.FALSE;

    @Builder.Default
    @Column(name = "requires_expiry_date")
    private Boolean requiresExpiryDate = Boolean.FALSE;

    @Builder.Default
    @Column(name = "requires_review")
    private Boolean requiresReview = Boolean.FALSE;

    @Builder.Default
    @Column(name = "auto_approve")
    private Boolean autoApprove = Boolean.FALSE;

    @Column(name = "auto_approve_after_days")
    private Integer autoApproveAfterDays;

    @Builder.Default
    @Column(name = "requires_signature")
    private Boolean requiresSignature = Boolean.FALSE;

    @Builder.Default
    @Column(name = "multiple_allowed")
    private Boolean multipleAllowed = Boolean.FALSE;

    @Builder.Default
    @Column(name = "requires_back_side")
    private Boolean requiresBackSide = Boolean.FALSE;

    @Column(name = "allowed_mime_types")
    private String allowedMimeTypes;

    @Column(name = "max_file_size_bytes")
    private Long maxFileSizeBytes;

    @Builder.Default
    @Column(name = "active")
    private Boolean active = Boolean.TRUE;
}
