package com.sni.bokaticowork.features.payment.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.payment.dto.request.WalletTopUpRequest;
import com.sni.bokaticowork.features.payment.dto.response.WalletLedgerEntryResponse;
import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface WalletService {
    WalletResponse adminTopUp(WalletTopUpRequest request);
    WalletResponse getOrCreate(String ownerType, String ownerCode, String currency);
    WalletResponse get(String walletNumber);
    PaginatedResponse<WalletResponse> list(String ownerType, String ownerCode, Pageable pageable);
    PaginatedResponse<WalletLedgerEntryResponse> ledger(String walletNumber, Pageable pageable);
    WalletAccount serviceWallet(String walletNumber);
    void credit(WalletAccount wallet, BigDecimal amount, WalletEntryType entryType, String sourceType, String sourceCode, String reference, String createdBy);
    void debit(WalletAccount wallet, BigDecimal amount, WalletEntryType entryType, String sourceType, String sourceCode, String reference, String createdBy);
}
