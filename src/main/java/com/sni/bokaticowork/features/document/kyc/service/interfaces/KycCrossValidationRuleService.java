package com.sni.bokaticowork.features.document.kyc.service.interfaces;

import com.sni.bokaticowork.features.document.kyc.dto.request.KycCrossValidationRuleRequest;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycCrossValidationRuleResponse;

import java.util.List;

public interface KycCrossValidationRuleService {

    KycCrossValidationRuleResponse create(KycCrossValidationRuleRequest request);

    KycCrossValidationRuleResponse update(Long id, KycCrossValidationRuleRequest request);

    KycCrossValidationRuleResponse getById(Long id);

    List<KycCrossValidationRuleResponse> list(Boolean activeOnly);

    void delete(Long id);
}
