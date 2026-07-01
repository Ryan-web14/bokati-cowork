package com.sni.bokaticowork.features.contract.dto.request;

import lombok.Data;

@Data
public class SignAmendmentRequest {
    private String signedDocumentCode;
    private String justification;
}
