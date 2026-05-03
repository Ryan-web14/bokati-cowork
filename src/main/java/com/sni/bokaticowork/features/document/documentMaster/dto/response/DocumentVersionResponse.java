package com.sni.bokaticowork.features.document.documentMaster.dto.response;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentAntivirusStatus;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentVersionUploadStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DocumentVersionResponse {
    private Integer versionNumber;
    private String storageProvider;
    private String storagePath;
    private String previewUrl;
    private String downloadUrl;
    private String originalFileName;
    private String storedFileName;
    private String mimeTypeDeclared;
    private String mimeTypeDetected;
    private String fileExtension;
    private String checksumSha256;
    private Long fileSizeBytes;
    private DocumentVersionUploadStatus uploadStatus;
    private DocumentAntivirusStatus antivirusStatus;
    private Long uploadedBy;
    private Instant uploadedAt;
    private Boolean current;
}
