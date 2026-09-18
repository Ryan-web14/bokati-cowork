package com.sni.bokaticowork.features.subscription.subscription.service.support;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
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
        createSubscriptionSetupItems(subscription, safe(price.getAmount()),
                safe(price.getSetupFee()), safe(price.getDepositAmount()));
    }

    /**
     * Ventile en montants effectifs plutot qu'en tarif catalogue.
     *
     * <p>La repartition entre recurrent et frais d'entree se fait au prorata du total facturable.
     * Une remise generale se reporte donc d'elle-meme sur les deux lignes. Une dispense de frais
     * d'entree, elle, ne doit toucher que la seconde · d'ou le besoin de recevoir les montants deja
     * reduits, et non le tarif du catalogue.</p>
     */
    public void createSubscriptionSetupItems(Subscription subscription,
                                             BigDecimal recurringRaw,
                                             BigDecimal setupFeeRaw,
                                             BigDecimal depositRaw) {
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

    public BillingDocumentResponse createBillableItem(Subscription subscription, String sourceType, String description) {
        return createBillableItem(subscription, sourceType, description, subscription.getTotalAmount());
    }

    /** Un element facturable sur la periode courante, facture aussitot · rend la facture, nulle si rien a facturer. */
    public BillingDocumentResponse createBillableItem(Subscription subscription, String sourceType, String description, java.math.BigDecimal amount) {
        return createBillableItem(subscription, sourceType, description, amount,
                subscription.getCurrentPeriodStart(), subscription.getCurrentPeriodEnd());
    }

    public BillingDocumentResponse createBillableItem(Subscription subscription, String sourceType, String description, java.math.BigDecimal amount,
                                                      java.time.LocalDate periodStart, java.time.LocalDate periodEnd) {
        if (amount == null || amount.signum() <= 0) {
            return null;
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
                .billingPeriodStart(periodStart)
                .billingPeriodEnd(periodEnd)
                .status(BillableItemStatus.PENDING)
                .build());
        BillingDocumentResponse invoice = billableItemInvoiceSupport.ensureInvoiced(
                item,
                "Facture abonnement " + subscription.getSubscriptionNumber(),
                description + " - " + subscription.getPlanVersion().getName()
        );
        eventWriter.writeEvent(subscription, SubscriptionEventType.BILLING_SCHEDULED, null);
        return invoice;
    }

    /** Un element facturable hors periode courante (frais de rupture, jours de raccordement) · rend son numero. */
    public String createStandaloneBillableItem(Subscription subscription, String sourceType, String description, java.math.BigDecimal amount,
                                               java.time.LocalDate periodStart, java.time.LocalDate periodEnd) {
        if (amount == null || amount.signum() <= 0) {
            return null;
        }
        BillableItem item = billableItemRepository.save(BillableItem.builder()
                .billableNumber(sequenceGenerator.next("billable_item"))
                .sourceType(sourceType)
                .sourceId(subscription.getSubscriptionNumber())
                .subscriberType(subscription.getSubscriberType())
                .subscriberCode(subscription.getSubscriberCode())
                .description(description)
                .amount(amount.setScale(4, RoundingMode.HALF_UP))
                .currency(subscription.getCurrency())
                .billingPeriodStart(periodStart)
                .billingPeriodEnd(periodEnd)
                .status(BillableItemStatus.PENDING)
                .build());
        billableItemInvoiceSupport.ensureInvoiced(item, "Facture abonnement " + subscription.getSubscriptionNumber(), description);
        eventWriter.writeEvent(subscription, SubscriptionEventType.BILLING_SCHEDULED, null);
        return item.getBillableNumber();
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
