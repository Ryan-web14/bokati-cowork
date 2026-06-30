package com.sni.bokaticowork.features.contract.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.contract.dto.request.ContractDraftSectionRequest;
import com.sni.bokaticowork.features.contract.dto.request.CreateContractTemplateRequest;
import com.sni.bokaticowork.features.contract.dto.request.UpdateContractTemplateRequest;
import com.sni.bokaticowork.features.contract.dto.response.ContractPreviewResponse;
import com.sni.bokaticowork.features.contract.dto.response.ContractTemplateResponse;
import com.sni.bokaticowork.features.contract.dto.response.ContractTemplateSectionResponse;
import com.sni.bokaticowork.features.contract.model.ContractTemplate;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ContractTemplateService {
    ContractTemplateResponse create(CreateContractTemplateRequest request);
    ContractTemplateResponse update(String code, UpdateContractTemplateRequest request);
    ContractTemplateResponse getByCode(String code);
    PaginatedResponse<ContractTemplateResponse> list(Pageable pageable);
    PaginatedResponse<ContractTemplateResponse> search(String query, Pageable pageable);
    PaginatedResponse<ContractTemplateResponse> listByCategory(String category, Pageable pageable);
    ContractPreviewResponse previewTemplate(String code, Map<String, String> sampleVariables);
    ContractTemplateResponse duplicate(String code);
    void delete(String code);
    ContractTemplate getTemplateForService(String code);

    // ── Section management ─────────────────────────────────────────────────────
    ContractTemplateSectionResponse addSection(String templateCode, ContractDraftSectionRequest request);
    ContractTemplateSectionResponse updateSection(String templateCode, Long sectionId, ContractDraftSectionRequest request);
    void removeSection(String templateCode, Long sectionId);
    List<ContractTemplateSectionResponse> listSections(String templateCode);
}
