package com.sni.bokaticowork.features.contract.service.interfaces;

import com.sni.bokaticowork.features.contract.dto.request.GenerateContractRequest;
import com.sni.bokaticowork.features.contract.dto.response.ContractPreviewResponse;
import com.sni.bokaticowork.features.contract.dto.response.ContractTemplateResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;

import java.util.List;

public interface ContractGenerationService {
    List<ContractTemplateResponse> listTemplates();
    ContractPreviewResponse preview(GenerateContractRequest request);
    DocumentResponse generatePdf(GenerateContractRequest request);
}
