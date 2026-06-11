package com.sni.bokaticowork.features.payment.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.dto.request.CreateCashRequestRequest;
import com.sni.bokaticowork.features.payment.dto.request.ReviewCashRequestRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashRequestResponse;
import com.sni.bokaticowork.features.payment.enums.CashRequestStatus;
import com.sni.bokaticowork.features.payment.enums.CashRequestType;
import com.sni.bokaticowork.features.payment.service.interfaces.CashRequestService;
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

@RestController
@RequestMapping(ApiPath.V1 + "/cash-registers")
@RequiredArgsConstructor
public class CashRequestController {

    private final CashRequestService cashRequestService;

    @PostMapping("/sessions/{sessionNumber}/requests")
    public ResponseEntity<CashRequestResponse> submit(@PathVariable String sessionNumber,
                                                       @Valid @RequestBody CreateCashRequestRequest request) {
        return ResponseEntity.ok(cashRequestService.submit(sessionNumber, request));
    }

    @GetMapping("/requests")
    public ResponseEntity<PaginatedResponse<CashRequestResponse>> list(
            @RequestParam(required = false) String registerCode,
            @RequestParam(required = false) String sessionNumber,
            @RequestParam(required = false) CashRequestType requestType,
            @RequestParam(required = false) CashRequestStatus status,
            @RequestParam(required = false) String requestedBy,
            @RequestParam(required = false) String searchText,
            @PageableDefault(size = 20, sort = "requestedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(cashRequestService.list(registerCode, sessionNumber, requestType, status, requestedBy, searchText, pageable));
    }

    @GetMapping("/requests/{requestNumber}")
    public ResponseEntity<CashRequestResponse> get(@PathVariable String requestNumber) {
        return ResponseEntity.ok(cashRequestService.get(requestNumber));
    }

    @PatchMapping("/requests/{requestNumber}/approve")
    public ResponseEntity<CashRequestResponse> approve(@PathVariable String requestNumber,
                                                        @Valid @RequestBody ReviewCashRequestRequest request) {
        return ResponseEntity.ok(cashRequestService.approve(requestNumber, request));
    }

    @PatchMapping("/requests/{requestNumber}/reject")
    public ResponseEntity<CashRequestResponse> reject(@PathVariable String requestNumber,
                                                       @Valid @RequestBody ReviewCashRequestRequest request) {
        return ResponseEntity.ok(cashRequestService.reject(requestNumber, request));
    }
}
