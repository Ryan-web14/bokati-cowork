package com.sni.bokaticowork.features.payment.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.dto.request.CreateWalletHoldRequest;
import com.sni.bokaticowork.features.payment.dto.response.WalletHoldResponse;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletHoldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPath.V1 + "/wallet-holds")
@RequiredArgsConstructor
public class WalletHoldController {

    private final WalletHoldService walletHoldService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<WalletHoldResponse>> list(
            @RequestParam(required = false) String walletNumber,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String sourceCode,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(walletHoldService.list(walletNumber, status, sourceType, sourceCode, pageable));
    }

    @PostMapping
    public ResponseEntity<WalletHoldResponse> create(@Valid @RequestBody CreateWalletHoldRequest request) {
        return ResponseEntity.ok(walletHoldService.create(request));
    }

    @PatchMapping("/{holdNumber}/capture")
    public ResponseEntity<WalletHoldResponse> capture(@PathVariable String holdNumber,
                                                      @RequestParam(required = false) String createdBy) {
        return ResponseEntity.ok(walletHoldService.capture(holdNumber, createdBy));
    }

    @PatchMapping("/{holdNumber}/release")
    public ResponseEntity<WalletHoldResponse> release(@PathVariable String holdNumber,
                                                      @RequestParam(required = false) String createdBy) {
        return ResponseEntity.ok(walletHoldService.release(holdNumber, createdBy));
    }
}
