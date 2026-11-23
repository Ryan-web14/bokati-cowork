package com.sni.bokaticowork.features.payment.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.payment.dto.request.AddCashMovementAttachmentRequest;
import com.sni.bokaticowork.features.payment.dto.request.ApproveCashVarianceRequest;
import com.sni.bokaticowork.features.payment.dto.request.CloseCashSessionRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreateCashMovementRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreateCashRegisterRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreateCashVoucherRequest;
import com.sni.bokaticowork.features.payment.dto.request.OpenCashSessionRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashMovementResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashMetricsOverviewResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashRegisterResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashRegisterMetricsResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionSummaryResponse;
import com.sni.bokaticowork.features.payment.enums.CashDocumentType;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;

public interface CashRegisterService {
    CashRegisterResponse createRegister(CreateCashRegisterRequest request);
    CashRegisterResponse activateRegister(String registerCode);
    CashRegisterResponse deactivateRegister(String registerCode);
    PaginatedResponse<CashRegisterResponse> listRegisters(Boolean active, String locationCode, String businessEntityCode, String searchText, Pageable pageable);
    CashSessionResponse openSession(OpenCashSessionRequest request);
    PaginatedResponse<CashSessionResponse> listSessions(String registerCode, CashSessionStatus status,
                                                        String openedBy, String closedBy, String searchText, Pageable pageable);
    CashSessionResponse closeSession(String sessionNumber, CloseCashSessionRequest request);
    CashSessionResponse approveVariance(String sessionNumber, ApproveCashVarianceRequest request);

    /**
     * Clot par le systeme une session laissee ouverte d'un mois sur l'autre, et en rouvre une pour
     * le meme caissier · le montant attendu y est reporte en fonds de caisse.
     *
     * @return la session ouverte a sa suite
     */
    CashSessionResponse closeForMonthEnd(String sessionNumber);

    /**
     * Cree au besoin la caisse par defaut et lui ouvre une session tenue par le systeme. Appele au
     * demarrage ; sans effet si tout existe deja.
     */
    CashRegisterResponse ensureDefaultRegister();

    /**
     * Session ouverte de la caisse par defaut, qui recoit les paiements saisis sans session de
     * caisse ; ouverte au besoin.
     */
    String defaultSessionNumber();

    /**
     * Ouvre la session de l'entrant d'une passation, avec le montant retenu comme fonds (lot C2).
     * Appele par le cycle de vie des sessions, jamais directement par une route.
     */
    CashSessionResponse openSessionFromHandover(String registerCode, String incoming, BigDecimal amount,
                                                String handoverNumber, String deviceCode);
    CashSessionSummaryResponse sessionSummary(String sessionNumber);
    CashMovementResponse createMovement(String sessionNumber, CreateCashMovementRequest request);
    CashMovementResponse createEntryVoucher(String sessionNumber, CreateCashVoucherRequest request);
    CashMovementResponse createExitVoucher(String sessionNumber, CreateCashVoucherRequest request);
    PaginatedResponse<CashMovementResponse> listMovements(String registerCode, String sessionNumber, CashMovementType movementType,
                                                          CashDocumentType documentType, String documentNumber, String flowCategory,
                                                          String referenceType, String referenceCode, String counterpartyCode, String counterpartyName, String createdBy,
                                                          Instant fromDate, Instant toDate, String searchText, Pageable pageable);
    CashMovementResponse getMovement(String movementNumber);
    CashMetricsOverviewResponse overviewMetrics(String registerCode, String businessEntityCode, Instant fromDate, Instant toDate);
    java.util.List<CashRegisterMetricsResponse> registerMetrics(String businessEntityCode, Instant fromDate, Instant toDate);
    void recordPayment(String sessionNumber, BigDecimal amount, String referenceCode, String createdBy);

    /**
     * Rattache a la bonne session un paiement en especes saisi dans la mauvaise : sortie de la
     * session d'origine, entree dans la session cible (motif, meme montant), transaction rattachee a
     * la cible. Les soldes courants des deux sessions restent justes.
     */
    ReassignedPayment reassignPayment(String sessionNumber, String transactionNumber, String targetSessionNumber,
                                      String reason, String actor);

    /** Resultat d'une reaffectation. */
    record ReassignedPayment(String transactionNumber, String fromSession, String toSession, BigDecimal amount,
                             String exitMovementNumber, String entryMovementNumber) {
    }

    /** Solde de portefeuille rendu en especes au titulaire : sortie de la session (action RESTITUTE visee). */
    void recordWalletRestitution(String sessionNumber, BigDecimal amount, String walletNumber, String actionNumber, String createdBy);

    /** Especes rendues a un client (remboursement d'un paiement en especes) : sortie de la session. */
    void recordRefund(String sessionNumber, BigDecimal amount, String referenceCode, String createdBy);
    void recordAutomaticPayment(PaymentTransaction transaction);

    /** Refuse une session close ou tenue par le systeme · pour rattacher un paiement saisi au guichet. */
    void assertCounterSessionOpen(String sessionNumber);
    CashMovementResponse addAttachment(String movementNumber, AddCashMovementAttachmentRequest request);
    CashMovementResponse signMovement(String movementNumber, String signedBy);
    CashMovementResponse markMovementPrinted(String movementNumber);
}
