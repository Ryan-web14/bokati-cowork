package com.sni.bokaticowork.features.subscription.subscription.service.support;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.service.support.BillableItemInvoiceSupport;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.BillingScheduleResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillableItemStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingScheduleStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionEventType;
import com.sni.bokaticowork.features.subscription.subscription.mapper.interfaces.SubscriptionBillingMapper;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import com.sni.bokaticowork.features.subscription.subscription.model.BillingSchedule;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.repository.BillingScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SubscriptionBillingSupport {

    private final BillableItemRepository billableItemRepository;
    private final BillingScheduleRepository billingScheduleRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final SubscriptionBillingMapper mapper;
    private final SubscriptionEventWriter eventWriter;
    private final BillableItemInvoiceSupport billableItemInvoiceSupport;

    public void createSubscriptionSetupItems(Subscription subscription, PlanPrice price) {
        BigDecimal recurringRaw  = safe(price.getAmount());
        BigDecimal setupFeeRaw   = safe(price.getSetupFee());
        BigDecimal depositRaw    = safe(price.getDepositAmount());
        BigDecimal taxableBase   = recurringRaw.add(setupFeeRaw);
        BigDecimal totalAmount   = safe(subscription.getTotalAmount());
        BigDecimal chargeableTotal = totalAmount.subtract(depositRaw);

        BigDecimal setupFeeGross;
        BigDecimal recurringGross;
        if (taxableBase.signum() > 0 && setupFeeRaw.signum() > 0) {
            setupFeeGross  = chargeableTotal.multiply(setupFeeRaw).divide(taxableBase, 4, RoundingMode.HALF_UP);
            recurringGross = chargeableTotal.subtract(setupFeeGross);
        } else {
            setupFeeGross  = BigDecimal.ZERO;
            recurringGross = chargeableTotal;
        }

        String planName = subscription.getPlanVersion() != null ? subscription.getPlanVersion().getName() : "";
        List<BillableItem> items = new ArrayList<>();

        if (recurringGross.signum() > 0) {
            items.add(saveBillableItem(subscription, "SUBSCRIPTION_RECURRING",
                    "Abonnement " + planName, recurringGross));
        }
        if (setupFeeGross.signum() > 0) {
            items.add(saveBillableItem(subscription, "SUBSCRIPTION_SETUP_FEE",
                    "Frais d'installation - " + planName, setupFeeGross));
        }
        if (depositRaw.signum() > 0) {
            items.add(saveBillableItem(subscription, "DEPOSIT",
                    "Dépôt de garantie - " + planName, depositRaw));
        }

        if (items.isEmpty()) return;

        String invoiceTitle = "Facture abonnement " + subscription.getSubscriptionNumber();
        billableItemInvoiceSupport.ensureInvoicedGroup(items, invoiceTitle,
                "Souscription " + subscription.getSubscriptionNumber());
        eventWriter.writeEvent(subscription, SubscriptionEventType.BILLING_SCHEDULED, null);
    }

    private BillableItem saveBillableItem(Subscription subscription, String sourceType,
                                          String description, BigDecimal amount) {
        return billableItemRepository.save(BillableItem.builder()
                .billableNumber(sequenceGenerator.next("billable_item"))
                .sourceType(sourceType)
                .sourceId(subscription.getSubscriptionNumber())
                .subscriberType(subscription.getSubscriberType())
                .subscriberCode(subscription.getSubscriberCode())
                .description(description)
                .amount(amount)
                .currency(subscription.getCurrency())
                .billingPeriodStart(subscription.getCurrentPeriodStart())
                .billingPeriodEnd(subscription.getCurrentPeriodEnd())
                .status(BillableItemStatus.PENDING)
                .build());
    }

    private BigDecimal safe(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(4, RoundingMode.HALF_UP);
    }

    public void createBillableItem(Subscription subscription, String sourceType, String description) {
        createBillableItem(subscription, sourceType, description, subscription.getTotalAmount());
    }

    public void createBillableItem(Subscription subscription, String sourceType, String description, java.math.BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return;
        }
        BillableItem item = billableItemRepository.save(BillableItem.builder()
                .billableNumber(sequenceGenerator.next("billable_item"))
                .sourceType(sourceType)
                .sourceId(subscription.getSubscriptionNumber())
                .subscriberType(subscription.getSubscriberType())
                .subscriberCode(subscription.getSubscriberCode())
                .description(description + " - " + subscription.getPlanVersion().getName())
                .amount(amount)
                .currency(subscription.getCurrency())
                .billingPeriodStart(subscription.getCurrentPeriodStart())
                .billingPeriodEnd(subscription.getCurrentPeriodEnd())
                .status(BillableItemStatus.PENDING)
                .build());
        billableItemInvoiceSupport.ensureInvoiced(
                item,
                "Facture abonnement " + subscription.getSubscriptionNumber(),
                description + " - " + subscription.getPlanVersion().getName()
        );
        eventWriter.writeEvent(subscription, SubscriptionEventType.BILLING_SCHEDULED, null);
    }

    public void upsertBillingSchedule(Subscription subscription, BillingScheduleStatus status) {
        BillingSchedule schedule = billingScheduleRepository.findBySubscriptionId(subscription.getId())
                .orElseGet(() -> BillingSchedule.builder()
                        .subscription(subscription)
                        .retryCount(0)
                        .build());
        schedule.setBillingCycle(subscription.getBillingCycle());
        schedule.setCurrentPeriodStart(subscription.getCurrentPeriodStart());
        schedule.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd());
        schedule.setNextBillingDate(subscription.getNextBillingDate());
        schedule.setStatus(status);
        billingScheduleRepository.save(schedule);
    }

    public BillingScheduleResponse getBillingSchedule(Subscription subscription) {
        return billingScheduleRepository.findBySubscriptionId(subscription.getId())
                .map(mapper::toBillingScheduleResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Billing schedule not found"));
    }
}
