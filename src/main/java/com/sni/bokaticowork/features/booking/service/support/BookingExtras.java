package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.request.CreateCreditNoteRequest;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.ServiceCatalogItem;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.billing.service.support.BillableItemInvoiceSupport;
import com.sni.bokaticowork.features.booking.enums.BookingEventType;
import com.sni.bokaticowork.features.booking.enums.BookingLineType;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.model.BookingLine;
import com.sni.bokaticowork.features.booking.repository.BookingLineRepository;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceAmenityLink;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAmenityLinkRepository;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourcePricingRuleService;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingContext;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingEngine;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingResult;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.DiscountDocumentType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.TargetScope;
import com.sni.bokaticowork.features.subscription.promotion.pricing.service.DiscountApplicationService;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillableItemStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Lot R5c : ce qui se vend autour d'une reservation.
 *
 * <ul>
 *   <li>services additionnels du catalogue (cafe, traiteur, impression, materiel) ;</li>
 *   <li>equipements optionnels de la ressource, a leur supplement ;</li>
 *   <li>participants supplementaires, factures ou offerts au choix de l'equipe ;</li>
 *   <li>caution de la ressource (non taxee, rendue ou retenue par le registre des cautions) ;</li>
 *   <li>depassement mesure au depart et non-presentation, selon la politique ;</li>
 *   <li>code promo (moteur des promotions).</li>
 * </ul>
 *
 * <p>Avant la facture de la reservation, chaque supplement attend et part sur la meme facture ;
 * apres, il part sur une facture a lui (et une remise, sur un avoir).</p>
 */
@Slf4j
@Component
public class BookingExtras {

    public static final String ADDON_SOURCE = "BOOKING_ADDON";
    public static final String DEPOSIT_SOURCE = "DEPOSIT";
    static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");

    /** Une ligne de la reservation, telle que l'equipe la voit. */
    public record LineView(String type, String code, String description, BigDecimal quantity, BigDecimal unitPrice,
                           BigDecimal amount, String currency, String billableNumber) {
    }

    private final BookingRepository bookingRepository;
    private final BookingLineRepository lineRepository;
    private final BillableItemRepository billableItemRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final BillableItemInvoiceSupport invoiceSupport;
    private final BookingEventWriter eventWriter;
    private ServiceCatalogItemRepository catalog;
    private ResourceAmenityLinkRepository amenityLinks;
    private ResourcePricingRuleService pricing;
    private PricingEngine promotions;
    private DiscountApplicationService discounts;
    private BillingDocumentRepository documents;
    private BillingDocumentService billingDocumentService;

    public BookingExtras(BookingRepository bookingRepository, BookingLineRepository lineRepository,
                         BillableItemRepository billableItemRepository, SequenceGeneratorFacade sequenceGenerator,
                         BillableItemInvoiceSupport invoiceSupport, BookingEventWriter eventWriter) {
        this.bookingRepository = bookingRepository;
        this.lineRepository = lineRepository;
        this.billableItemRepository = billableItemRepository;
        this.sequenceGenerator = sequenceGenerator;
        this.invoiceSupport = invoiceSupport;
        this.eventWriter = eventWriter;
    }

    @Autowired(required = false)
    void setOptional(ServiceCatalogItemRepository catalog, ResourceAmenityLinkRepository amenityLinks,
                     @Lazy ResourcePricingRuleService pricing, PricingEngine promotions, DiscountApplicationService discounts,
                     BillingDocumentRepository documents, @Lazy BillingDocumentService billingDocumentService) {
        this.catalog = catalog;
        this.amenityLinks = amenityLinks;
        this.pricing = pricing;
        this.promotions = promotions;
        this.discounts = discounts;
        this.documents = documents;
        this.billingDocumentService = billingDocumentService;
    }

    // ------------------------------------------------------------------ consultation

    @Transactional(readOnly = true)
    public List<LineView> lines(String bookingNumber) {
        Booking booking = booking(bookingNumber);
        return lineRepository.findByBookingId(booking.getId()).stream()
                .map(line -> new LineView(line.getLineType().name(), line.getItemCode(), line.getDescription(), line.getQuantity(),
                        line.getUnitPrice(), line.getAmount(), line.getCurrency(), line.getBillableNumber()))
                .toList();
    }

    // ------------------------------------------------------------------ ajouts de l'equipe

    /** Service additionnel du catalogue, au prix du catalogue. */
    @Transactional
    public LineView addService(String bookingNumber, String itemCode, Integer quantity) {
        Booking booking = editable(bookingNumber);
        if (catalog == null || !StringUtils.hasText(itemCode)) {
            throw new BadRequestException("Le code du service du catalogue est requis");
        }
        ServiceCatalogItem item = catalog.findByItemCode(itemCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Service introuvable dans le catalogue : " + itemCode.trim()));
        if (!Boolean.TRUE.equals(item.getActive())) {
            throw new ConflictException("Le service " + item.getItemCode() + " est désactivé");
        }
        int qty = quantity == null || quantity < 1 ? 1 : quantity;
        return view(charge(booking, BookingLineType.SERVICE, item.getItemCode(), item.getName(), qty,
                item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice(), ADDON_SOURCE));
    }

    /** Equipement optionnel de la ressource, a son supplement (equipement rattache comme optionnel). */
    @Transactional
    public LineView addAmenity(String bookingNumber, String amenityCode, Integer quantity) {
        Booking booking = editable(bookingNumber);
        if (amenityLinks == null || !StringUtils.hasText(amenityCode)) {
            throw new BadRequestException("Le code de l'équipement est requis");
        }
        ResourceAmenityLink link = amenityLinks.findAllByResource(booking.getResource()).stream()
                .filter(candidate -> candidate.getAmenity() != null && amenityCode.trim().equalsIgnoreCase(candidate.getAmenity().getCode()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Équipement non rattaché à la ressource : " + amenityCode.trim()));
        if (!Boolean.TRUE.equals(link.getOptional())) {
            throw new ConflictException("L'équipement " + link.getAmenity().getCode() + " est compris dans la ressource, il ne se vend pas en option");
        }
        int qty = quantity == null || quantity < 1 ? 1 : quantity;
        BigDecimal price = link.getExtraPrice() == null ? BigDecimal.ZERO : BigDecimal.valueOf(link.getExtraPrice());
        return view(charge(booking, BookingLineType.AMENITY, link.getAmenity().getCode(),
                "Option : " + link.getAmenity().getName(), qty, price, ADDON_SOURCE));
    }

    /**
     * Participants supplementaires : au prix par participant de la ressource, ou offerts si
     * l'equipe le decide pour cette reservation ({@code waive}). La ligne reste pour la trace.
     */
    @Transactional
    public LineView addParticipants(String bookingNumber, Integer count, boolean waive) {
        Booking booking = editable(bookingNumber);
        if (count == null || count < 1) {
            throw new BadRequestException("Le nombre de participants supplémentaires est requis");
        }
        Resource resource = booking.getResource();
        Integer included = resource.getIncludedParticipants();
        if (included != null && resource.getCapacity() != null && included + count > resource.getCapacity()) {
            throw new ConflictException("Capacité de " + resource.getCode() + " dépassée : " + resource.getCapacity() + " personnes au plus");
        }
        BigDecimal price = waive || resource.getExtraParticipantPrice() == null
                ? BigDecimal.ZERO : BigDecimal.valueOf(resource.getExtraParticipantPrice());
        return view(charge(booking, BookingLineType.PARTICIPANT, null,
                "Participant(s) supplémentaire(s)" + (waive ? " offert(s)" : ""), count, price, ADDON_SOURCE));
    }

    // ------------------------------------------------------------------ automatiques

    /**
     * Caution de la ressource, ajoutee juste avant la premiere facture de la reservation (une fois).
     * Le registre des cautions la prend a l'emission de la facture ; l'equipe la rend ou la retient.
     */
    public void addDepositIfAny(Booking booking) {
        Resource resource = booking.getResource();
        if (resource == null || resource.getDepositAmount() == null || resource.getDepositAmount() <= 0) {
            return;
        }
        boolean present = lineRepository.findByBookingId(booking.getId()).stream()
                .anyMatch(line -> line.getLineType() == BookingLineType.DEPOSIT);
        if (!present) {
            lineRepository.save(line(booking, BookingLineType.DEPOSIT, null, "Caution · " + resource.getName(), 1,
                    BigDecimal.valueOf(resource.getDepositAmount())));
        }
    }

    /**
     * Elements a facturer avec la reservation : chaque supplement en attente (sans element). Les
     * lignes recoivent leur numero d'element.
     */
    public List<BillableItem> pendingItems(Booking booking) {
        List<BillableItem> items = new ArrayList<>();
        for (BookingLine line : lineRepository.findByBookingId(booking.getId())) {
            if (!isExtra(line.getLineType()) || line.getBillableNumber() != null
                    || line.getAmount() == null || line.getAmount().signum() <= 0) {
                continue;
            }
            BillableItem item = item(booking, line);
            line.setBillableNumber(item.getBillableNumber());
            lineRepository.save(line);
            items.add(item);
        }
        return items;
    }

    public static boolean isExtra(BookingLineType type) {
        return type == BookingLineType.SERVICE || type == BookingLineType.AMENITY || type == BookingLineType.PARTICIPANT
                || type == BookingLineType.DEPOSIT || type == BookingLineType.OVERRUN || type == BookingLineType.NO_SHOW;
    }

    /** Depassement mesure au depart, au-dela de la tolerance de la politique, au prix du moteur. */
    public void onCheckOut(Booking booking) {
        ResourcePolicy policy = booking.getResource() == null ? null : booking.getResource().getEffectivePolicy();
        if (policy == null || policy.getOverrunGraceMinutes() == null || booking.getCheckedOutAt() == null) {
            return;
        }
        LocalDateTime left = LocalDateTime.ofInstant(booking.getCheckedOutAt(), ZONE);
        long over = Duration.between(booking.getEndedAt(), left).toMinutes();
        if (over <= policy.getOverrunGraceMinutes()) {
            return;
        }
        int slot = booking.getResource().getSlotDurationMinutes() == null ? 30 : Math.max(1, booking.getResource().getSlotDurationMinutes());
        long billed = ((over + slot - 1) / slot) * slot;
        BigDecimal amount = overrunPrice(booking, billed);
        if (amount.signum() <= 0) {
            return;
        }
        charge(booking, BookingLineType.OVERRUN, null, "Dépassement de " + over + " min (facturé " + billed + " min)", 1, amount, ADDON_SOURCE);
    }

    private BigDecimal overrunPrice(Booking booking, long minutes) {
        if (pricing != null) {
            try {
                var quote = pricing.quote(booking.getResource().getCode(), null, booking.getEndedAt(), booking.getEndedAt().plusMinutes(minutes),
                        booking.getQuantity(), booking.getOwnerType() == null ? null : booking.getOwnerType().name(), booking.getOwnerCode(), null);
                if (quote != null && quote.amount() != null) {
                    return quote.amount();
                }
            } catch (RuntimeException ex) {
                log.info("Depassement de {} : prix du moteur indisponible ({}), prix au prorata", booking.getBookingNumber(), ex.getMessage());
            }
        }
        if (booking.getTotalAmount() == null || booking.getDurationMinutes() == null || booking.getDurationMinutes() <= 0) {
            return BigDecimal.ZERO;
        }
        return booking.getTotalAmount().multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(booking.getDurationMinutes()), 0, RoundingMode.HALF_UP);
    }

    /** Frais de non-presentation de la politique. */
    public void onNoShow(Booking booking) {
        ResourcePolicy policy = booking.getResource() == null ? null : booking.getResource().getEffectivePolicy();
        if (policy == null || policy.getNoShowFee() == null || policy.getNoShowFee() <= 0) {
            return;
        }
        charge(booking, BookingLineType.NO_SHOW, null, "Non-présentation", 1, BigDecimal.valueOf(policy.getNoShowFee()), ADDON_SOURCE);
    }

    // ------------------------------------------------------------------ code promo

    /**
     * Code promo sur le prix de la ressource, par le moteur des promotions (conditions, budget,
     * plafond, une fois par reservation). Avant facture : le montant baisse ; apres : un avoir.
     */
    @Transactional
    public LineView applyPromoCode(String bookingNumber, String couponCode, String actor) {
        Booking booking = booking(bookingNumber);
        if (promotions == null || discounts == null || !StringUtils.hasText(couponCode)) {
            throw new BadRequestException("Le code promo est requis");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED
                || booking.getStatus() == BookingStatus.EXPIRED) {
            throw new ConflictException("Réservation " + booking.getBookingNumber() + " close : plus de remise possible");
        }
        String code = couponCode.trim().toUpperCase(Locale.ROOT);
        boolean already = lineRepository.findByBookingId(booking.getId()).stream()
                .anyMatch(line -> line.getLineType() == BookingLineType.DISCOUNT);
        if (already) {
            throw new ConflictException("Une remise est déjà appliquée à cette réservation");
        }
        BigDecimal base = booking.getTotalAmount() == null ? BigDecimal.ZERO : booking.getTotalAmount();
        if (base.signum() <= 0) {
            throw new ConflictException("Rien à remiser : la réservation est gratuite ou payée par un abonnement");
        }
        Resource resource = booking.getResource();
        PricingContext context = new PricingContext(booking.getOwnerType() == null ? null : booking.getOwnerType().name(), booking.getOwnerCode(), null, null, null, null,
                List.of(new PricingContext.PricingLine(booking.getBookingNumber(), TargetScope.RESOURCE, resource.getCode(),
                        resource.getResourceType() == null ? null : resource.getResourceType().getCode(), resource.getName(), 1, base,
                        BigDecimal.ZERO)),
                null, null, null, null, booking.getCurrency(), List.of(code), null, true);
        PricingResult trial = promotions.evaluate(context);
        if (trial.discountTotal() == null || trial.discountTotal().signum() <= 0) {
            String why = trial.rejections() == null || trial.rejections().isEmpty() ? "aucune remise pour cette réservation"
                    : trial.rejections().stream().map(PricingResult.Rejection::reason).collect(Collectors.joining(" ; "));
            throw new BadRequestException("Code " + code + " refusé : " + why);
        }
        PricingResult applied = discounts.apply(context, DiscountDocumentType.BOOKING, booking.getBookingNumber(), actor);
        BigDecimal discount = applied.discountTotal().min(base);
        String label = "Remise " + code;
        BookingLine line = line(booking, BookingLineType.DISCOUNT, code, label, 1, discount.negate());
        String document = reduce(booking, discount, label);
        line.setBillableNumber(document);
        lineRepository.save(line);
        booking.setTotalAmount(base.subtract(discount));
        bookingRepository.save(booking);
        eventWriter.write(booking, BookingEventType.BOOKING_PRICE_ADJUSTED, "Remise appliquée",
                label + " : -" + discount.stripTrailingZeros().toPlainString() + " " + booking.getCurrency()
                        + (document == null ? "" : ", pièce " + document), null);
        return view(line);
    }

    /** Baisse le montant facture : element non facture modifie, sinon avoir sur la facture. */
    private String reduce(Booking booking, BigDecimal discount, String label) {
        if (!StringUtils.hasText(booking.getBillableNumber())) {
            return null;
        }
        BillableItem item = billableItemRepository.findByBillableNumber(booking.getBillableNumber()).orElse(null);
        if (item == null) {
            return null;
        }
        if (item.getStatus() == BillableItemStatus.PENDING) {
            item.setAmount(item.getAmount().subtract(discount).max(BigDecimal.ZERO));
            billableItemRepository.save(item);
            return null;
        }
        BillingDocument invoice = documents == null ? null
                : documents.findFirstBySourceAndType("BILLABLE_ITEM", booking.getBillableNumber(), "INVOICE").orElse(null);
        if (invoice == null || billingDocumentService == null) {
            log.warn("Reservation {} : remise {} sans facture retrouvee, avoir a emettre par l'equipe", booking.getBookingNumber(), discount);
            return null;
        }
        boolean unpaid = invoice.getBalanceDue() != null && invoice.getBalanceDue().signum() > 0;
        String reason = label + " · réservation " + booking.getBookingNumber();
        try {
            return billingDocumentService.createCreditNote(invoice.getDocumentNumber(),
                    new CreateCreditNoteRequest(discount, reason, unpaid, true, null)).documentNumber();
        } catch (RuntimeException ex) {
            log.warn("Avoir de remise de {} non valide d'office ({}) : laisse en brouillon", booking.getBookingNumber(), ex.getMessage());
            return billingDocumentService.createCreditNote(invoice.getDocumentNumber(),
                    new CreateCreditNoteRequest(discount, reason, false, false, null)).documentNumber();
        }
    }

    // ------------------------------------------------------------------ mecanique

    /**
     * Ligne de supplement : en attente si la reservation n'est pas encore facturee (elle partira sur
     * la meme facture), facturee a part sinon. Une ligne a zero reste pour la trace.
     */
    BookingLine charge(Booking booking, BookingLineType type, String code, String label, int quantity, BigDecimal unitPrice,
                       String sourceType) {
        BookingLine line = lineRepository.save(line(booking, type, code, label, quantity, unitPrice));
        if (StringUtils.hasText(booking.getBillableNumber()) && line.getAmount().signum() > 0) {
            BillableItem item = item(booking, line);
            line.setBillableNumber(item.getBillableNumber());
            lineRepository.save(line);
            invoiceSupport.ensureInvoiced(item, label + " · " + booking.getBookingNumber(), label);
        }
        eventWriter.write(booking, BookingEventType.BOOKING_PRICE_ADJUSTED, "Supplément ajouté",
                label + " : " + line.getAmount().stripTrailingZeros().toPlainString() + " " + booking.getCurrency(), null);
        return line;
    }

    private BookingLine line(Booking booking, BookingLineType type, String code, String label, int quantity, BigDecimal unitPrice) {
        BigDecimal qty = BigDecimal.valueOf(quantity);
        return BookingLine.builder()
                .booking(booking)
                .lineType(type)
                .itemCode(code)
                .description(label.length() > 250 ? label.substring(0, 250) : label)
                .quantity(qty)
                .unit(booking.getBookingUnit() == null ? ResourceBookingUnit.HOUR : booking.getBookingUnit())
                .unitPrice(unitPrice)
                .amount(unitPrice.multiply(qty))
                .currency(booking.getCurrency())
                .createdAt(Instant.now())
                .build();
    }

    private BillableItem item(Booking booking, BookingLine line) {
        boolean deposit = line.getLineType() == BookingLineType.DEPOSIT;
        return billableItemRepository.save(BillableItem.builder()
                .billableNumber(sequenceGenerator.next("billable_item"))
                .sourceType(deposit ? DEPOSIT_SOURCE : ADDON_SOURCE)
                .sourceId(booking.getBookingNumber())
                .subscriberType(booking.getOwnerType())
                .subscriberCode(booking.getOwnerCode())
                .subscriberName(booking.getContactName())
                .subscriberEmail(booking.getContactEmail())
                .subscriberPhone(booking.getContactPhone())
                .billingCompanyCode(booking.getBillingCompanyCode())
                .description(line.getDescription() + " · réservation " + booking.getBookingNumber())
                .amount(line.getAmount())
                .currency(booking.getCurrency())
                .billingPeriodStart(booking.getStartedAt().toLocalDate())
                .billingPeriodEnd(booking.getEndedAt().toLocalDate())
                .status(BillableItemStatus.PENDING)
                .build());
    }

    private Booking booking(String number) {
        if (!StringUtils.hasText(number)) {
            throw new BadRequestException("Le numéro de réservation est requis");
        }
        return bookingRepository.findByBookingNumber(number.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable : " + number.trim()));
    }

    private Booking editable(String number) {
        Booking booking = booking(number);
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED
                || booking.getStatus() == BookingStatus.EXPIRED || booking.getStatus() == BookingStatus.NO_SHOW) {
            throw new ConflictException("Réservation " + booking.getBookingNumber() + " close : plus de supplément possible");
        }
        return booking;
    }

    private static LineView view(BookingLine line) {
        return new LineView(line.getLineType().name(), line.getItemCode(), line.getDescription(), line.getQuantity(),
                line.getUnitPrice(), line.getAmount(), line.getCurrency(), line.getBillableNumber());
    }
}
