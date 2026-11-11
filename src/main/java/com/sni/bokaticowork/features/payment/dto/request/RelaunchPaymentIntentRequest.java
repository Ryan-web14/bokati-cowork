package com.sni.bokaticowork.features.payment.dto.request;

import java.time.Instant;

/**
 * @param expiresAt nouvelle echeance ; absente, la duree de validite d'origine repart de maintenant
 */
public record RelaunchPaymentIntentRequest(Instant expiresAt) {
}
