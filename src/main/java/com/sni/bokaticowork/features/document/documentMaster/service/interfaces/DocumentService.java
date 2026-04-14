package com.sni.bokaticowork.features.document.documentMaster.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentReviewDecisionRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentUploadMetadataRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {
    DocumentResponse upload(DocumentUploadMetadataRequest metadata, MultipartFile file);
    DocumentResponse createGeneratedDocument(DocumentUploadMetadataRequest metadata, String originalFileName, String declaredMimeType, byte[] content);
    DocumentResponse replace(String documentCode, Long uploadedBy, MultipartFile file);
    DocumentResponse getByCode(String documentCode);
    PaginatedResponse<DocumentResponse> list(DocumentOwnerType ownerType, String ownerCode, Pageable pageable);
    List<com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentVersionResponse> listVersions(String documentCode);
    DocumentResponse approve(String documentCode, DocumentReviewDecisionRequest request);
    DocumentResponse reject(String documentCode, DocumentReviewDecisionRequest request);
}
