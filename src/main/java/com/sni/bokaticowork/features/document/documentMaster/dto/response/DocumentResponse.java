package com.sni.bokaticowork.features.document.documentMaster.dto.response;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentCategory;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class DocumentResponse {
    private String code;
    private Long ownerId;
    private DocumentOwnerType ownerType;
    private DocumentCategory category;
    private DocumentSpace space;
    private String spaceReferenceCode;
    private String folderCode;
    private String folderName;
    private String documentTypeCode;
    private String documentTypeName;
    private String title;
    private String description;
    private String fileName;
    private String fileUrl;
    private String previewUrl;
    private String downloadUrl;
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
    private List<DocumentTagResponse> tags;
    private Map<String, String> metadata;
}
