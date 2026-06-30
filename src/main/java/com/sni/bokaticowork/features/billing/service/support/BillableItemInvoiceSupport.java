package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.features.billing.dto.request.CreateInvoiceFromBillableItemsRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentFromBillingDocumentRequest;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BillableItemInvoiceSupport {

    private static final String BILLABLE_ITEM_SOURCE = "BILLABLE_ITEM";

    private final BillingDocumentRepository billingDocumentRepository;
    private final BillingDocumentService billingDocumentService;
    @Lazy
    private final PaymentService paymentService;

    public BillingDocumentResponse ensureInvoicedGroup(List<BillableItem> items, String title, String description) {
        if (items == null || items.isEmpty()) return null;
        if (items.size() == 1) return ensureInvoiced(items.get(0), title, description);
        List<String> numbers = items.stream().map(BillableItem::getBillableNumber).toList();
        BillingDocumentResponse created = billingDocumentService.createInvoiceFromBillableItems(
                new CreateInvoiceFromBillableItemsRequest(title, description, LocalDate.now(), LocalDate.now(), numbers)
        );
        BillingDocumentResponse issued = issueIfDraft(created);
        ensurePaymentIntent(issued);
        return issued;
    }

    public BillingDocumentResponse ensureInvoiced(BillableItem item) {
        return ensureInvoiced(item, defaultTitle(item), defaultDescription(item));
    }

    public BillingDocumentResponse ensureInvoiced(BillableItem item, String title, String description) {
        if (item == null || !StringUtils.hasText(item.getBillableNumber())) {
            return null;
        }
        if (item.getInvoiceId() != null) {
            BillingDocumentResponse issued = issueIfDraft(existingInvoice(item));
            ensurePaymentIntent(issued);
            return issued;
        }

        BillingDocumentResponse created = billingDocumentRepository
                .findFirstBySourceAndType(BILLABLE_ITEM_SOURCE, item.getBillableNumber(), BillingDocumentType.INVOICE.name())
                .map(document -> billingDocumentService.get(document.getDocumentNumber()))
                .orElseGet(() -> billingDocumentService.createInvoiceFromBillableItems(
                        new CreateInvoiceFromBillableItemsRequest(
                                safeTitle(title, item),
                                safeDescription(description, item),
                                LocalDate.now(),
                                LocalDate.now(),
                                List.of(item.getBillableNumber())
                        )
                ));

        BillingDocumentResponse issued = issueIfDraft(created);
        ensurePaymentIntent(issued);
        return issued;
    }

    private BillingDocumentResponse existingInvoice(BillableItem item) {
        return billingDocumentRepository.findById(item.getInvoiceId())
                .or(() -> billingDocumentRepository.findFirstBySourceAndType(
                        BILLABLE_ITEM_SOURCE,
                        item.getBillableNumber(),
                        BillingDocumentType.INVOICE.name()
                ))
                .map(document -> billingDocumentService.get(document.getDocumentNumber()))
                .orElse(null);
    }

    private BillingDocumentResponse issueIfDraft(BillingDocumentResponse response) {
        if (response == null) {
            return null;
        }
        if (response.status() == BillingDocumentStatus.DRAFT) {
            return billingDocumentService.issue(response.documentNumber());
        }
        return response;
    }

    private void ensurePaymentIntent(BillingDocumentResponse response) {
        if (response == null || response.balanceDue() == null || response.balanceDue().signum() <= 0) {
            return;
        }
        paymentService.createIntentFromBillingDocument(new CreatePaymentIntentFromBillingDocumentRequest(
                response.documentNumber(),
                null,
                null,
                Instant.now().plusSeconds(900),
                null
        ));
    }

    private String safeTitle(String title, BillableItem item) {
        if (StringUtils.hasText(title)) {
            return title.trim();
        }
        return defaultTitle(item);
    }

    private String safeDescription(String description, BillableItem item) {
        if (StringUtils.hasText(description)) {
            return description.trim();
        }
        return defaultDescription(item);
    }

    private String defaultTitle(BillableItem item) {
        return "Facture " + normalizeSource(item.getSourceType());
    }

    private String defaultDescription(BillableItem item) {
        if (StringUtils.hasText(item.getDescription())) {
            return item.getDescription().trim();
        }
        return "Prestation relative a " + normalizeSource(item.getSourceType());
    }

    private String normalizeSource(String sourceType) {
        if (!StringUtils.hasText(sourceType)) {
            return "operation";
        }
        return sourceType.trim().replace('_', ' ').toLowerCase();
    }
}
