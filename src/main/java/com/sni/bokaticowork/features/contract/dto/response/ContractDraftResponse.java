package com.sni.bokaticowork.features.contract.dto.response;

import com.sni.bokaticowork.features.contract.enums.ContractDraftStatus;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ContractDraftResponse {
    private String code;
    private String title;
    private String description;
    private String templateCode;
    private DocumentOwnerType ownerType;
    private String ownerCode;
    private String businessCode;
    private String cssContent;
    private String headerHtml;
    private String footerHtml;
    private ContractDraftStatus status;
    private Long createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ContractDraftSectionResponse> sections;
}
