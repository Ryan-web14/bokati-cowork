package com.sni.bokaticowork.features.payment.provider;

public interface MobileMoneyPaymentProvider {
    MobileMoneyInitiationResponse initiate(MobileMoneyInitiationRequest request);
    MobileMoneyStatusResponse checkStatus(String providerReference);
    MobileMoneyRefundResponse refund(MobileMoneyRefundRequest request);
}
