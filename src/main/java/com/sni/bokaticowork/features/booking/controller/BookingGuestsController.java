package com.sni.bokaticowork.features.booking.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.dto.response.BookingParticipantResponse;
import com.sni.bokaticowork.features.booking.dto.response.BookingResponse;
import com.sni.bokaticowork.features.booking.mapper.interfaces.BookingMapper;
import com.sni.bokaticowork.features.booking.service.support.BookingGuests;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Lot B9d : invitations des participants, pointage des invites, details de la reservation. */
@RestController
@RequestMapping(ApiPath.V1 + "/bookings")
@RequiredArgsConstructor
public class BookingGuestsController {

    private final BookingGuests guests;
    private final BookingMapper mapper;

    /** Invite (ou relance) un participant par courriel ; {@code sent} vaut 0 sans courriel. */
    @PostMapping("/{bookingNumber}/participants/{participantId}/invite")
    public ResponseEntity<Map<String, Integer>> invite(@PathVariable String bookingNumber, @PathVariable Long participantId) {
        return ResponseEntity.ok(Map.of("sent", guests.invite(bookingNumber, participantId)));
    }

    /** Invite tous les participants qui n'ont pas encore repondu. */
    @PostMapping("/{bookingNumber}/participants/invite")
    public ResponseEntity<Map<String, Integer>> inviteAll(@PathVariable String bookingNumber) {
        return ResponseEntity.ok(Map.of("sent", guests.inviteAll(bookingNumber)));
    }

    /** Pointage d'un invite a l'accueil. */
    @PatchMapping("/{bookingNumber}/participants/{participantId}/check-in")
    public ResponseEntity<BookingParticipantResponse> checkIn(@PathVariable String bookingNumber, @PathVariable Long participantId) {
        return ResponseEntity.ok(mapper.toResponse(guests.checkIn(bookingNumber, participantId)));
    }

    /** Disposition de la salle, demandes speciales, notes internes, etiquettes ; seuls les champs donnes changent. */
    @PatchMapping("/{bookingNumber}/details")
    public ResponseEntity<BookingResponse> details(@PathVariable String bookingNumber, @RequestBody BookingGuests.Details details) {
        return ResponseEntity.ok(mapper.toResponse(guests.updateDetails(bookingNumber, details)));
    }
}
