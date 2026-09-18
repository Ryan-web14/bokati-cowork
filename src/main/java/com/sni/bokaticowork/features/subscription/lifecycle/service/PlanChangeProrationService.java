package com.sni.bokaticowork.features.subscription.lifecycle.service;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import com.sni.bokaticowork.features.subscription.change.model.SubscriptionChangeRequest;
import com.sni.bokaticowork.features.subscription.lifecycle.model.ProrationPolicy;
import com.sni.bokaticowork.features.subscription.repository.PlanPriceRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionEventType;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.PlanPriceAmountCalculator;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionBillingSupport;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionEventWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Le changement de plan en cours de periode · un prorata explicite, dans les deux sens.
 *
 * <p>Le reste a courir a l'ancien prix est rendu, le reste a courir au nouveau prix est du ; la
 * difference est facturee si elle est positive, creditee au portefeuille si elle est negative ·
 * une avance client, pas un avoir a rapprocher. La politique de prorata est celle de tout le
 * module, et elle est memorisee sur le changement.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanChangeProrationService {

    static final String CHARGE_SOURCE = "PLAN_CHANGE_PRORATION";
    static final String CREDIT_SOURCE = "PLAN_CHANGE";

    private final SubscriptionPolicyService policyService;
    private final ProrationCalculator proration;
    private final PlanPriceRepository planPriceRepository;
    private final PlanPriceAmountCalculator amountCalculator;
    private final SubscriptionBillingSupport billingSupport;
    private final WalletService walletService;
    private final SubscriptionEventWriter eventWriter;

    public record Preview(ProrationPolicy policy, LocalDate effectiveDate, BigDecimal currentPeriodTotal, BigDecimal targetPeriodTotal,
                          BigDecimal unusedAtCurrentPrice, BigDecimal remainingAtTargetPrice, BigDecimal amount, boolean credit,
                          PlanPriceAmountCalculator.Amounts targetAmounts, String currency) {
    }

    @Transactional(readOnly = true)
    public Preview preview(Subscription subscription, PlanVersion target, LocalDate effectiveDate) {
        ProrationPolicy policy = policyService.current().getProrationPolicy();
        LocalDate effective = effectiveDate == null ? LocalDate.now() : effectiveDate;
        PlanPrice targetPrice = planPriceRepository.findAllByPlanVersion(target.getId()).stream()
                .filter(p -> p.getBillingCycle() == subscription.getBillingCycle()).findFirst()
                .orElseThrow(() -> new ConflictException("plan change", "la version cible n'a pas de prix pour le cycle " + subscription.getBillingCycle()));
        PlanPriceAmountCalculator.Amounts targetAmounts = amountCalculator.compute(targetPrice.getAmount(), BigDecimal.ZERO, BigDecimal.ZERO, targetPrice.getTaxIncluded());

        BigDecimal currentTotal = subscription.getTotalAmount() == null ? BigDecimal.ZERO : subscription.getTotalAmount();
        BigDecimal unused = proration.remaining(currentTotal, subscription.getCurrentPeriodStart(), subscription.getCurrentPeriodEnd(), effective, policy);
        BigDecimal remainingNew = policy == ProrationPolicy.NONE ? BigDecimal.ZERO
                : proration.share(targetAmounts.total(), subscription.getCurrentPeriodStart(), subscription.getCurrentPeriodEnd(), effective,
                subscription.getCurrentPeriodEnd(), policy);
        BigDecimal delta = remainingNew.subtract(unused);
        return new Preview(policy, effective, currentTotal, targetAmounts.total(), unused, remainingNew, delta.abs(), delta.signum() < 0,
                targetAmounts, subscription.getCurrency());
    }

    /**
     * Applique · bascule la version, recalcule les montants, facture ou credite la difference.
     *
     * <p>Avec la politique NONE, rien n'est rendu ni facture : le nouveau prix vaut a la prochaine
     * echeance, la periode en cours reste ce qu'elle etait.</p>
     */
    @Transactional
    public Preview apply(SubscriptionChangeRequest change) {
        Subscription subscription = change.getSubscription();
        PlanVersion target = change.getTargetPlanVersion();
        Preview preview = preview(subscription, target, change.getEffectiveDate());

        subscription.setPlanVersion(target);
        subscription.setSubtotalAmount(preview.targetAmounts().subtotal());
        subscription.setTaxAmount(preview.targetAmounts().tax());
        subscription.setTotalAmount(preview.targetAmounts().total());

        change.setProrationPolicy(preview.policy());
        change.setProrationAmount(preview.amount());
        change.setProrationCredit(preview.credit());
        if (preview.amount().signum() > 0) {
            if (preview.credit()) {
                WalletResponse wallet = walletService.getOrCreate(subscription.getSubscriberType().name(), subscription.getSubscriberCode(), subscription.getCurrency());
                walletService.credit(walletService.serviceWallet(wallet.walletNumber()), preview.amount(), WalletEntryType.ADJUSTMENT,
                        CREDIT_SOURCE, change.getChangeNumber(), change.getChangeNumber(), "SYSTEM",
                        "PLAN_CHANGE_CREDIT:" + change.getChangeNumber());
            } else {
                change.setProrationBillableNumber(billingSupport.createStandaloneBillableItem(subscription, CHARGE_SOURCE,
                        "Changement de plan au " + preview.effectiveDate() + " · prorata " + preview.policy(),
                        preview.amount(), preview.effectiveDate(), subscription.getCurrentPeriodEnd()));
            }
        }
        eventWriter.writeEvent(subscription, SubscriptionEventType.PLAN_CHANGED,
                "{\"change\":\"" + change.getChangeNumber() + "\",\"policy\":\"" + preview.policy() + "\",\"amount\":" + preview.amount().toPlainString()
                        + ",\"credit\":" + preview.credit() + "}");
        log.info("Abonnement {} · changement {} applique · prorata {} {} {}", subscription.getSubscriptionNumber(), change.getChangeNumber(),
                preview.policy(), preview.amount(), preview.credit() ? "crédité" : "facturé");
        return preview;
    }
}
