package com.sni.bokaticowork.features.portal.wallet.service;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.payment.dto.response.WalletLedgerEntryResponse;
import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClientWalletService {

    private static final String OWNER_TYPE = "MEMBER";

    private final WalletService walletService;

    @Transactional(readOnly = true)
    public PaginatedResponse<WalletResponse> listWallets(Member member, Pageable pageable) {
        return walletService.list(OWNER_TYPE, member.getMemberId(), pageable);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(Member member, String walletNumber) {
        WalletResponse wallet = walletService.get(walletNumber);
        verifyOwnership(member, wallet);
        return wallet;
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<WalletLedgerEntryResponse> getLedger(Member member, String walletNumber,
                                                                    Pageable pageable) {
        WalletResponse wallet = walletService.get(walletNumber);
        verifyOwnership(member, wallet);
        return walletService.ledger(walletNumber, pageable);
    }

    private void verifyOwnership(Member member, WalletResponse wallet) {
        if (!OWNER_TYPE.equals(wallet.ownerType())
                || !member.getMemberId().equals(wallet.ownerCode())) {
            throw new ResourceNotFoundException("Wallet not found");
        }
    }
}
