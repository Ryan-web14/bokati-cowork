package com.sni.bokaticowork.features.payment.service.interfaces;

import com.sni.bokaticowork.features.payment.dto.request.CashExchangeRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashNatureRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashReasonRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashTransferReceiveRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashTransferRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashMovementResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashNatureResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashPettyRefillResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashTransferResponse;
import com.sni.bokaticowork.features.payment.enums.CashFlowDirection;
import com.sni.bokaticowork.features.payment.enums.CashNatureFamily;
import com.sni.bokaticowork.features.payment.enums.CashTransferStatus;
import com.sni.bokaticowork.features.payment.model.CashSession;

import java.math.BigDecimal;
import java.util.List;

/** Operations de caisse et transferts internes (lot C3 du plan finance). */
public interface CashOperationsService {

    List<CashNatureResponse> natures(CashFlowDirection direction, CashNatureFamily family, String search, boolean includeInactive);

    List<CashNatureResponse> replaceNatures(List<CashNatureRequest> natures);

    CashMovementResponse approveMovement(String movementNumber);

    /** Visa donne par un circuit qui a deja eu son second regard (demande de caisse approuvee). */
    void approveInternally(String movementNumber, String approver);

    CashMovementResponse cancelMovement(String movementNumber, CashReasonRequest request);

    CashMovementResponse reverseMovement(String movementNumber, CashReasonRequest request);

    CashMovementResponse exchange(String sessionNumber, CashExchangeRequest request);

    CashTransferResponse sendTransfer(CashTransferRequest request);

    CashTransferResponse receiveTransfer(String transferNumber, CashTransferReceiveRequest request);

    CashTransferResponse cancelTransfer(String transferNumber, CashReasonRequest request);

    List<CashTransferResponse> transfers(String registerCode, CashTransferStatus status);

    CashPettyRefillResponse pettyRefillProposal(String registerCode);

    CashTransferResponse pettyRefill(String registerCode, String carrierEmail);

    /** Remise a la caisse mere a la cloture : transfert en transit depuis la session close. */
    CashTransferResponse remitAtClosing(CashSession session, BigDecimal amount, String actor);

    /** Traitement periodique : visas expires, transferts trop longtemps en transit. */
    void runPeriodicChecks();
}
