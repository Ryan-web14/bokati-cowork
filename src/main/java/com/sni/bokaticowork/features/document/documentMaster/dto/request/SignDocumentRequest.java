package com.sni.bokaticowork.features.document.documentMaster.dto.request;

import jakarta.validation.constraints.AssertTrue;

public record SignDocumentRequest(
        @AssertTrue Boolean accepted,
        String signatureData
) {
}
