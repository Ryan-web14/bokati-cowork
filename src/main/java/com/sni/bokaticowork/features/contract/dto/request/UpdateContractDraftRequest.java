package com.sni.bokaticowork.features.contract.dto.request;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class UpdateContractDraftRequest {

    private String title;

    private String description;

    private DocumentOwnerType ownerType;

    private String ownerCode;

    private String businessCode;

    private String cssContent;

    private String headerHtml;

    private String footerHtml;

    @Valid
    private List<ContractDraftSectionRequest> sections;
}
