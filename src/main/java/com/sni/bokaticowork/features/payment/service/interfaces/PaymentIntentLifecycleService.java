package com.sni.bokaticowork.features.payment.service.interfaces;

import com.sni.bokaticowork.features.payment.dto.request.CancelPaymentIntentRequest;
import com.sni.bokaticowork.features.payment.dto.request.RelaunchPaymentIntentRequest;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;

/** Annuler une intention en cours, relancer une intention expiree, annulee ou en echec. */
public interface PaymentIntentLifecycleService {

    PaymentIntentResponse cancel(String intentNumber, CancelPaymentIntentRequest request);

    PaymentIntentResponse relaunch(String intentNumber, RelaunchPaymentIntentRequest request);
}
