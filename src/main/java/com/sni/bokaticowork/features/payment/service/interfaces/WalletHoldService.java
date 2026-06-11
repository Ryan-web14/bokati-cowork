package com.sni.bokaticowork.features.payment.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.payment.dto.request.CreateWalletHoldRequest;
import com.sni.bokaticowork.features.payment.dto.response.WalletHoldResponse;
import org.springframework.data.domain.Pageable;

public interface WalletHoldService {
    PaginatedResponse<WalletHoldResponse> list(String walletNumber, String status, String sourceType, String sourceCode, Pageable pageable);
    WalletHoldResponse create(CreateWalletHoldRequest request);
    WalletHoldResponse capture(String holdNumber, String createdBy);
    WalletHoldResponse release(String holdNumber, String createdBy);
    int expireDueHolds();
}
