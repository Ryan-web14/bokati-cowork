package com.sni.bokaticowork.features.payment.service.interfaces;

import com.sni.bokaticowork.features.payment.dto.request.CreateWalletHoldRequest;
import com.sni.bokaticowork.features.payment.dto.response.WalletHoldResponse;

public interface WalletHoldService {
    WalletHoldResponse create(CreateWalletHoldRequest request);
    WalletHoldResponse capture(String holdNumber, String createdBy);
    WalletHoldResponse release(String holdNumber, String createdBy);
    int expireDueHolds();
}
