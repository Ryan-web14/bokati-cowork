package com.sni.bokaticowork.features.contract.dto.request;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateContractDraftRequest {

    @NotBlank
    private String title;

    private String description;

    private String templateCode;

    private DocumentOwnerType ownerType;

    private String ownerCode;

    private String businessCode;

    private String cssContent;

    private String headerHtml;

    private String footerHtml;

    @NotNull
    private Long createdBy;

    @Valid
    private List<ContractDraftSectionRequest> sections;
}
