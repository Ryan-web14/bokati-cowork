package com.sni.bokaticowork.features.document.documentMaster.dto.response;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentCategory;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class DocumentResponse {
    private String code;
    private Long ownerId;
    private DocumentOwnerType ownerType;
    private DocumentCategory category;
    private String documentTypeCode;
    private String documentTypeName;
    private String title;
    private String description;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
    private String checksumSha256;
    private Integer currentVersionNumber;
    private DocumentStatus status;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private Long uploadedBy;
    private Instant uploadedAt;
    private Instant updatedAt;
    private List<DocumentVersionResponse> versions;
}
