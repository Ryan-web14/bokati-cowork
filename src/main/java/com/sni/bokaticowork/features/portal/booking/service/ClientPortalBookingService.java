package com.sni.bokaticowork.features.portal.booking.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.booking.dto.request.BookingAvailabilityRequest;
import com.sni.bokaticowork.features.booking.dto.request.BookingStatusChangeRequest;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingAvailabilityResponse;
import com.sni.bokaticowork.features.booking.dto.response.BookingResponse;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingAvailabilityService;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import com.sni.bokaticowork.features.payment.cash.dto.request.DeclareCashPaymentRequest;
import com.sni.bokaticowork.features.payment.cash.dto.response.CashPaymentDeclarationResponse;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.portal.booking.dto.request.ClientCancelBookingRequest;
import com.sni.bokaticowork.features.portal.booking.dto.request.ClientCheckAvailabilityRequest;
import com.sni.bokaticowork.features.portal.booking.dto.request.ClientCreateBookingRequest;
import com.sni.bokaticowork.features.portal.booking.dto.response.ClientBookingResponse;
import com.sni.bokaticowork.features.portal.booking.dto.response.ClientBookingSummaryResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ClientPortalBookingService {

    private final BookingService bookingService;
    private final BookingAvailabilityService bookingAvailabilityService;
    private final com.sni.bokaticowork.features.portal.resource.service.ClientResourceService clientResourceService;
    private final com.sni.bokaticowork.features.portal.context.SubscriberAccess subscriberAccess;
    private final com.sni.bokaticowork.features.payment.cash.service.CashPaymentDeclarationService cashDeclarationService;

    @Transactional(readOnly = true)
    public PaginatedResponse<ClientBookingSummaryResponse> listBookings(Member member,
                                                                         BookingStatus status,
                                                                         LocalDateTime from,
                                                                         LocalDateTime to,
                                                                         Pageable pageable) {
        PaginatedResponse<BookingResponse> source = bookingService.list(
                null, SubscriberType.MEMBER, member.getMemberId(), null, status, from, to, pageable
        );
        PaginatedResponse<ClientBookingSummaryResponse> result = new PaginatedResponse<>();
        result.setData(source.getData().stream().map(this::toSummary).toList());
        result.setPageable(source.getPageable());
        return result;
    }

    @Transactional(readOnly = true)
    public ClientBookingResponse getBooking(Member member, String bookingNumber) {
        BookingResponse booking = bookingService.get(bookingNumber);
        verifyOwnership(member, booking);
        return toDetailResponse(booking);
    }

    private com.sni.bokaticowork.features.booking.service.support.BookingPayOnSite payOnSite;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setPayOnSite(com.sni.bokaticowork.features.booking.service.support.BookingPayOnSite payOnSite) {
        this.payOnSite = payOnSite;
    }

    /** Lot B3 : le membre peut-il prendre un rendez-vous regle sur place ? */
    @Transactional(readOnly = true)
    public com.sni.bokaticowork.features.booking.service.support.BookingPayOnSite.Access payOnSiteAccess(Member member) {
        return payOnSite == null ? new com.sni.bokaticowork.features.booking.service.support.BookingPayOnSite.Access(false,
                com.sni.bokaticowork.features.booking.service.support.BookingPayOnSite.Source.GLOBAL_SETTING, null)
                : payOnSite.access(com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType.MEMBER, member.getMemberId());
    }

    /**
     * Lot B9d : le membre deplace lui-meme sa reservation, dans la politique de la ressource
     * (preavis, duree, capacite) ; une hausse de prix demande {@code acceptPriceChange}.
     */
    @Transactional
    public ClientBookingResponse rescheduleBooking(Member member, String bookingNumber, java.time.LocalDateTime newStartedAt,
                                                   java.time.LocalDateTime newEndedAt, Boolean acceptPriceChange) {
        verifyOwnership(member, bookingService.get(bookingNumber));
        return toDetailResponse(bookingService.reschedule(bookingNumber,
                new com.sni.bokaticowork.features.booking.dto.request.BookingRescheduleRequest(newStartedAt, newEndedAt, null,
                        "Déplacée par le client", Boolean.TRUE, acceptPriceChange)));
    }

    /** Lot B9f : prolonger sa reservation. */
    @Transactional
    public ClientBookingResponse extendBooking(Member member, String bookingNumber, int minutes, Boolean acceptPriceChange) {
        verifyOwnership(member, bookingService.get(bookingNumber));
        if (filling == null) {
            throw new com.sni.bokaticowork.core.exception.customs.ConflictException("Prolongation indisponible");
        }
        return toDetailResponse(filling.extend(bookingNumber, minutes, acceptPriceChange));
    }

    private com.sni.bokaticowork.features.booking.service.support.BookingFilling filling;
    private com.sni.bokaticowork.features.booking.service.interfaces.BookingSuggestionService suggestions;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setFilling(com.sni.bokaticowork.features.booking.service.support.BookingFilling filling,
                    com.sni.bokaticowork.features.booking.service.interfaces.BookingSuggestionService suggestions) {
        this.filling = filling;
        this.suggestions = suggestions;
    }

    /** Lot B9f : creneaux libres proches d'un creneau pris (meme duree), pour le membre. */
    @Transactional(readOnly = true)
    public java.util.List<com.sni.bokaticowork.features.booking.dto.response.BookingSuggestionResponse> alternatives(Member member, String resourceCode,
                                                                                                                  java.time.LocalDateTime startedAt,
                                                                                                                  java.time.LocalDateTime endedAt,
                                                                                                                  Integer quantity) {
        clientResourceService.assertPortalVisible(resourceCode);
        if (suggestions == null) {
            return java.util.List.of();
        }
        return suggestions.suggest(new com.sni.bokaticowork.features.booking.dto.request.BookingAvailabilityRequest(resourceCode,
                member.getMemberId(), null, null, null, null, Boolean.FALSE, null, null, null, null, startedAt, endedAt,
                quantity == null ? 1 : quantity, null));
    }

    /** Lot B9f : offre de derniere minute, un creneau libre et son prix remise. */
    public record LastMinuteOffer(String resourceCode, String resourceName, java.time.LocalDateTime startedAt, java.time.LocalDateTime endedAt,
                                  java.math.BigDecimal price, java.math.BigDecimal discountedPrice, int discountRate, String currency) {
    }

    /**
     * Lot B9f : creneaux libres d'une heure dans les prochaines heures, au prix remise. Vide si
     * l'offre de derniere minute est coupee.
     */
    @Transactional(readOnly = true)
    public java.util.List<LastMinuteOffer> lastMinute(Member member, Integer durationMinutes) {
        if (filling == null || filling.lastMinuteRate() <= 0) {
            return java.util.List.of();
        }
        int duration = durationMinutes == null || durationMinutes < 30 ? 60 : Math.min(durationMinutes, 480);
        java.time.LocalDateTime now = com.sni.bokaticowork.core.utils.time.AppTime.now();
        java.time.LocalDateTime until = now.plusHours(filling.lastMinuteHours());
        java.util.List<LastMinuteOffer> offers = new java.util.ArrayList<>();
        var resources = clientResourceService.listPortalResources(null, null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 30)).getData();
        for (var resource : resources) {
            try {
                for (var window : clientResourceService.getAvailableSlots(resource.getCode(), now, until, duration, 1, false)) {
                    if (window.getStartedAt() == null || !window.getStartedAt().isAfter(now)) {
                        continue;
                    }
                    java.time.LocalDateTime end = window.getStartedAt().plusMinutes(duration);
                    var quote = clientResourceService.getQuote(resource.getCode(), window.getStartedAt(), end, 1, member.getMemberId());
                    java.math.BigDecimal price = quote == null ? null : quote.amount();
                    offers.add(new LastMinuteOffer(resource.getCode(), resource.getName(), window.getStartedAt(), end, price,
                            filling.discounted(price), filling.lastMinuteRate(), quote == null || quote.currency() == null ? "XAF" : quote.currency()));
                    break;
                }
            } catch (RuntimeException ex) {
                // Ressource sans creneau ou sans prix : pas d'offre pour elle.
            }
        }
        offers.sort(java.util.Comparator.comparing(LastMinuteOffer::startedAt));
        return offers;
    }

    /** Lot B9d : apercu du prix avant un deplacement. */
    @Transactional(readOnly = true)
    public com.sni.bokaticowork.features.booking.dto.response.BookingChangePreviewResponse previewReschedule(Member member, String bookingNumber,
                                                                                                          java.time.LocalDateTime newStartedAt,
                                                                                                          java.time.LocalDateTime newEndedAt) {
        verifyOwnership(member, bookingService.get(bookingNumber));
        return bookingService.previewReschedule(bookingNumber, newStartedAt, newEndedAt);
    }

    @Transactional
    public ClientBookingResponse createBooking(Member member, ClientCreateBookingRequest request) {
        // Une ressource retiree du portail ne se reserve pas depuis le portail · seule sa page
        // etait masquee, la reservation par son code passait.
        clientResourceService.assertPortalVisible(request.getResourceCode());
        // Le seul chemin ou le membre reserve pour lui-meme · l'accueil et le systeme (confirmation
        // apres paiement) passent ailleurs, et ont le droit de reserver une soiree pour quiconque.
        subscriberAccess.assertMayBook(member, request.getResourceCode(), request.getStartedAt(), request.getEndedAt());
        boolean payOnSiteWanted = Boolean.TRUE.equals(request.getPayOnSite());
        if (payOnSiteWanted && payOnSite != null) {
            // Lot B3 (decision D3) : option du membre posee par l'equipe, sinon reglage global.
            payOnSite.assertPortalAllowed(com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType.MEMBER, member.getMemberId());
        }
        CreateBookingRequest req = new CreateBookingRequest(
                request.getResourceCode(),
                member.getMemberId(),
                null,
                null,
                member.getEmail(),
                member.getPhone(),
                false,
                member.getFirstname() + " " + member.getLastname(),
                member.getEmail(),
                member.getPhone(),
                request.getIdempotencyKey(),
                null,
                request.getStartedAt(),
                request.getEndedAt(),
                request.getQuantity(),
                request.getPaymentMode(),
                true,
                true,
                request.getNotes(),
                null,
                // Aucune unite imposee : cote client, le tarif se deduit de la duree, point.
                // ClientCreateBookingRequest ne porte volontairement aucun champ correspondant.
                null,
                request.getParticipants(),
                // « Pour mon organisation » · le membre doit y etre rattache, sinon refus.
                request.getBillingCompanyCode(),
                null,
                null,
                payOnSiteWanted ? Boolean.TRUE : null
        );
        return toDetailResponse(bookingService.create(req));
    }

    /**
     * Le membre annonce qu il reglera sa reservation en especes.
     *
     * <p>Une reservation en paiement direct attend un reglement venu de l exterieur · elle reste
     * PENDING_PAYMENT et son creneau est tenu jusqu a l echeance de l annonce. Rien n est
     * encaisse tant que la caisse n a pas compte les billets.</p>
     */
    @Transactional
    public CashPaymentDeclarationResponse declareCashPayment(Member member, String bookingNumber,
                                                             DeclareCashPaymentRequest request) {
        BookingResponse booking = bookingService.get(bookingNumber);
        verifyOwnership(member, booking);
        if (booking.status() != BookingStatus.PENDING_PAYMENT && booking.status() != BookingStatus.DRAFT) {
            throw new BadRequestException("La réservation " + bookingNumber + " n'attend pas de paiement ("
                    + booking.status() + ").");
        }
        String documentNumber = cashDeclarationService.documentOfBooking(bookingNumber);
        return cashDeclarationService.declare(documentNumber, request, member.getMemberId());
    }

    @Transactional
    public ClientBookingResponse cancelBooking(Member member, String bookingNumber,
                                               ClientCancelBookingRequest request) {
        BookingResponse booking = bookingService.get(bookingNumber);
        verifyOwnership(member, booking);
        if (booking.status() != BookingStatus.CONFIRMED
                && booking.status() != BookingStatus.PENDING_APPROVAL
                && booking.status() != BookingStatus.PENDING_PAYMENT) {
            throw new BadRequestException(
                    "Seule une réservation confirmée ou en attente (paiement ou approbation) s'annule ; celle-ci est " + booking.status().label()
            );
        }
        String reason = request != null ? request.getReason() : null;
        BookingStatusChangeRequest change = new BookingStatusChangeRequest(member.getMemberId(), reason, true);
        return toDetailResponse(bookingService.cancel(bookingNumber, change));
    }

    public BookingAvailabilityResponse checkAvailability(Member member, ClientCheckAvailabilityRequest request) {
        clientResourceService.assertPortalVisible(request.getResourceCode());
        BookingAvailabilityRequest req = new BookingAvailabilityRequest(
                request.getResourceCode(),
                member.getMemberId(),
                null,
                null,
                member.getEmail(),
                member.getPhone(),
                false,
                null,
                null,
                null,
                request.getPaymentMode(),
                request.getStartedAt(),
                request.getEndedAt(),
                request.getQuantity(),
                null,
                request.getBillingCompanyCode()
        );
        BookingAvailabilityResponse result = bookingAvailabilityService.check(req);
        if (result.available() && subscriberAccess.isDenied(member, request.getResourceCode(),
                request.getStartedAt(), request.getEndedAt())) {
            return new BookingAvailabilityResponse(result.resourceCode(), result.resourceName(), result.startedAt(),
                    result.endedAt(), result.durationMinutes(), result.quantity(), false, null,
                    result.bookingUnit(), result.unitPrice(), result.estimatedAmount(), result.currency(),
                    "Créneau réservé aux abonnés");
        }
        return result.withoutBookingNumbers();
    }


    private void verifyOwnership(Member member, BookingResponse booking) {
        if (booking.ownerType() != SubscriberType.MEMBER
                || !member.getMemberId().equals(booking.ownerCode())) {
            throw new ResourceNotFoundException("Réservation introuvable");
        }
    }

    private ClientBookingSummaryResponse toSummary(BookingResponse b) {
        return ClientBookingSummaryResponse.builder()
                .bookingNumber(b.bookingNumber())
                .resourceName(b.resourceName())
                .status(b.status())
                .startedAt(b.startedAt())
                .endedAt(b.endedAt())
                .durationMinutes(b.durationMinutes())
                .quantity(b.quantity())
                .seatsLabel(b.seatsLabel())
                .resourceCapacity(b.resourceCapacity())
                .wholeResourceBooking(b.wholeResourceBooking())
                .bookingUnit(b.bookingUnit())
                .paymentMode(b.paymentMode())
                .paymentStatus(b.paymentStatus())
                .paymentStatusLabel(b.paymentStatusLabel())
                .totalAmount(b.totalAmount())
                .currency(b.currency())
                .confirmedAt(b.confirmedAt())
                .cancelledAt(b.cancelledAt())
                .completedAt(b.completedAt())
                .build();
    }

    private ClientBookingResponse toDetailResponse(BookingResponse b) {
        return ClientBookingResponse.builder()
                .bookingNumber(b.bookingNumber())
                .resourceName(b.resourceName())
                .status(b.status())
                .startedAt(b.startedAt())
                .endedAt(b.endedAt())
                .durationMinutes(b.durationMinutes())
                .quantity(b.quantity())
                .seatsLabel(b.seatsLabel())
                .resourceCapacity(b.resourceCapacity())
                .wholeResourceBooking(b.wholeResourceBooking())
                .bookingUnit(b.bookingUnit())
                .paymentMode(b.paymentMode())
                .paymentStatus(b.paymentStatus())
                .paymentStatusLabel(b.paymentStatusLabel())
                .subscriptionNumber(b.subscriptionNumber())
                .passNumber(b.passNumber())
                .unitPrice(b.unitPrice())
                .subtotalAmount(b.subtotalAmount())
                .totalAmount(b.totalAmount())
                .currency(b.currency())
                .notes(b.notes())
                .checkInToken(b.checkInToken())
                .checkInQrValue(b.checkInQrValue())
                .virtualMeetingUrl(b.virtualMeetingUrl())
                .confirmedAt(b.confirmedAt())
                .completedAt(b.completedAt())
                .cancelledAt(b.cancelledAt())
                .participants(b.participants())
                .build();
    }
}
