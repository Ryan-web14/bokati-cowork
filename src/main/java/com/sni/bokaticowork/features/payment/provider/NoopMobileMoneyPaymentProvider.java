package com.sni.bokaticowork.features.payment.provider;

import org.springframework.stereotype.Component;

@Component
public class NoopMobileMoneyPaymentProvider implements MobileMoneyPaymentProvider {

    @Override
    public MobileMoneyInitiationResponse initiate(MobileMoneyInitiationRequest request) {
        return new MobileMoneyInitiationResponse(null, "NOT_IMPLEMENTED", "No mobile money provider is configured");
    }

    @Override
    public MobileMoneyStatusResponse checkStatus(String providerReference) {
        return new MobileMoneyStatusResponse(providerReference, "NOT_IMPLEMENTED", "Mobile money provider is not implemented yet");
    }

    @Override
    public MobileMoneyRefundResponse refund(MobileMoneyRefundRequest request) {
        return new MobileMoneyRefundResponse(request.providerReference(), null, "NOT_IMPLEMENTED", "Mobile money provider is not implemented yet");
    }
}
