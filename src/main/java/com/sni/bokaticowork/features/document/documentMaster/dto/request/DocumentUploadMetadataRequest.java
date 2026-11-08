package com.sni.bokaticowork.features.document.documentMaster.dto.request;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DocumentUploadMetadataRequest {

    @NotNull
    private DocumentOwnerType ownerType;

    /** Obligatoire, sauf pour un document {@code INTERNAL}. */
    private String ownerCode;

    @NotBlank
    private String documentTypeCode;

    @NotBlank
    private String title;

    private String description;

    private String documentNumber;

    private LocalDate issueDate;

    private LocalDate expiryDate;

    /** Dossier de la GED ou ranger le document, facultatif. */
    private String folderCode;
}
