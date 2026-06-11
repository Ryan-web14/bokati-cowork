package com.sni.bokaticowork.features.document.kyc.service.interfaces;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import com.sni.bokaticowork.features.document.kyc.KycRiskLevel;
import com.sni.bokaticowork.features.document.kyc.dto.request.CreateKycCaseRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycAssignRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycBulkApproveRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycBulkRejectRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycCaseNoteRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycDecisionRequest;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycRiskLevelRequest;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycBulkActionResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycCaseResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycCaseNoteResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycDashboardResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycDocumentResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycDocumentOcrResultResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycExpiryDocumentStatus;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycRequirementStatus;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycTimelineEntryResponse;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface KycService {
    KycCaseResponse createCase(CreateKycCaseRequest request);
    KycCaseResponse getByCode(String code);
    List<KycCaseResponse> list(DocumentOwnerType ownerType);
    PaginatedResponse<KycCaseResponse> search(KycCaseStatus status, DocumentOwnerType ownerType, Instant submittedAfter,
                                              Instant submittedBefore, Long reviewedBy, Boolean pendingReviewOnly,
                                              Integer expiringWithinDays, KycRiskLevel riskLevel, Pageable pageable);
    KycCaseResponse submit(String code);
    KycCaseResponse approve(String code, KycDecisionRequest request);
    KycCaseResponse reject(String code, KycDecisionRequest request);
    List<KycRequirementStatus> getMissingRequirements(String code);
    KycDashboardResponse dashboard();
    KycCaseResponse assign(String code, KycAssignRequest request);
    PaginatedResponse<KycCaseResponse> myQueue(Long userId, Pageable pageable);
    KycCaseNoteResponse addNote(String code, KycCaseNoteRequest request);
    List<KycCaseNoteResponse> listNotes(String code);
    List<KycTimelineEntryResponse> timeline(String code);
    PaginatedResponse<KycCaseResponse> expiringSoon(Integer days, Pageable pageable);
    List<KycExpiryDocumentStatus> expiryStatus(String code);
    KycCaseResponse updateRiskLevel(String code, KycRiskLevelRequest request);
    List<KycDocumentResponse> reviewQueue();
    List<KycDocumentResponse> reviewedDocuments(List<KycDocumentVerificationStatus> statuses);
    KycDocumentOcrResultResponse getOcrResult(String documentCode);
    KycBulkActionResponse bulkApprove(KycBulkApproveRequest request);
    KycBulkActionResponse bulkReject(KycBulkRejectRequest request);
    byte[] exportPdf(String code, boolean includeInternalNotes);
}
