package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

/** Ce qu'on peut faire d'une intention selon son etat · une seule regle pour l'ecran et le service. */
public final class PaymentIntentStates {

    /** Une intention encore en cours, que l'on peut annuler. */
    public static final Set<PaymentIntentStatus> OPEN =
            Set.of(PaymentIntentStatus.PENDING, PaymentIntentStatus.PROCESSING, PaymentIntentStatus.AUTHORIZED);

    /** Une intention close sans avoir ete reglee, que l'on peut relancer. */
    public static final Set<PaymentIntentStatus> RELAUNCHABLE =
            Set.of(PaymentIntentStatus.EXPIRED, PaymentIntentStatus.CANCELLED, PaymentIntentStatus.FAILED);

    private PaymentIntentStates() {
    }

    /** Ouverte sur le papier mais echeance depassee, avant le passage du worker d'expiration. */
    public static boolean lapsed(PaymentIntent intent) {
        return intent.getExpiresAt() != null && intent.getExpiresAt().isBefore(Instant.now())
                && OPEN.contains(intent.getStatus());
    }

    public static boolean cancellable(PaymentIntent intent, BigDecimal processingAmount) {
        return OPEN.contains(intent.getStatus()) && !lapsed(intent) && !positive(processingAmount);
    }

    public static boolean relaunchable(PaymentIntent intent, BigDecimal remainingAmount, BigDecimal processingAmount) {
        return (RELAUNCHABLE.contains(intent.getStatus()) || lapsed(intent))
                && positive(remainingAmount)
                && !positive(processingAmount);
    }

    private static boolean positive(BigDecimal amount) {
        return amount != null && amount.signum() > 0;
    }
}
