package com.sni.bokaticowork.features.document.documentMaster.service.interfaces;

import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentTagAssignRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentTagRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentTagUpdateRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentTagResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;

import java.util.List;

public interface DocumentTagService {
    DocumentTagResponse create(DocumentTagRequest request);
    DocumentTagResponse update(String code, DocumentTagUpdateRequest request);
    DocumentTagResponse getByCode(String code);
    List<DocumentTagResponse> list(DocumentSpace space);
    void delete(String code);

    List<DocumentTagResponse> assignTags(String documentCode, DocumentTagAssignRequest request);
    void removeTag(String documentCode, String tagCode);
    List<DocumentTagResponse> getDocumentTags(String documentCode);
}
