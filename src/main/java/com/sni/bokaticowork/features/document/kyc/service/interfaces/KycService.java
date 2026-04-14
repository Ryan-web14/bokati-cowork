package com.sni.bokaticowork.features.document.kyc.service.interfaces;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.kyc.dto.request.CreateKycCaseRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycDecisionRequest;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycCaseResponse;

import java.util.List;

public interface KycService {
    KycCaseResponse createCase(CreateKycCaseRequest request);
    KycCaseResponse getByCode(String code);
    List<KycCaseResponse> list(DocumentOwnerType ownerType);
    KycCaseResponse submit(String code);
    KycCaseResponse approve(String code, KycDecisionRequest request);
    KycCaseResponse reject(String code, KycDecisionRequest request);
}
