package com.sni.bokaticowork.features.document.documentMaster.service.interfaces;

import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentRequirementRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentRequirementResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;

import java.util.List;

public interface DocumentRequirementService {
    DocumentRequirementResponse create(DocumentRequirementRequest request);
    DocumentRequirementResponse update(Long id, DocumentRequirementRequest request);
    DocumentRequirementResponse get(Long id);
    List<DocumentRequirementResponse> list(DocumentOwnerType ownerType, Boolean active);
    DocumentRequirementResponse activate(Long id);
    DocumentRequirementResponse deactivate(Long id);
    void delete(Long id);
}
