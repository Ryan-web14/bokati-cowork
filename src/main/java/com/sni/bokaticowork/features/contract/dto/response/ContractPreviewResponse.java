package com.sni.bokaticowork.features.contract.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContractPreviewResponse {
    private String templateCode;
    private String html;
}
