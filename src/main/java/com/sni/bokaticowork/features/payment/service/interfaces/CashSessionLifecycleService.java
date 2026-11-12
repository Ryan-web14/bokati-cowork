package com.sni.bokaticowork.features.payment.service.interfaces;

import com.sni.bokaticowork.features.payment.dto.request.CashCountRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashDenominationRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashHandoverAnswerRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashHandoverRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashReasonRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashVarianceDecisionRequest;
import com.sni.bokaticowork.features.payment.dto.request.ResumeCashSessionRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashCountResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashDenominationResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashExpectedOpeningResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashHandoverResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionStepResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashVarianceReviewResponse;
import com.sni.bokaticowork.features.payment.enums.CashVarianceStatus;
import com.sni.bokaticowork.features.payment.model.CashSession;

import java.math.BigDecimal;
import java.util.List;

/** Cycle de vie complet d'une session de caisse et ses comptages (lot C2 du plan finance). */
public interface CashSessionLifecycleService {

    CashExpectedOpeningResponse expectedOpening(String registerCode);

    /** Enregistre le comptage d'ouverture d'une session qui vient d'etre creee. */
    void recordOpeningCount(CashSession session, CashCountRequest count, BigDecimal expected);

    CashSessionStepResponse confirmOpening(String sessionNumber, CashReasonRequest request);

    CashSessionStepResponse pause(String sessionNumber);

    CashSessionStepResponse resume(String sessionNumber, ResumeCashSessionRequest request);

    CashSessionStepResponse suspend(String sessionNumber, CashReasonRequest request);

    /** Suspension par le systeme (fin de mois, absence) : sans motif de liste. */
    void suspendBySystem(String sessionNumber, String reason);

    CashSessionStepResponse unsuspend(String sessionNumber);

    CashSessionStepResponse startClosing(String sessionNumber);

    /** Comptage de cloture ou de recomptage : calcule l'ecart, puis clot ou met en revue. */
    CashSessionStepResponse submitClosingCount(String sessionNumber, CashCountRequest request);

    CashSessionStepResponse requestRecount(String sessionNumber, CashReasonRequest request);

    CashSessionStepResponse decideReview(String reviewNumber, CashVarianceDecisionRequest request);

    /** Decision sur l'ecart en revue d'une session (forme historique {@code approve-variance}). */
    CashSessionStepResponse decideSessionVariance(String sessionNumber, CashVarianceDecisionRequest request);

    List<CashVarianceReviewResponse> reviews(CashVarianceStatus status, String registerCode, String sessionNumber);

    CashSessionStepResponse spotCheck(String sessionNumber, CashCountRequest request);

    CashSessionStepResponse forceClose(String sessionNumber, CashCountRequest request, CashReasonRequest reason);

    CashSessionStepResponse contest(String sessionNumber, CashReasonRequest request);

    CashSessionStepResponse cancel(String sessionNumber, CashReasonRequest request);

    List<CashCountResponse> counts(String sessionNumber);

    CashHandoverResponse startHandover(String sessionNumber, CashHandoverRequest request);

    CashSessionStepResponse acceptHandover(String handoverNumber, CashHandoverAnswerRequest request);

    CashSessionStepResponse disputeHandover(String handoverNumber, CashHandoverAnswerRequest request);

    CashHandoverResponse cancelHandover(String handoverNumber);

    List<CashDenominationResponse> denominations(String currency, boolean includeRetired);

    List<CashDenominationResponse> replaceDenominations(String currency, List<CashDenominationRequest> values);
}
