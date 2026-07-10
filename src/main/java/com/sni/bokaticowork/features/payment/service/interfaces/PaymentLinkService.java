package com.sni.bokaticowork.features.payment.service.interfaces;

import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentLinkRequest;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentLinkResponse;

public interface PaymentLinkService {
    PaymentLinkResponse createLink(String intentNumber, CreatePaymentLinkRequest request);
    PaymentIntentResponse resolve(String token);
}
