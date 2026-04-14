package com.sni.bokaticowork.features.contract.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContractTemplateResponse {
    private String code;
    private String name;
    private String description;
}
