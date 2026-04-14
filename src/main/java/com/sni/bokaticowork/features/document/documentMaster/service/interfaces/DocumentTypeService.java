package com.sni.bokaticowork.features.document.documentMaster.service.interfaces;

import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentTypeRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentTypeResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentType;

import java.util.List;

public interface DocumentTypeService {
    DocumentTypeResponse create(DocumentTypeRequest request);
    DocumentTypeResponse update(String code, DocumentTypeRequest request);
    DocumentTypeResponse get(String code);
    List<DocumentTypeResponse> list(DocumentOwnerType ownerType, Boolean active);
    DocumentTypeResponse activate(String code);
    DocumentTypeResponse deactivate(String code);
    void delete(String code);
    DocumentType serviceByCode(String code);
}
