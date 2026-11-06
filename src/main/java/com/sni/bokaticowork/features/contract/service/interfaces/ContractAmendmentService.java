package com.sni.bokaticowork.features.contract.service.interfaces;

import com.sni.bokaticowork.features.contract.dto.request.ContractAmendmentSectionRequest;
import com.sni.bokaticowork.features.contract.dto.request.ContractAmendmentVariableRequest;
import com.sni.bokaticowork.features.contract.dto.request.ProposeAmendmentRequest;
import com.sni.bokaticowork.features.contract.dto.request.ReviewAmendmentRequest;
import com.sni.bokaticowork.features.contract.dto.request.SignAmendmentRequest;
import com.sni.bokaticowork.features.contract.dto.response.ContractAmendmentResponse;
import com.sni.bokaticowork.features.contract.dto.response.ContractAmendmentSectionResponse;
import com.sni.bokaticowork.features.contract.dto.response.ContractAmendmentVariableResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;

import java.time.LocalDate;
import java.util.List;

public interface ContractAmendmentService {

    // ── Lifecycle ──────────────────────────────────────────────────────────────
    ContractAmendmentResponse propose(String contractCode, Long proposedBy, ProposeAmendmentRequest request);
    ContractAmendmentResponse getByCode(String amendmentCode);
    List<ContractAmendmentResponse> listByContract(String contractCode);
    ContractAmendmentResponse submitForReview(String amendmentCode);
    ContractAmendmentResponse approveReview(String amendmentCode, Long reviewedBy, ReviewAmendmentRequest request);
    ContractAmendmentResponse rejectReview(String amendmentCode, Long reviewedBy, ReviewAmendmentRequest request);
    ContractAmendmentResponse sign(String amendmentCode, Long actorId, SignAmendmentRequest request);
    ContractAmendmentResponse cancel(String amendmentCode, Long actorId, String reason);

    /**
     * Le renouvellement paye d'un abonnement · un avenant du meme contrat, applique sans signature,
     * qui porte la fin du contrat au terme de la periode payee. Rend vide quand le contrat n'en
     * appelle pas : clos, ou pas encore signe et dont la date est simplement reportee.
     */
    java.util.Optional<ContractAmendmentResponse> applyRenewal(String contractCode, RenewalTerms terms);

    record RenewalTerms(String subscriptionNumber, LocalDate previousStart, LocalDate previousEnd,
                        LocalDate newStart, LocalDate newEnd, LocalDate nextBillingDate) {
    }

    // ── Section content management ─────────────────────────────────────────────
    ContractAmendmentSectionResponse addSection(String amendmentCode, ContractAmendmentSectionRequest request);
    ContractAmendmentSectionResponse updateSection(String amendmentCode, Long sectionId, ContractAmendmentSectionRequest request);
    void removeSection(String amendmentCode, Long sectionId);
    List<ContractAmendmentSectionResponse> listSections(String amendmentCode);

    // ── Variable management ────────────────────────────────────────────────────
    List<ContractAmendmentVariableResponse> setVariables(String amendmentCode, List<ContractAmendmentVariableRequest> variables);
    List<ContractAmendmentVariableResponse> listVariables(String amendmentCode);

    // ── Preview & PDF ──────────────────────────────────────────────────────────
    String previewHtml(String amendmentCode);
    DocumentResponse generatePdf(String amendmentCode, Long uploadedBy);
}
