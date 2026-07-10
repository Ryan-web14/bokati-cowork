package com.sni.bokaticowork.features.payment.provider.pawaypay.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request body for PawaPay's hosted Payment Page (checkout) · POST /v2/paymentpage.
 * The customer is redirected to the returned redirectUrl to complete the payment,
 * and PawaPay fires the standard deposit callback with the final status.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PawapayPaymentPageRequest(
        String depositId,
        String returnUrl,
        String amount,
        String country,
        String reason,
        String language
) {
}
