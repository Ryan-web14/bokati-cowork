package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.request.CreateCreditNoteRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.billing.service.support.BillableItemInvoiceSupport;
import com.sni.bokaticowork.features.booking.enums.BookingEventType;
import com.sni.bokaticowork.features.booking.enums.BookingLineType;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingLineRepository;
import com.sni.bokaticowork.features.ressource.service.pricing.PriceContextResolver;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillableItemStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Lot R1, decision D1 : quand l'horaire, la duree ou la ressource d'une reservation change, son prix
 * est recalcule par le moteur unique, avec le contexte du titulaire.
 *
 * <ul>
 *   <li>Rien encore facture : le montant de la reservation (et de son element a facturer en attente)
 *   suit le nouveau prix.</li>
 *   <li>Deja facture, hausse : un element complementaire est facture (facture complementaire).</li>
 *   <li>Deja facture, baisse : un avoir est emis sur la facture d'origine, valide et impute sur
 *   son solde s'il en reste ; a defaut, l'avoir reste en brouillon pour l'equipe.</li>
 * </ul>
 * Chaque recalcul est trace dans l'historique de la reservation (BOOKING_PRICE_ADJUSTED).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingRepricer {

    private static final String BILLABLE_SOURCE_TYPE = "BILLABLE_ITEM";

    private final BookingPricingCalculator calculator;
    private final BookingLineRepository lineRepository;
    private final BillableItemRepository billableItemRepository;
    private final BillingDocumentRepository billingDocumentRepository;
    private final BillableItemInvoiceSupport invoiceSupport;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final BookingEventWriter eventWriter;

    private PriceContextResolver contextResolver;
    private BillingDocumentService billingDocumentService;

    @Autowired(required = false)
    void setContextResolver(PriceContextResolver contextResolver) {
        this.contextResolver = contextResolver;
    }

    @Autowired(required = false)
    void setBillingDocumentService(@Lazy BillingDocumentService billingDocumentService) {
        this.billingDocumentService = billingDocumentService;
    }

    /** Ce que le recalcul a change. */
    public record Outcome(BigDecimal previous, BigDecimal current, BigDecimal difference, String adjustmentDocument) {
    }

    /** Lot B2b : montant actuel et nouveau montant de la ressource pour une autre plage ou ressource, sans rien ecrire. */
    public record Preview(BigDecimal current, BigDecimal next, BigDecimal difference) {
    }

    public Preview preview(Booking booking, com.sni.bokaticowork.features.ressource.model.Resource resource,
                           java.time.LocalDateTime start, java.time.LocalDateTime end) {
        BigDecimal previous = booking.getTotalAmount() == null ? BigDecimal.ZERO : booking.getTotalAmount();
        BigDecimal discounts = lineRepository.findByBookingId(booking.getId()).stream()
                .filter(line -> line.getLineType() == BookingLineType.DISCOUNT && line.getAmount() != null)
                .map(com.sni.bokaticowork.features.booking.model.BookingLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal current = previous.subtract(discounts);
        BookingPricingCalculator.Price price = calculator.calculate(resource, start, end,
                booking.getQuantity() == null ? 1 : booking.getQuantity(),
                Boolean.TRUE.equals(booking.getUnitForced()) ? booking.getBookingUnit() : null,
                contextResolver == null ? null : contextResolver.forOwner(
                        booking.getOwnerType() == null ? null : booking.getOwnerType().name(), booking.getOwnerCode()));
        int duration = (int) java.time.Duration.between(start, end).toMinutes();
        BigDecimal next = booking.getCoveredMinutes() != null
                ? BookingPartialCoverage.uncoveredAmount(price.amount(), duration, Math.min(booking.getCoveredMinutes(), duration))
                : price.amount();
        return new Preview(current, next, next.subtract(current).setScale(4, RoundingMode.HALF_UP));
    }

    public Outcome reprice(Booking booking, String reason) {
        BigDecimal previous = booking.getTotalAmount() == null ? BigDecimal.ZERO : booking.getTotalAmount();
        // Lot B2 (ecart Bn-1) : les remises (lignes DISCOUNT) restent acquises ; seul le prix de la
        // ressource est recalcule, et l'unite imposee par l'equipe est gardee.
        BigDecimal discounts = lineRepository.findByBookingId(booking.getId()).stream()
                .filter(line -> line.getLineType() == BookingLineType.DISCOUNT && line.getAmount() != null)
                .map(com.sni.bokaticowork.features.booking.model.BookingLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal previousResource = previous.subtract(discounts);
        BookingPricingCalculator.Price price = calculator.calculate(booking.getResource(), booking.getStartedAt(), booking.getEndedAt(),
                booking.getQuantity() == null ? 1 : booking.getQuantity(),
                Boolean.TRUE.equals(booking.getUnitForced()) ? booking.getBookingUnit() : null,
                contextResolver == null ? null : contextResolver.forOwner(
                        booking.getOwnerType() == null ? null : booking.getOwnerType().name(), booking.getOwnerCode()));
        booking.setBookingUnit(price.unit());
        booking.setUnitPrice(price.unitPrice());
        BigDecimal resourceAmount = booking.getCoveredMinutes() != null && booking.getDurationMinutes() != null
                ? BookingPartialCoverage.uncoveredAmount(price.amount(), booking.getDurationMinutes(), booking.getCoveredMinutes())
                : price.amount();
        booking.setSubtotalAmount(price.amount());
        booking.setTotalAmount(resourceAmount.add(discounts).max(BigDecimal.ZERO));
        booking.setPriceTrace(BookingPricingCalculator.trace(price));
        lineRepository.findByBookingId(booking.getId()).forEach(line -> {
            if (line.getLineType() == BookingLineType.RESOURCE) {
                line.setQuantity(price.quantity());
                line.setUnit(price.unit());
                line.setUnitPrice(price.unitPrice());
                line.setAmount(resourceAmount);
                lineRepository.save(line);
            } else if (line.getLineType() == BookingLineType.ENTITLEMENT && booking.getEntitlementQuantity() != null) {
                // Le droit retenu a pu changer avec la duree ou la ressource.
                line.setQuantity(booking.getEntitlementQuantity());
                line.setEntitlementCode(booking.getEntitlementCode());
                lineRepository.save(line);
            }
        });

        BigDecimal difference = resourceAmount.subtract(previousResource).setScale(4, RoundingMode.HALF_UP);
        String document = null;
        boolean payable = booking.getPaymentMode() == null || booking.getPaymentMode().isPayable()
                || booking.getCoveredMinutes() != null;
        if (difference.signum() != 0 && payable && StringUtils.hasText(booking.getBillableNumber())) {
            document = adjustBilling(booking, difference, reason);
        }
        if (difference.signum() != 0) {
            eventWriter.write(booking, BookingEventType.BOOKING_PRICE_ADJUSTED, "Prix recalculé",
                    previous.stripTrailingZeros().toPlainString() + " → " + price.amount().stripTrailingZeros().toPlainString() + " "
                            + booking.getCurrency() + " (" + reason + ")" + (document == null ? "" : ", pièce " + document), null);
        }
        return new Outcome(previous, price.amount(), difference, document);
    }

    private String adjustBilling(Booking booking, BigDecimal difference, String reason) {
        BillableItem item = billableItemRepository.findByBillableNumber(booking.getBillableNumber()).orElse(null);
        if (item == null) {
            return null;
        }
        if (item.getStatus() == BillableItemStatus.PENDING) {
            // Pas encore facture : l'element suit le nouveau prix.
            item.setAmount(booking.getTotalAmount());
            billableItemRepository.save(item);
            return null;
        }
        String label = "Réservation " + booking.getBookingNumber() + " modifiée (" + reason + ")";
        if (difference.signum() > 0) {
            BillableItem complement = billableItemRepository.save(BillableItem.builder()
                    .billableNumber(sequenceGenerator.next("billable_item"))
                    .sourceType("BOOKING")
                    .sourceId(booking.getBookingNumber())
                    .subscriberType(booking.getOwnerType())
                    .subscriberCode(booking.getOwnerCode())
                    .subscriberName(booking.getContactName())
                    .subscriberEmail(booking.getContactEmail())
                    .subscriberPhone(booking.getContactPhone())
                    .billingCompanyCode(booking.getBillingCompanyCode())
                    .description("Complément de prix · " + label)
                    .amount(difference)
                    .currency(booking.getCurrency())
                    .billingPeriodStart(booking.getStartedAt().toLocalDate())
                    .billingPeriodEnd(booking.getEndedAt().toLocalDate())
                    .status(BillableItemStatus.PENDING)
                    .build());
            BillingDocumentResponse invoice = invoiceSupport.ensureInvoiced(complement, "Complément · " + booking.getBookingNumber(), label);
            return invoice == null ? complement.getBillableNumber() : invoice.documentNumber();
        }
        BillingDocument invoice = billingDocumentRepository.findFirstBySourceAndType(BILLABLE_SOURCE_TYPE, booking.getBillableNumber(), "INVOICE")
                .orElse(null);
        if (invoice == null || billingDocumentService == null) {
            log.warn("Reservation {} : baisse de {} sans facture retrouvee, avoir a emettre par l'equipe", booking.getBookingNumber(), difference);
            return null;
        }
        BigDecimal amount = difference.abs();
        boolean unpaid = invoice.getBalanceDue() != null && invoice.getBalanceDue().signum() > 0;
        try {
            return billingDocumentService.createCreditNote(invoice.getDocumentNumber(),
                    new CreateCreditNoteRequest(amount, label, unpaid, true, null)).documentNumber();
        } catch (RuntimeException ex) {
            log.warn("Avoir de la reservation {} non valide d'office ({}) : laisse en brouillon", booking.getBookingNumber(), ex.getMessage());
            return billingDocumentService.createCreditNote(invoice.getDocumentNumber(),
                    new CreateCreditNoteRequest(amount, label, false, false, null)).documentNumber();
        }
    }
}
