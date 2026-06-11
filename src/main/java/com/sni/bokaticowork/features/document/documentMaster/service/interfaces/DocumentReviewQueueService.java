package com.sni.bokaticowork.features.document.documentMaster.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentBulkApproveRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentBulkCorrectionRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentBulkRejectRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentBulkActionResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import org.springframework.data.domain.Pageable;

public interface DocumentReviewQueueService {

    PaginatedResponse<DocumentResponse> getQueue(DocumentSpace space, Pageable pageable);

    PaginatedResponse<DocumentResponse> getGlobalQueue(Pageable pageable);

    DocumentBulkActionResponse bulkApprove(DocumentSpace space, DocumentBulkApproveRequest request);

    DocumentBulkActionResponse bulkReject(DocumentSpace space, DocumentBulkRejectRequest request);

    DocumentBulkActionResponse bulkRequestCorrection(DocumentSpace space, DocumentBulkCorrectionRequest request);
}
