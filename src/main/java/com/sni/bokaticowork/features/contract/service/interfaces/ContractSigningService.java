package com.sni.bokaticowork.features.contract.service.interfaces;

import com.sni.bokaticowork.features.contract.dto.request.RequestContractSigningRequest;
import com.sni.bokaticowork.features.contract.dto.request.SignContractRequest;
import com.sni.bokaticowork.features.contract.dto.response.ContractSigningResponse;

import java.util.UUID;

public interface ContractSigningService {

    ContractSigningResponse requestSigning(String contractCode, RequestContractSigningRequest request);

    ContractSigningResponse getSigningDetails(UUID token);

    ContractSigningResponse sign(UUID token, SignContractRequest request, String ipAddress, String userAgent);
}
