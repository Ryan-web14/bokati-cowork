package com.sni.bokaticowork.features.subscription.subscription.service.support.pass;

import com.sni.bokaticowork.features.subscription.subscription.enums.BillingScheduleStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassEventType;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.PassRenewalSchedule;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.repository.PassRenewalScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PassRenewalOperator {

    private final PassRepository passRepository;
    private final PassRenewalScheduleRepository scheduleRepository;
    private final PassBillingSupport billingSupport;
    private final PassEventWriter eventWriter;
    private final PassPeriodCalculator periodCalculator;
    private final PassPlanResolver planResolver;
    private final com.sni.bokaticowork.features.subscription.subscription.service.support.PlanPriceAmountCalculator priceCalculator;

    public int renewDuePasses() {
        List<PassRenewalSchedule> due = scheduleRepository
                .findAllByStatusAndNextRenewalDateBefore(BillingScheduleStatus.ACTIVE, Instant.now());
        int count = 0;
        for (PassRenewalSchedule schedule : due) {
            try {
                renew(schedule);
                count++;
            } catch (Exception ex) {
                log.error("Failed to renew pass {} : {}", schedule.getPass().getPassNumber(), ex.getMessage());
            }
        }
        return count;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void renew(PassRenewalSchedule schedule) {
        Pass pass = schedule.getPass();
        if (pass.getStatus() != PassStatus.ACTIVE) {
            log.info("Skipping renewal for pass {} in status {}", pass.getPassNumber(), pass.getStatus());
            return;
        }

        Instant newFrom = pass.getValidUntil();
        Instant newUntil = periodCalculator.periodEnd(newFrom, schedule.getDuration(), schedule.getDurationUnit());

        pass.setValidFrom(newFrom);
        pass.setValidUntil(newUntil);
        pass.setNextRenewalDate(newUntil);
        pass.setRenewalCount(pass.getRenewalCount() + 1);
        pass.setUsedCount(0);
        passRepository.save(pass);

        schedule.setCurrentPeriodStart(newFrom);
        schedule.setCurrentPeriodEnd(newUntil);
        schedule.setNextRenewalDate(newUntil);
        schedule.setLastAttemptAt(Instant.now());
        schedule.setRetryCount(0);
        scheduleRepository.save(schedule);

        billingSupport.createAndInvoice(pass, "PASS_RENEWAL", renewalAmount(pass));

        eventWriter.writeEvent(pass, PassEventType.PASS_RENEWED, null);
        eventWriter.writeEvent(pass, PassEventType.BILLING_SCHEDULED, null);
        log.info("Renewed pass {} · new period {} to {}", pass.getPassNumber(), newFrom, newUntil);
    }

    /**
     * Prix d'une periode renouvelee, taxe comprise · sans les frais d'entree ni la caution.
     *
     * <p>Le total du pass les contient, il est celui de l'emission : le refacturer a chaque
     * renouvellement faisait repayer des frais uniques et une caution deja detenue. Sans prix de
     * plan lisible, ou sans frais ni caution, le total reste ce qu'il etait.</p>
     */
    java.math.BigDecimal renewalAmount(Pass pass) {
        if (pass.getPassVersion() == null) {
            return pass.getTotalAmount();
        }
        com.sni.bokaticowork.features.subscription.subscription.model.PassPlanPrice price;
        try {
            price = planResolver.resolvePrice(pass.getPassVersion(), pass.getCurrency());
        } catch (com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException ex) {
            return pass.getTotalAmount();
        }
        boolean oneOffCharges = (price.getSetupFee() != null && price.getSetupFee().signum() > 0)
                || (price.getDepositAmount() != null && price.getDepositAmount().signum() > 0);
        if (!oneOffCharges) {
            return pass.getTotalAmount();
        }
        return priceCalculator.compute(price.getAmount(), java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                price.getTaxIncluded()).total();
    }

    public void handleRenewalPaymentSucceeded(Pass pass, String transactionNumber) {
        eventWriter.writeEvent(pass, PassEventType.ENTITLEMENTS_GRANTED, null);
        log.info("Renewal payment received for pass {} · transaction {}", pass.getPassNumber(), transactionNumber);
    }
}
