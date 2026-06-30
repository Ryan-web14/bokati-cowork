package com.sni.bokaticowork.features.document.kyc.dto.response;

import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class KycDocumentResponse {
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
