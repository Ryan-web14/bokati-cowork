package com.sni.bokaticowork.features.contract.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.contract.dto.request.ContractDraftSectionRequest;
import com.sni.bokaticowork.features.contract.dto.request.CreateContractDraftRequest;
import com.sni.bokaticowork.features.contract.dto.request.UpdateContractDraftRequest;
import com.sni.bokaticowork.features.contract.dto.response.ContractDraftResponse;
import com.sni.bokaticowork.features.contract.dto.response.ContractDraftSectionResponse;
import com.sni.bokaticowork.features.contract.dto.response.ContractPreviewResponse;
import com.sni.bokaticowork.features.contract.enums.ContractDraftStatus;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface ContractDraftService {
    ContractDraftResponse create(CreateContractDraftRequest request);
    ContractDraftResponse createFromTemplate(String templateCode, CreateContractDraftRequest request);
    ContractDraftResponse update(String code, UpdateContractDraftRequest request);
    ContractDraftResponse getByCode(String code);
    PaginatedResponse<ContractDraftResponse> list(Pageable pageable);
    PaginatedResponse<ContractDraftResponse> listByStatus(ContractDraftStatus status, Pageable pageable);
    PaginatedResponse<ContractDraftResponse> listByOwner(DocumentOwnerType ownerType, String ownerCode, Pageable pageable);
    PaginatedResponse<ContractDraftResponse> search(String query, Pageable pageable);
    ContractDraftSectionResponse addSection(String draftCode, ContractDraftSectionRequest request);
    ContractDraftSectionResponse updateSection(String draftCode, Long sectionId, ContractDraftSectionRequest request);
    void removeSection(String draftCode, Long sectionId);
    ContractDraftResponse updateStatus(String code, ContractDraftStatus status);
    ContractPreviewResponse preview(String code, Map<String, String> extraVariables);
    DocumentResponse generatePdf(String code, Map<String, String> extraVariables, Long uploadedBy);
    void delete(String code);
}
