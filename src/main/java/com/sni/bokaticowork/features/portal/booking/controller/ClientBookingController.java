package com.sni.bokaticowork.features.portal.booking.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.cash.dto.response.CashPaymentDeclarationResponse;
import com.sni.bokaticowork.features.payment.cash.dto.request.DeclareCashPaymentRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingAvailabilityResponse;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.portal.booking.dto.request.ClientCancelBookingRequest;
import com.sni.bokaticowork.features.portal.booking.dto.request.ClientCheckAvailabilityRequest;
import com.sni.bokaticowork.features.portal.booking.dto.request.ClientCreateBookingRequest;
import com.sni.bokaticowork.features.portal.booking.dto.response.ClientBookingResponse;
import com.sni.bokaticowork.features.portal.booking.dto.response.ClientBookingSummaryResponse;
import com.sni.bokaticowork.features.portal.booking.service.ClientPortalBookingService;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/client/bookings")
public class ClientBookingController {

    private final ClientContextService clientContextService;
    private final ClientPortalBookingService clientPortalBookingService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<ClientBookingSummaryResponse>> listBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientPortalBookingService.listBookings(member, status, from, to, pageable));
    }

    public record ClientRescheduleRequest(@jakarta.validation.constraints.NotNull java.time.LocalDateTime newStartedAt,
                                          @jakarta.validation.constraints.NotNull java.time.LocalDateTime newEndedAt,
                                          Boolean acceptPriceChange) {
    }

    /** Lot B9d : deplacer sa reservation, dans la politique de la ressource. */
    @PostMapping("/{bookingNumber}/reschedule")
    public ResponseEntity<ClientBookingResponse> reschedule(@PathVariable String bookingNumber, @Valid @RequestBody ClientRescheduleRequest request) {
        return ResponseEntity.ok(clientPortalBookingService.rescheduleBooking(clientContextService.getAuthenticatedMember(), bookingNumber,
                request.newStartedAt(), request.newEndedAt(), request.acceptPriceChange()));
    }

    /** Lot B9d : apercu du prix d'un deplacement. */
    @GetMapping("/{bookingNumber}/reschedule/preview")
    public ResponseEntity<com.sni.bokaticowork.features.booking.dto.response.BookingChangePreviewResponse> previewReschedule(
            @PathVariable String bookingNumber, @RequestParam java.time.LocalDateTime newStartedAt, @RequestParam java.time.LocalDateTime newEndedAt) {
        return ResponseEntity.ok(clientPortalBookingService.previewReschedule(clientContextService.getAuthenticatedMember(), bookingNumber,
                newStartedAt, newEndedAt));
    }

    /** Lot B3 : le rendez-vous regle sur place est-il ouvert a ce membre (et pourquoi) ? */
    @GetMapping("/pay-on-site")
    public ResponseEntity<com.sni.bokaticowork.features.booking.service.support.BookingPayOnSite.Access> payOnSite() {
        return ResponseEntity.ok(clientPortalBookingService.payOnSiteAccess(clientContextService.getAuthenticatedMember()));
    }

    @GetMapping("/{bookingNumber}")
    public ResponseEntity<ClientBookingResponse> getBooking(@PathVariable String bookingNumber) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientPortalBookingService.getBooking(member, bookingNumber));
    }

    @PostMapping({"/create", ""})
    public ResponseEntity<ClientBookingResponse> createBooking(
            @Valid @RequestBody ClientCreateBookingRequest request) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.status(201).body(clientPortalBookingService.createBooking(member, request));
    }

    @DeleteMapping("/{bookingNumber}")
    public ResponseEntity<ClientBookingResponse> cancelBooking(
            @PathVariable String bookingNumber,
            @RequestBody(required = false) ClientCancelBookingRequest request) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientPortalBookingService.cancelBooking(member, bookingNumber, request));
    }

    /**
     * « Je passerai payer ma reservation en especes ».
     *
     * <p>Le creneau est tenu jusqu a l echeance de l annonce · passe ce delai sans encaissement,
     * il est rendu. Rien n est encaisse tant que la caisse n a pas compte les billets.</p>
     */
    @PostMapping("/{bookingNumber}/pay/cash")
    public ResponseEntity<CashPaymentDeclarationResponse> declareCashPayment(
            @PathVariable String bookingNumber,
            @Valid @RequestBody(required = false) DeclareCashPaymentRequest request) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientPortalBookingService.declareCashPayment(member, bookingNumber, request));
    }

    @PostMapping("/availability")
    public ResponseEntity<BookingAvailabilityResponse> checkAvailability(
            @Valid @RequestBody ClientCheckAvailabilityRequest request) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientPortalBookingService.checkAvailability(member, request));
    }
}
