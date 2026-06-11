package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentBulkApproveRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentBulkCorrectionRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentBulkRejectRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentCorrectionRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentReviewDecisionRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentBulkActionResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentBulkItemResult;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentReviewQueueService;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DocumentReviewQueueServiceImpl implements DocumentReviewQueueService {

    private final DocumentRepository documentRepository;
    private final DocumentService documentService;

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<DocumentResponse> getQueue(DocumentSpace space, Pageable pageable) {
        Page<?> page = space != null
                ? documentRepository.findAllBySpaceAndStatus(space, DocumentStatus.PENDING_REVIEW, pageable)
                : documentRepository.findAllByStatus(DocumentStatus.PENDING_REVIEW, pageable);
        return new PaginatedResponse<>(
                page.map(doc -> documentService.getByCode(((com.sni.bokaticowork.features.document.documentMaster.model.Document) doc).getCode()))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<DocumentResponse> getGlobalQueue(Pageable pageable) {
        return getQueue(null, pageable);
    }

    @Override
    public DocumentBulkActionResponse bulkApprove(DocumentSpace space, DocumentBulkApproveRequest request) {
        List<DocumentBulkItemResult> results = new ArrayList<>();
        for (String code : request.getCodes()) {
            try {
                DocumentReviewDecisionRequest decision = new DocumentReviewDecisionRequest();
                decision.setReviewedBy(request.getReviewedBy());
                decision.setComment(request.getComment());
                documentService.approve(code, decision);
                results.add(DocumentBulkItemResult.builder().code(code).status("SUCCESS").build());
            } catch (Exception e) {
                log.warn("Bulk approve failed for {}: {}", code, e.getMessage());
                results.add(DocumentBulkItemResult.builder().code(code).status("FAILED").reason(e.getMessage()).build());
            }
        }
        return buildSummary(results);
    }

    @Override
    public DocumentBulkActionResponse bulkReject(DocumentSpace space, DocumentBulkRejectRequest request) {
        List<DocumentBulkItemResult> results = new ArrayList<>();
        for (String code : request.getCodes()) {
            try {
                DocumentReviewDecisionRequest decision = new DocumentReviewDecisionRequest();
                decision.setReviewedBy(request.getReviewedBy());
                decision.setComment(request.getComment());
                decision.setRejectionReasonCode(request.getRejectionReasonCode());
                decision.setRejectionReasonDetail(request.getRejectionReasonDetail());
                documentService.reject(code, decision);
                results.add(DocumentBulkItemResult.builder().code(code).status("SUCCESS").build());
            } catch (Exception e) {
                log.warn("Bulk reject failed for {}: {}", code, e.getMessage());
                results.add(DocumentBulkItemResult.builder().code(code).status("FAILED").reason(e.getMessage()).build());
            }
        }
        return buildSummary(results);
    }

    @Override
    public DocumentBulkActionResponse bulkRequestCorrection(DocumentSpace space, DocumentBulkCorrectionRequest request) {
        List<DocumentBulkItemResult> results = new ArrayList<>();
        for (String code : request.getCodes()) {
            try {
                DocumentCorrectionRequest correction = new DocumentCorrectionRequest();
                correction.setReviewedBy(request.getReviewedBy());
                correction.setCorrectionNote(request.getCorrectionNote());
                correction.setDeadlineDays(request.getDeadlineDays());
                documentService.requestCorrection(code, correction);
                results.add(DocumentBulkItemResult.builder().code(code).status("SUCCESS").build());
            } catch (Exception e) {
                log.warn("Bulk correction request failed for {}: {}", code, e.getMessage());
                results.add(DocumentBulkItemResult.builder().code(code).status("FAILED").reason(e.getMessage()).build());
            }
        }
        return buildSummary(results);
    }

    private DocumentBulkActionResponse buildSummary(List<DocumentBulkItemResult> results) {
        long succeeded = results.stream().filter(r -> "SUCCESS".equals(r.getStatus())).count();
        return DocumentBulkActionResponse.builder()
                .total(results.size())
                .succeeded((int) succeeded)
                .failed((int) (results.size() - succeeded))
                .results(results)
                .build();
    }
}
