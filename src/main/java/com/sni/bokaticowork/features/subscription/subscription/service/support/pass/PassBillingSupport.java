package com.sni.bokaticowork.features.subscription.subscription.service.support.pass;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.service.support.BillableItemInvoiceSupport;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingScheduleStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillableItemStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.PassRenewalSchedule;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.repository.PassRenewalScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class PassBillingSupport {

    private final BillableItemRepository billableItemRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final BillableItemInvoiceSupport billableItemInvoiceSupport;
    private final PassRenewalScheduleRepository renewalScheduleRepository;

    public BillingDocumentResponse createAndInvoice(Pass pass) {
        return createAndInvoice(pass, "PASS_SETUP");
    }

    public BillingDocumentResponse createAndInvoice(Pass pass, String sourceType) {
        if (pass.getTotalAmount() == null || pass.getTotalAmount().signum() <= 0) return null;
        PassPlanVersion version = pass.getPassVersion();
        String description = "Émission du pass " + pass.getPassNumber()
                + " — " + pass.getName()
                + (version != null ? " (v" + version.getVersionNumber() + ")" : "");

        BillableItem item = billableItemRepository.save(BillableItem.builder()
                .billableNumber(sequenceGenerator.next("billable_item"))
                .sourceType(sourceType)
                .sourceId(pass.getPassNumber())
                .subscriberType(pass.getOwnerType())
                .subscriberCode(pass.getOwnerCode())
                .description(description)
                .amount(pass.getTotalAmount())
                .currency(pass.getCurrency())
                .billingPeriodStart(toLocalDate(pass.getValidFrom()))
                .billingPeriodEnd(toLocalDate(pass.getValidUntil()))
                .status(BillableItemStatus.PENDING)
                .build());

        return billableItemInvoiceSupport.ensureInvoiced(
                item,
                "Facture pass " + pass.getPassNumber(),
                description
        );
    }

    public void upsertRenewalSchedule(Pass pass) {
        PassPlanVersion version = pass.getPassVersion();
        if (version == null || !Boolean.TRUE.equals(pass.getAutoRenew())) return;

        PassRenewalSchedule schedule = renewalScheduleRepository.findByPassId(pass.getId())
                .orElseGet(() -> PassRenewalSchedule.builder().pass(pass).retryCount(0).build());
        schedule.setDuration(version.getDuration());
        schedule.setDurationUnit(version.getDurationUnit());
        schedule.setNextRenewalDate(pass.getValidUntil());
        schedule.setCurrentPeriodStart(pass.getValidFrom());
        schedule.setCurrentPeriodEnd(pass.getValidUntil());
        schedule.setStatus(BillingScheduleStatus.ACTIVE);
        pass.setNextRenewalDate(pass.getValidUntil());
        renewalScheduleRepository.save(schedule);
    }

    private LocalDate toLocalDate(Instant instant) {
        return instant == null ? null : LocalDate.ofInstant(instant, ZoneOffset.UTC);
    }
}
