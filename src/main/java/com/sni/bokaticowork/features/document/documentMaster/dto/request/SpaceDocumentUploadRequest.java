package com.sni.bokaticowork.features.document.documentMaster.dto.request;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class SpaceDocumentUploadRequest {

    @NotNull
    private DocumentOwnerType ownerType;

    @NotBlank
    private String ownerCode;

    @NotBlank
    private String documentTypeCode;

    @NotBlank
    private String title;

    private String description;

    private String documentNumber;

    private LocalDate issueDate;

    private LocalDate expiryDate;

    private String referenceCode;

    private String folderCode;

    private List<String> tagCodes;

    private Map<String, String> metadata;
}
