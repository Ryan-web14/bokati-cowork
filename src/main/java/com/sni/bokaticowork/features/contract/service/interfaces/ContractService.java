package com.sni.bokaticowork.features.contract.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.contract.dto.request.CreateContractRequest;
import com.sni.bokaticowork.features.contract.dto.request.UpdateContractRequest;
import com.sni.bokaticowork.features.contract.dto.request.UpdateContractStatusRequest;
import com.sni.bokaticowork.features.contract.dto.response.ContractResponse;
import com.sni.bokaticowork.features.contract.enums.ContractStatus;
import com.sni.bokaticowork.features.contract.model.Contract;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import org.springframework.data.domain.Pageable;

public interface ContractService {
    ContractResponse create(CreateContractRequest request);
    ContractResponse update(String contractCode, UpdateContractRequest request);
    ContractResponse getByCode(String contractCode);
    PaginatedResponse<ContractResponse> list(DocumentOwnerType ownerType, String ownerCode, ContractStatus status, String businessCode, String templateCode, Pageable pageable);
    ContractResponse updateStatus(String contractCode, UpdateContractStatusRequest request);
    ContractResponse markGenerated(String contractCode, String documentCode);
    ContractResponse markSigned(String contractCode, String documentCode);
    ContractResponse activate(String contractCode);
    ContractResponse suspend(String contractCode, String reason);
    ContractResponse terminate(String contractCode, String reason);
    ContractResponse cancel(String contractCode, String reason);
    void delete(String contractCode);
    Contract serviceByCode(String contractCode);
}
