package com.sni.bokaticowork.features.document.kyc.dto.request;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateKycCaseRequest {

    @NotNull
    private DocumentOwnerType ownerType;

    @NotBlank
    private String ownerCode;

    private List<String> documentTypeCodes;
}
