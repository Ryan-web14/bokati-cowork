package com.sni.bokaticowork.features.booking.service.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingEmailService;
import com.sni.bokaticowork.features.billing.service.support.BillableItemInvoiceSupport;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingLineRepository;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillableItemStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingBillableBridge {

    private final BillableItemRepository billableItemRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final BookingLineRepository lineRepository;
    private final BillableItemInvoiceSupport billableItemInvoiceSupport;
    private final BillingEmailService billingEmailService;
    private final ObjectMapper objectMapper;
    private final com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository billingDocumentRepository;

    /**
     * Pendant la creation d'un lot multi-ressources, les elements facturables sont mis de cote au
     * lieu d'etre factures un par un · le lot les facture ensuite par membre ({@link #invoiceGroup}).
     */
    private static final ThreadLocal<List<BillableItem>> DEFERRED = new ThreadLocal<>();

    /** Met la facturation en attente pour le fil courant · a refermer par {@link #endDeferredInvoicing()}. */
    public void beginDeferredInvoicing() {
        DEFERRED.set(new ArrayList<>());
    }

    /** Rend les elements mis de cote depuis {@link #beginDeferredInvoicing()} et retablit la facturation immediate. */
    public List<BillableItem> endDeferredInvoicing() {
        List<BillableItem> items = DEFERRED.get();
        DEFERRED.remove();
        return items == null ? List.of() : items;
    }

    /**
     * Une seule facture pour plusieurs elements d'un meme titulaire, envoyee au titulaire apres le
     * commit comme la facture d'une reservation seule.
     */
    public BillingDocumentResponse invoiceGroup(List<BillableItem> items, String title, String description) {
        BillingDocumentResponse issued = billableItemInvoiceSupport.ensureInvoicedGroup(items, title, description);
        String email = items.isEmpty() ? null : items.get(0).getSubscriberEmail();
        if (issued != null && StringUtils.hasText(email)) {
            try {
                dispatchInvoiceEmail(issued.documentNumber());
            } catch (Exception ex) {
                log.warn("Failed to send grouped booking invoice {} · billing already completed", issued.documentNumber(), ex);
            }
        }
        return issued;
    }

    /**
     * La facture de la reservation est-elle deja soldee · le cas d'une reservation de lot approuvee
     * apres que le membre a regle la facture commune.
     */
    public boolean invoiceSettled(Booking booking) {
        if (!StringUtils.hasText(booking.getBillableNumber())) {
            return false;
        }
        return billableItemRepository.findByBillableNumber(booking.getBillableNumber())
                .map(BillableItem::getInvoiceId)
                .flatMap(billingDocumentRepository::findById)
                .map(invoice -> invoice.getStatus() == com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus.PAID
                        || (invoice.getBalanceDue() != null && invoice.getBalanceDue().signum() <= 0
                        && invoice.getPaidAmount() != null && invoice.getPaidAmount().signum() > 0))
                .orElse(false);
    }

    public String ensureBillableItem(Booking booking) {
        if (StringUtils.hasText(booking.getBillableNumber())) {
            return booking.getBillableNumber();
        }

        BookingPaymentMode mode = booking.getPaymentMode();
        boolean payable = mode == null || mode.isPayable();

        BigDecimal amount = payable ? booking.getTotalAmount() : BigDecimal.ZERO;
        String description = buildDescription(booking);
        String metadataJson = payable ? booking.getMetadataJson() : buildPaymentMetadata(booking);

        BillableItem item = billableItemRepository.save(BillableItem.builder()
                .billableNumber(sequenceGenerator.next("billable_item"))
                .sourceType("BOOKING")
                .sourceId(booking.getBookingNumber())
                .subscriberType(booking.getOwnerType())
                .subscriberCode(booking.getOwnerCode())
                .subscriberName(booking.getContactName())
                .subscriberEmail(booking.getContactEmail())
                .subscriberPhone(booking.getContactPhone())
                // Le choix « pour l'organisation » fait a la reservation suit jusqu'a la facture.
                .billingCompanyCode(booking.getBillingCompanyCode())
                .description(description)
                .amount(amount)
                .currency(booking.getCurrency())
                .billingPeriodStart(booking.getStartedAt().toLocalDate())
                .billingPeriodEnd(booking.getEndedAt().toLocalDate())
                .status(BillableItemStatus.PENDING)
                .build());

        List<BillableItem> deferred = DEFERRED.get();
        if (deferred != null) {
            deferred.add(item);
            lineRepository.findByBookingId(booking.getId()).forEach(line -> {
                if (line.getBillableNumber() == null) {
                    line.setBillableNumber(item.getBillableNumber());
                    lineRepository.save(line);
                }
            });
            return item.getBillableNumber();
        }

        BillingDocumentResponse issued = billableItemInvoiceSupport.ensureInvoiced(item, buildInvoiceTitle(booking), description);

        lineRepository.findByBookingId(booking.getId()).forEach(line -> {
            if (line.getBillableNumber() == null) {
                line.setBillableNumber(item.getBillableNumber());
                lineRepository.save(line);
            }
        });

        // Notification after all business logic
        if (issued != null && StringUtils.hasText(booking.getContactEmail())) {
            try {
                dispatchInvoiceEmail(issued.documentNumber());
            } catch (Exception ex) {
                log.warn("Failed to send billing document email for booking {} · billing already completed",
                        booking.getBookingNumber(), ex);
            }
        }
        return item.getBillableNumber();
    }

    private String buildDescription(Booking booking) {
        String resourceName = booking.getResource().getName();
        String bookingNumber = booking.getBookingNumber();

        if (booking.getPaymentMode() == BookingPaymentMode.SUBSCRIPTION) {
            StringBuilder desc = new StringBuilder("Abonné - ").append(resourceName)
                    .append(" | Réservation ").append(bookingNumber);
            if (StringUtils.hasText(booking.getSubscriptionNumber())) {
                desc.append(" | Abonnement: ").append(booking.getSubscriptionNumber());
            }
            appendReferenceAmount(desc, booking);
            desc.append(" | Membre abonné - couvert par abonnement");
            return desc.toString();
        }

        if (booking.getPaymentMode() == BookingPaymentMode.PASS) {
            StringBuilder desc = new StringBuilder("Pass - ").append(resourceName)
                    .append(" | Réservation ").append(bookingNumber);
            if (StringUtils.hasText(booking.getPassNumber())) {
                desc.append(" | Pass: ").append(booking.getPassNumber());
            }
            appendReferenceAmount(desc, booking);
            return desc.toString();
        }

        return "Booking " + bookingNumber + " - " + resourceName;
    }

    private void appendReferenceAmount(StringBuilder desc, Booking booking) {
        if (booking.getTotalAmount() != null && booking.getTotalAmount().signum() > 0) {
            desc.append(" | Tarif habituel: ")
                    .append(booking.getTotalAmount().stripTrailingZeros().toPlainString())
                    .append(" ").append(booking.getCurrency());
        }
    }

    /**
     * Poste l'envoi de la facture apres le commit.
     *
     * <p>L'appel partait directement sur un fil {@code @Async}, depuis l'interieur de la transaction
     * qui venait tout juste de creer la facture. Le fil la cherchait donc avant le commit et ne la
     * trouvait pas : l'envoi echouait sur « Billing document not found », le client ne recevait
     * jamais sa facture, et il n'en restait qu'un avertissement dans les journaux.
     */
    private void dispatchInvoiceEmail(String documentNumber) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            billingEmailService.sendDocumentAsync(documentNumber);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                billingEmailService.sendDocumentAsync(documentNumber);
            }
        });
    }

    private String buildInvoiceTitle(Booking booking) {
        if (booking.getPaymentMode() == BookingPaymentMode.SUBSCRIPTION) {
            return "Abonné - Réservation " + booking.getBookingNumber();
        }
        if (booking.getPaymentMode() == BookingPaymentMode.PASS) {
            return "Pass - Réservation " + booking.getBookingNumber();
        }
        return "Facture reservation " + booking.getBookingNumber();
    }

    private String buildPaymentMetadata(Booking booking) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("paymentMode", booking.getPaymentMode().name());
        if (booking.getPaymentMode() == BookingPaymentMode.SUBSCRIPTION
                && StringUtils.hasText(booking.getSubscriptionNumber())) {
            metadata.put("subscriptionNumber", booking.getSubscriptionNumber());
        }
        if (booking.getPaymentMode() == BookingPaymentMode.PASS
                && StringUtils.hasText(booking.getPassNumber())) {
            metadata.put("passNumber", booking.getPassNumber());
        }
        if (booking.getTotalAmount() != null) {
            metadata.put("referenceAmount", booking.getTotalAmount());
        }
        if (StringUtils.hasText(booking.getCurrency())) {
            metadata.put("currency", booking.getCurrency());
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return com.sni.bokaticowork.core.utils.json.JsonPayload.of("paymentMode", booking.getPaymentMode().name());
        }
    }
}
