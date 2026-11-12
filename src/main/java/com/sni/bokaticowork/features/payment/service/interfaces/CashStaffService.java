package com.sni.bokaticowork.features.payment.service.interfaces;

import com.sni.bokaticowork.features.payment.dto.request.CashAbsenceRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashDelegationRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashSealRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashAbsenceResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashDelegationResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashReplacementSuggestionResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSealResponse;
import com.sni.bokaticowork.features.payment.enums.CashAbsenceStatus;

import java.time.LocalDate;
import java.util.List;

/** Personnel de caisse : absences et remplacements, scelles, delegations (lot C2). */
public interface CashStaffService {

    CashAbsenceResponse declareAbsence(CashAbsenceRequest request);

    CashAbsenceResponse approveAbsence(String absenceNumber);

    CashAbsenceResponse cancelAbsence(String absenceNumber);

    CashAbsenceResponse endAbsence(String absenceNumber);

    List<CashAbsenceResponse> absences(String userEmail, CashAbsenceStatus status);

    List<CashReplacementSuggestionResponse> replacementSuggestions(String registerCode, LocalDate from, LocalDate to);

    CashSealResponse seal(String registerCode, CashSealRequest request);

    CashSealResponse unseal(String registerCode, CashSealRequest request);

    List<CashSealResponse> seals(String registerCode);

    CashDelegationResponse delegate(CashDelegationRequest request);

    List<CashDelegationResponse> delegations();

    CashDelegationResponse cancelDelegation(Long id);

    /** Traitement periodique : absences qui commencent ou finissent, rappels, sessions trop longues. */
    void runPeriodicChecks();
}
