package com.sni.bokaticowork.features.contract.dto.request;

import lombok.Data;

import java.util.Map;

@Data
public class PreviewContractDraftRequest {

    private Map<String, String> variables;
}
