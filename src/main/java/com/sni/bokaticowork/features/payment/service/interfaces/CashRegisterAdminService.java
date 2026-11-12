package com.sni.bokaticowork.features.payment.service.interfaces;

import com.sni.bokaticowork.features.payment.dto.request.CashDeviceDecisionRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashReferenceValueRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashRegisterAssignmentRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashRegisterStatusRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreateResponsibilityTransferRequest;
import com.sni.bokaticowork.features.payment.dto.request.EmergencyAssignmentRequest;
import com.sni.bokaticowork.features.payment.dto.request.UpdateCashRegisterRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashFormDefaultsResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashPendingItemResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashReferenceValueResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashRegisterAssignmentResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashRegisterDetailResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashRegisterHistoryResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashResponsibilityTransferResponse;

import java.util.List;
import java.util.Map;

/** Referentiel des caisses (lot C1 du plan finance). */
public interface CashRegisterAdminService {

    CashRegisterDetailResponse get(String registerCode);

    CashRegisterDetailResponse update(String registerCode, UpdateCashRegisterRequest request);

    CashRegisterDetailResponse suspend(String registerCode, CashRegisterStatusRequest request);

    CashRegisterDetailResponse reactivate(String registerCode);

    CashRegisterDetailResponse close(String registerCode, CashRegisterStatusRequest request);

    List<CashRegisterHistoryResponse> history(String registerCode);

    List<CashRegisterAssignmentResponse> assignments(String registerCode, boolean includeEnded);

    List<CashRegisterAssignmentResponse> replaceAssignments(String registerCode, List<CashRegisterAssignmentRequest> requests);

    CashRegisterAssignmentResponse emergencyAssignment(String registerCode, EmergencyAssignmentRequest request);

    CashRegisterDetailResponse decideDevice(String registerCode, String deviceCode, CashDeviceDecisionRequest request);

    List<CashPendingItemResponse> pendingItems(String registerCode);

    CashResponsibilityTransferResponse createResponsibilityTransfer(String registerCode, CreateResponsibilityTransferRequest request);

    List<CashResponsibilityTransferResponse> responsibilityTransfers(String registerCode);

    CashResponsibilityTransferResponse signResponsibilityTransfer(String transferNumber);

    Map<String, List<CashReferenceValueResponse>> referenceLists(boolean includeInactive);

    List<CashReferenceValueResponse> referenceList(String listCode, boolean includeInactive);

    List<CashReferenceValueResponse> replaceReferenceList(String listCode, List<CashReferenceValueRequest> values);

    CashFormDefaultsResponse formDefaults(String context, Map<String, String> parameters);

    /** Caisses sur lesquelles l'utilisateur connecte est affecte maintenant. */
    List<CashRegisterDetailResponse> myRegisters();
}
