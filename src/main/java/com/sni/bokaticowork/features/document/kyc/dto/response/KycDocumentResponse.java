package com.sni.bokaticowork.features.document.kyc.dto.response;

import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class KycDocumentResponse {

    /** Lot KG7 : dossiers d'autres titulaires qui ont la meme piece (type et numero) ; signal, sans blocage. */
    private java.util.List<String> duplicateOf;
    private Long id;
    private String documentCode;
    private String documentType;
    private String ownerName;
    private String documentNumber;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private String previewUrl;
    private String downloadUrl;
    private Boolean requiresBackSide;
    private String backDocumentCode;
    /** Qualite d'image au depot : OK, WARNING, UNKNOWN (nul avant la mesure). */
    private String qualityStatus;
    private java.util.List<String> qualityWarnings;
    private String backQualityStatus;
    private java.util.List<String> backQualityWarnings;
    /** Lot KG9 : LIKELY_IDENTITY, UNCERTAIN, NOT_IDENTITY ou NOT_CHECKED (nul si non analysee). */
    private String identityVerdict;
    private String backIdentityVerdict;
    /** JAVA ou OPENCV. */
    private String identityEngine;
    private String backFileName;
    private Long backFileSize;
    private String backMimeType;
    private String backPreviewUrl;
    private String backDownloadUrl;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private KycDocumentVerificationStatus status;
    private Long uploadedBy;
    private String uploadedByEmail;
    private Instant uploadedAt;
}
