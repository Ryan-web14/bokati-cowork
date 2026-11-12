package com.sni.bokaticowork.features.subscription.promotion.pricing.referral.event;

import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.service.ReferralService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

/**
 * Qualifie les parrainages d'un filleul qui vient de payer.
 *
 * <p>Apres la validation du paiement, dans sa propre transaction : la recompense credite des
 * portefeuilles, et le paiement peut tenir le verrou de celui du filleul (rechargement) ; dans la
 * meme transaction, l'attente serait sans fin. Un echec ici est journalise et ne remet jamais le
 * paiement en cause ; la qualification reste rejouable (seuls les parrainages en attente sont
 * pris), au paiement suivant.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReferralPaymentListener {

    private final ReferralService referralService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRefereePaid(RefereePaidEvent event) {
        if (!StringUtils.hasText(event.customerType()) || !StringUtils.hasText(event.customerCode())) {
            return;
        }
        try {
            int qualified = referralService.qualifyOnFirstPayment(event.customerType(), event.customerCode());
            if (qualified > 0) {
                log.info("Parrainage · {} parrainage(s) qualifie(s) pour {} {} apres le paiement {}",
                        qualified, event.customerType(), event.customerCode(), event.transactionNumber());
            }
        } catch (RuntimeException ex) {
            log.error("Parrainage · qualification en echec pour {} {} apres le paiement {}",
                    event.customerType(), event.customerCode(), event.transactionNumber(), ex);
        }
    }
}
