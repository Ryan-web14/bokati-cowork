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
    void recordAutomaticPayment(PaymentTransaction transaction);
    CashMovementResponse addAttachment(String movementNumber, AddCashMovementAttachmentRequest request);
    CashMovementResponse signMovement(String movementNumber, String signedBy);
    CashMovementResponse markMovementPrinted(String movementNumber);
}
