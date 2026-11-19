package com.sni.bokaticowork.features.booking.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.dto.request.JoinBookingWaitlistRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingWaitlistEntryResponse;
import com.sni.bokaticowork.features.booking.enums.BookingWaitlistStatus;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingWaitlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/bookings/waitlist")
public class BookingWaitlistController {

    private final BookingWaitlistService service;

    @PostMapping
    public ResponseEntity<BookingWaitlistEntryResponse> join(@Valid @RequestBody JoinBookingWaitlistRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.join(request));
    }

    /**
     * Lot B6 : liste filtree et paginee (statut, ressource, titulaire), la plus ancienne d'abord.
     * Sans {@code status} : tous les etats.
     */
    @GetMapping
    public ResponseEntity<com.sni.bokaticowork.core.templateResponse.PaginatedResponse<BookingWaitlistEntryResponse>> list(
            @RequestParam(required = false) BookingWaitlistStatus status,
            @RequestParam(required = false) String resourceCode,
            @RequestParam(required = false) com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType ownerType,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.search(status, resourceCode, ownerType, ownerCode,
                org.springframework.data.domain.PageRequest.of(page, size)));
    }

    /** Lot B6 : l'equipe accepte l'offre pour le client ; la reservation est creee. */
    @PostMapping("/{id}/accept")
    public ResponseEntity<BookingWaitlistEntryResponse> accept(@PathVariable Long id) {
        return ResponseEntity.ok(service.accept(id));
    }

    /** Lot B6 : l'offre est declinee ; la place passe a l'inscription suivante. */
    @PostMapping("/{id}/decline")
    public ResponseEntity<BookingWaitlistEntryResponse> decline(@PathVariable Long id) {
        return ResponseEntity.ok(service.decline(id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingWaitlistEntryResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancel(id));
    }

    @PostMapping("/promote-available")
    public ResponseEntity<Integer> promoteAvailable(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(service.promoteAvailable(limit));
    }

    @PostMapping("/expire-offers")
    public ResponseEntity<Integer> expireOffers(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(service.expireOffers(limit));
    }
}
