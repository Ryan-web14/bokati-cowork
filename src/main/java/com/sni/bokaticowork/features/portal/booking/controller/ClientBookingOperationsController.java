package com.sni.bokaticowork.features.portal.booking.controller;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.service.support.BookingOperations;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import com.sni.bokaticowork.features.ressource.service.support.ResourceOperations;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lot R5d, cote membre : reglement d'usage de ses reservations, etat des lieux de fin, signalement
 * d'incident. Le membre ne bloque jamais une ressource : l'equipe decide.
 */
@RestController
@RequiredArgsConstructor
public class ClientBookingOperationsController {

    private final ClientContextService clientContextService;
    private final BookingOperations operations;
    private final ResourceOperations resourceOperations;

    @GetMapping(ApiPath.V1 + "/client/bookings/{number}/usage-rules")
    public ResponseEntity<BookingOperations.UsageRulesView> usageRules(@PathVariable String number) {
        return ResponseEntity.ok(operations.usageRules(owned(number)));
    }

    @PostMapping(ApiPath.V1 + "/client/bookings/{number}/usage-rules/accept")
    public ResponseEntity<BookingOperations.UsageRulesView> accept(@PathVariable String number) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(operations.acceptUsageRules(owned(number), member.getMemberId()));
    }

    @PostMapping(ApiPath.V1 + "/client/bookings/{number}/end-report")
    public ResponseEntity<BookingOperations.EndReportResponse> endReport(@PathVariable String number,
                                                                         @RequestBody BookingOperations.EndReportRequest request) {
        Member member = clientContextService.getAuthenticatedMember();
        operations.checkPhotos(request == null ? null : request.photoDocumentCodes(), member.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(operations.endReport(owned(number), request, "MEMBER",
                member.getMemberId(), name(member), member.getEmail(), member.getPhone()));
    }

    /** Signalement d'un membre, sur une ressource qu'il a reservee (numero de reservation requis). */
    @PostMapping(ApiPath.V1 + "/client/resource-incidents")
    public ResponseEntity<ResourceOperations.IncidentResponse> report(@RequestBody ResourceOperations.IncidentRequest request) {
        Member member = clientContextService.getAuthenticatedMember();
        Booking booking = owned(request == null ? null : request.bookingNumber());
        operations.checkPhotos(request.photoDocumentCodes(), member.getId());
        ResourceOperations.IncidentRequest safe = new ResourceOperations.IncidentRequest(booking.getResource().getCode(),
                booking.getBookingNumber(), request.title(), request.description(), request.severity(), false, false,
                request.photoDocumentCodes());
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceOperations.report(safe,
                new ResourceOperations.Reporter("MEMBER", member.getMemberId(), name(member), member.getEmail(), member.getPhone())));
    }

    private Booking owned(String number) {
        Member member = clientContextService.getAuthenticatedMember();
        Booking booking = operations.booking(number);
        if (booking.getOwnerType() != SubscriberType.MEMBER || member == null || !member.getMemberId().equals(booking.getOwnerCode())) {
            throw new ResourceNotFoundException("Réservation introuvable : " + number);
        }
        return booking;
    }

    private static String name(Member member) {
        String full = ((member.getFirstname() == null ? "" : member.getFirstname()) + " "
                + (member.getLastname() == null ? "" : member.getLastname())).trim();
        return full.isEmpty() ? member.getMemberId() : full;
    }
}
