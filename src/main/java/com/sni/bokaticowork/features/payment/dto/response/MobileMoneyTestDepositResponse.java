package com.sni.bokaticowork.features.payment.dto.response;

import java.util.List;

public record MobileMoneyTestDepositResponse(
        MobileMoneyDepositResponse deposit,
        PaymentIntentResponse intent,
        List<MobileMoneyProviderOptionResponse> providers
) {
}
