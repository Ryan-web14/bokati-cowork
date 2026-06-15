package com.sni.bokaticowork.features.portal.wallet.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.payment.dto.response.WalletLedgerEntryResponse;
import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import com.sni.bokaticowork.features.portal.wallet.service.ClientWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPath.V1 + "/client/wallet")
@RequiredArgsConstructor
public class ClientWalletController {

    private final ClientContextService clientContextService;
    private final ClientWalletService clientWalletService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<WalletResponse>> listWallets(
            @PageableDefault(size = 20) Pageable pageable) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientWalletService.listWallets(member, pageable));
    }

    @GetMapping("/{walletNumber}")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable String walletNumber) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientWalletService.getWallet(member, walletNumber));
    }

    @GetMapping("/{walletNumber}/ledger")
    public ResponseEntity<PaginatedResponse<WalletLedgerEntryResponse>> getLedger(
            @PathVariable String walletNumber,
            @PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientWalletService.getLedger(member, walletNumber, pageable));
    }
}
