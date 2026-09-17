package com.sni.bokaticowork.core.outbox.service.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.outbox.model.OutboxEvent;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxEventProcessor;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * Une facture annulee annule la reservation qu'elle facturait.
 *
 * <p>La propagation n'existait que dans un sens. Annuler une reservation reglait son sort a la
 * facture, mais annuler la facture laissait la reservation vivante : le creneau restait occupe, le
 * client gardait une reservation confirmee, et plus rien ne la facturait. Le desaccord ne se voyait
 * nulle part avant que quelqu'un ne se presente.</p>
 *
 * <p>Le retour passe par la boite d'envoi plutot que par un appel direct, pour que la facturation
 * n'ait pas a connaitre le module de reservation · elle publie ce qu'elle a fait, et qui doit en
 * tenir compte s'y abonne.</p>
 *
 * <p>Les deux sens se rejoignent sans boucler : une reservation deja annulee sort au premier test
 * de {@code cancelInternal}, et une facture deja annulee n'est pas republiee par
 * {@code cancelAndArchive}.</p>
 */
@Slf4j
@Component
public class BillingDocumentCancellationOutboxEventProcessor implements OutboxEventProcessor {

    /**
     * Les deux facons d'annuler une facture. La seconde est celle d'une facture deja reglee, que
     * l'on ne raye pas mais que l'on corrige par un avoir · la reservation n'existe pas davantage
     * pour autant.
     */
    private static final Set<String> EVENT_TYPES = Set.of(
            "BILLING_DOCUMENT_CANCELLED", "BILLING_DOCUMENT_CANCELLED_VIA_CREDIT_NOTE");

    private static final String BILLABLE_SOURCE_TYPE = "BILLABLE_ITEM";

    private final ObjectMapper objectMapper;
    private final BillingDocumentRepository billingDocumentRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    public BillingDocumentCancellationOutboxEventProcessor(ObjectMapper objectMapper,
                                                           BillingDocumentRepository billingDocumentRepository,
                                                           BookingRepository bookingRepository,
                                                           @Lazy BookingService bookingService) {
        this.objectMapper = objectMapper;
        this.billingDocumentRepository = billingDocumentRepository;
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return EVENT_TYPES.contains(event.getEventType());
    }

    @Override
    public void process(OutboxEvent event) {
        String documentNumber = documentNumber(event);
        if (!StringUtils.hasText(documentNumber)) {
            throw new IllegalStateException(
                    event.getEventType() + " payload missing documentNumber: " + event.getPayload());
        }

        Booking booking = linkedBooking(documentNumber);
        if (booking == null) {
            log.debug("Cancelled document {} facturait autre chose qu'une reservation · rien a propager", documentNumber);
            return;
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return;
        }

        bookingService.systemCancel(booking.getBookingNumber(),
                "Annulation de la facture " + documentNumber);
        log.info("Booking {} annulee a la suite de l'annulation de sa facture {}",
                booking.getBookingNumber(), documentNumber);
    }

    /**
     * Remonte de la facture a la reservation, par l'element facturable qui les relie. Une facture
     * issue d'autre chose, un abonnement ou une vente au comptoir, ne donne rien et n'est pas une
     * anomalie.
     */
    private Booking linkedBooking(String documentNumber) {
        BillingDocument document = billingDocumentRepository.findByDocumentNumber(documentNumber).orElse(null);
        if (document == null || !BILLABLE_SOURCE_TYPE.equalsIgnoreCase(document.getSourceType())
                || !StringUtils.hasText(document.getSourceCode())) {
            return null;
        }
        return bookingRepository.findByBillableNumber(document.getSourceCode()).orElse(null);
    }

    private String documentNumber(OutboxEvent event) {
        try {
            JsonNode payload = objectMapper.readTree(event.getPayload());
            String fromPayload = payload.path("documentNumber").asText(null);
            return StringUtils.hasText(fromPayload) ? fromPayload : event.getAggregateId();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to parse " + event.getEventType() + " payload", ex);
        }
    }
}
