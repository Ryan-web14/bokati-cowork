package com.sni.bokaticowork.features.document.documentMaster.dto.request;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BatchUploadMetadataRequest {

    @NotNull
    private DocumentOwnerType ownerType;

    @NotBlank
    private String ownerCode;

    @NotBlank
    private String documentTypeCode;

    private DocumentSpace space;

    private String folderCode;

    private String referenceCode;

    private List<String> tagCodes;
}
