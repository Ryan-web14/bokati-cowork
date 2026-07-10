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
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
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

    public String ensureBillableItem(Booking booking) {
        if (StringUtils.hasText(booking.getBillableNumber())) {
            return booking.getBillableNumber();
        }

        BookingPaymentMode mode = booking.getPaymentMode();
        boolean isDirect = mode == null || mode == BookingPaymentMode.DIRECT;

        BigDecimal amount = isDirect ? booking.getTotalAmount() : BigDecimal.ZERO;
        String description = buildDescription(booking);
        String metadataJson = isDirect ? booking.getMetadataJson() : buildPaymentMetadata(booking);

        BillableItem item = billableItemRepository.save(BillableItem.builder()
                .billableNumber(sequenceGenerator.next("billable_item"))
                .sourceType("BOOKING")
                .sourceId(booking.getBookingNumber())
                .subscriberType(booking.getOwnerType())
                .subscriberCode(booking.getOwnerCode())
                .subscriberName(booking.getContactName())
                .subscriberEmail(booking.getContactEmail())
                .subscriberPhone(booking.getContactPhone())
                .description(description)
                .amount(amount)
                .currency(booking.getCurrency())
                .billingPeriodStart(booking.getStartedAt().toLocalDate())
                .billingPeriodEnd(booking.getEndedAt().toLocalDate())
                .status(BillableItemStatus.PENDING)
                .build());

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
                billingEmailService.sendDocumentAsync(issued.documentNumber());
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
            return "{\"paymentMode\":\"" + booking.getPaymentMode().name() + "\"}";
        }
    }
}
