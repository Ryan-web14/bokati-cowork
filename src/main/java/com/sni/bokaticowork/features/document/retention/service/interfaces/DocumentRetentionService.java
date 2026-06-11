package com.sni.bokaticowork.features.document.retention.service.interfaces;

import com.sni.bokaticowork.features.document.retention.dto.request.DocumentRetentionPolicyRequest;
import com.sni.bokaticowork.features.document.retention.dto.response.DocumentRetentionPolicyResponse;

import java.util.List;

public interface DocumentRetentionService {

    DocumentRetentionPolicyResponse create(DocumentRetentionPolicyRequest request);

    DocumentRetentionPolicyResponse update(String code, DocumentRetentionPolicyRequest request);

    DocumentRetentionPolicyResponse getByCode(String code);

    List<DocumentRetentionPolicyResponse> listAll();

    void deactivate(String code);

    int applyPolicies();
}
