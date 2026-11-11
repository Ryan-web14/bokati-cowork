package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.Size;

public record CancelPaymentIntentRequest(@Size(max = 500) String reason) {
}
