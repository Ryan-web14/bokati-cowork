package com.sni.bokaticowork.features.payment.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
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
import com.sni.bokaticowork.features.payment.service.interfaces.CashRegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping(ApiPath.V1 + "/cash-registers")
@RequiredArgsConstructor
public class CashRegisterController {

    private final CashRegisterService cashRegisterService;

    @PostMapping
    public ResponseEntity<CashRegisterResponse> createRegister(@Valid @RequestBody CreateCashRegisterRequest request) {
        return ResponseEntity.ok(cashRegisterService.createRegister(request));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<CashRegisterResponse>> listRegisters(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) String businessEntityCode,
            @RequestParam(required = false) String searchText,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(cashRegisterService.listRegisters(active, locationCode, businessEntityCode, searchText, pageable));
    }

    @PatchMapping("/{registerCode}/activate")
    public ResponseEntity<CashRegisterResponse> activateRegister(@PathVariable String registerCode) {
        return ResponseEntity.ok(cashRegisterService.activateRegister(registerCode));
    }

    @PatchMapping("/{registerCode}/deactivate")
    public ResponseEntity<CashRegisterResponse> deactivateRegister(@PathVariable String registerCode) {
        return ResponseEntity.ok(cashRegisterService.deactivateRegister(registerCode));
    }

    @PostMapping("/sessions")
    public ResponseEntity<CashSessionResponse> openSession(@Valid @RequestBody OpenCashSessionRequest request) {
        return ResponseEntity.ok(cashRegisterService.openSession(request));
    }

    @GetMapping("/sessions")
    public ResponseEntity<PaginatedResponse<CashSessionResponse>> listSessions(
            @RequestParam(required = false) String registerCode,
            @RequestParam(required = false) CashSessionStatus status,
            @RequestParam(required = false) String openedBy,
            @RequestParam(required = false) String closedBy,
            @RequestParam(required = false) String searchText,
            @PageableDefault(size = 20, sort = "openedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(cashRegisterService.listSessions(registerCode, status, openedBy, closedBy, searchText, pageable));
    }

    @PostMapping("/sessions/{sessionNumber}/closing-request")
    public ResponseEntity<CashSessionResponse> requestClosing(@PathVariable String sessionNumber,
                                                              @Valid @RequestBody CloseCashSessionRequest request) {
        return ResponseEntity.ok(cashRegisterService.closeSession(sessionNumber, request));
    }

    @PatchMapping("/sessions/{sessionNumber}/close")
    public ResponseEntity<CashSessionResponse> closeSession(@PathVariable String sessionNumber,
                                                            @Valid @RequestBody CloseCashSessionRequest request) {
        return ResponseEntity.ok(cashRegisterService.closeSession(sessionNumber, request));
    }

    @PatchMapping("/sessions/{sessionNumber}/approve-variance")
    public ResponseEntity<CashSessionResponse> approveVariance(@PathVariable String sessionNumber,
                                                               @Valid @RequestBody ApproveCashVarianceRequest request) {
        return ResponseEntity.ok(cashRegisterService.approveVariance(sessionNumber, request));
    }

    @GetMapping("/sessions/{sessionNumber}/summary")
    public ResponseEntity<CashSessionSummaryResponse> sessionSummary(@PathVariable String sessionNumber) {
        return ResponseEntity.ok(cashRegisterService.sessionSummary(sessionNumber));
    }

    @PostMapping("/sessions/{sessionNumber}/movements")
    public ResponseEntity<CashMovementResponse> createMovement(@PathVariable String sessionNumber,
                                                               @Valid @RequestBody CreateCashMovementRequest request) {
        return ResponseEntity.ok(cashRegisterService.createMovement(sessionNumber, request));
    }

    @PostMapping("/sessions/{sessionNumber}/entry-vouchers")
    public ResponseEntity<CashMovementResponse> createEntryVoucher(@PathVariable String sessionNumber,
                                                                   @Valid @RequestBody CreateCashVoucherRequest request) {
        return ResponseEntity.ok(cashRegisterService.createEntryVoucher(sessionNumber, request));
    }

    @PostMapping("/sessions/{sessionNumber}/exit-vouchers")
    public ResponseEntity<CashMovementResponse> createExitVoucher(@PathVariable String sessionNumber,
                                                                  @Valid @RequestBody CreateCashVoucherRequest request) {
        return ResponseEntity.ok(cashRegisterService.createExitVoucher(sessionNumber, request));
    }

    @GetMapping("/movements")
    public ResponseEntity<PaginatedResponse<CashMovementResponse>> listMovements(
            @RequestParam(required = false) String registerCode,
            @RequestParam(required = false) String sessionNumber,
            @RequestParam(required = false) CashMovementType movementType,
            @RequestParam(required = false) CashDocumentType documentType,
            @RequestParam(required = false) String documentNumber,
            @RequestParam(required = false) String flowCategory,
            @RequestParam(required = false) String referenceType,
            @RequestParam(required = false) String referenceCode,
            @RequestParam(required = false) String counterpartyCode,
            @RequestParam(required = false) String counterpartyName,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate,
            @RequestParam(required = false) String searchText,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(cashRegisterService.listMovements(registerCode, sessionNumber, movementType,
                documentType, documentNumber, flowCategory, referenceType, referenceCode,
                counterpartyCode, counterpartyName, createdBy, fromDate, toDate, searchText, pageable));
    }

    @GetMapping("/metrics/overview")
    public ResponseEntity<CashMetricsOverviewResponse> overviewMetrics(
            @RequestParam(required = false) String registerCode,
            @RequestParam(required = false) String businessEntityCode,
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate) {
        return ResponseEntity.ok(cashRegisterService.overviewMetrics(registerCode, businessEntityCode, fromDate, toDate));
    }

    @GetMapping("/metrics/registers")
    public ResponseEntity<java.util.List<CashRegisterMetricsResponse>> registerMetrics(
            @RequestParam(required = false) String businessEntityCode,
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate) {
        return ResponseEntity.ok(cashRegisterService.registerMetrics(businessEntityCode, fromDate, toDate));
    }
}
