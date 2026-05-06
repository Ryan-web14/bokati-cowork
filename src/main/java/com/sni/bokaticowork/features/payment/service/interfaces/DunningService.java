package com.sni.bokaticowork.features.payment.service.interfaces;

import com.sni.bokaticowork.features.payment.model.PaymentDunningAttempt;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;

public interface DunningService {
    void scheduleForFailedIntent(PaymentIntent intent, String subscriptionNumber);
    void executeAttempt(PaymentDunningAttempt attempt);
}