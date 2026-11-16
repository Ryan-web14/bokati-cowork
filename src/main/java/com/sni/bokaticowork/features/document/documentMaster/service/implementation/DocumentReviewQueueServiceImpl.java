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
    private com.sni.bokaticowork.features.document.documentMaster.security.DocumentSpaceSecurityService spaceSecurity;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setSpaceSecurity(com.sni.bokaticowork.features.document.documentMaster.security.DocumentSpaceSecurityService spaceSecurity) {
        this.spaceSecurity = spaceSecurity;
    }

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
        java.util.Set<DocumentSpace> spaces = spaceSecurity == null ? null
                : spaceSecurity.reviewableSpaces(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
        if (spaces == null) {
            return getQueue(null, pageable);
        }
        if (spaces.isEmpty()) {
            return new PaginatedResponse<>(org.springframework.data.domain.Page.<DocumentResponse>empty(pageable));
        }
        return new PaginatedResponse<>(documentRepository.findAllBySpaceInAndStatus(spaces, DocumentStatus.PENDING_REVIEW, pageable)
                .map(doc -> documentService.getByCode(doc.getCode())));
    }

    /** Lot KG0 : un document hors de l'espace de la route n'est pas traite. */
    private void requireSpace(DocumentSpace space, String code) {
        com.sni.bokaticowork.features.document.documentMaster.model.Document document = documentRepository.findByCode(code)
                .orElseThrow(() -> new com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException("Document introuvable : " + code));
        if (space != null && document.getSpace() != space) {
            throw new com.sni.bokaticowork.core.exception.customs.BadRequestException(
                    "Document " + code + " hors de l'espace " + space.name() + " (espace " + document.getSpace() + ")");
        }
    }

    @Override
    // Lot KG0 : chaque document dans sa propre transaction ; un echec n'annule pas les autres.
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public DocumentBulkActionResponse bulkApprove(DocumentSpace space, DocumentBulkApproveRequest request) {
        List<DocumentBulkItemResult> results = new ArrayList<>();
        for (String code : request.getCodes()) {
            try {
                requireSpace(space, code);
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
    // Lot KG0 : chaque document dans sa propre transaction ; un echec n'annule pas les autres.
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public DocumentBulkActionResponse bulkReject(DocumentSpace space, DocumentBulkRejectRequest request) {
        List<DocumentBulkItemResult> results = new ArrayList<>();
        for (String code : request.getCodes()) {
            try {
                requireSpace(space, code);
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
    // Lot KG0 : chaque document dans sa propre transaction ; un echec n'annule pas les autres.
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public DocumentBulkActionResponse bulkRequestCorrection(DocumentSpace space, DocumentBulkCorrectionRequest request) {
        List<DocumentBulkItemResult> results = new ArrayList<>();
        for (String code : request.getCodes()) {
            try {
                requireSpace(space, code);
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
