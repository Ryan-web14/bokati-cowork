package com.sni.bokaticowork.features.booking.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.dto.response.BookingCheckInScanResponse;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints publics (sans authentification) pour le check-in par QR code.
 * Le pattern /public/** est en permitAll dans SecurityConfig.
 */
@RestController
@RequestMapping(ApiPath.V1 + "/public/bookings")
@RequiredArgsConstructor
public class PublicBookingController {

    private final BookingService bookingService;

    /**
     * Déclenché par le scan du QR code reçu dans l'email de confirmation.
     * Effectue le check-in et passe le booking CONFIRMED → IN_PROGRESS.
     * Le checkInToken (32 hex chars, 128-bit entropie) sert de credential unique.
     */
    @GetMapping("/check-in/scan/{checkInToken}")
    public ResponseEntity<BookingCheckInScanResponse> scanCheckIn(@PathVariable String checkInToken) {
        var booking = bookingService.checkInByToken(checkInToken, null);
        return ResponseEntity.ok(new BookingCheckInScanResponse(
                booking.bookingNumber(),
                booking.resourceName(),
                booking.contactName(),
                booking.status(),
                booking.startedAt(),
                booking.endedAt()
        ));
    }
}
