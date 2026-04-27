package com.sni.bokaticowork.features.payment.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.dto.request.WalletLookupRequest;
import com.sni.bokaticowork.features.payment.dto.request.WalletTopUpRequest;
import com.sni.bokaticowork.features.payment.dto.response.WalletLedgerEntryResponse;
import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPath.V1 + "/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<WalletResponse>> list(
            @RequestParam(required = false) String ownerType,
            @RequestParam(required = false) String ownerCode,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(walletService.list(ownerType, ownerCode, pageable));
    }

    @PostMapping("/admin/top-up")
    public ResponseEntity<WalletResponse> adminTopUp(@Valid @RequestBody WalletTopUpRequest request) {
        return ResponseEntity.ok(walletService.adminTopUp(request));
    }

    @PostMapping("/admin/get-or-create")
    public ResponseEntity<WalletResponse> getOrCreate(@Valid @RequestBody WalletLookupRequest request) {
        return ResponseEntity.ok(walletService.getOrCreate(request.ownerType(), request.ownerCode(), request.currency()));
    }

    @GetMapping("/{walletNumber}")
    public ResponseEntity<WalletResponse> get(@PathVariable String walletNumber) {
        return ResponseEntity.ok(walletService.get(walletNumber));
    }

    @GetMapping("/{walletNumber}/ledger")
    public ResponseEntity<PaginatedResponse<WalletLedgerEntryResponse>> ledger(
            @PathVariable String walletNumber,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(walletService.ledger(walletNumber, pageable));
    }
}
