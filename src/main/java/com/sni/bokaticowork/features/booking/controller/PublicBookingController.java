package com.sni.bokaticowork.features.booking.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.service.support.BookingConfirmationDocumentService;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * Endpoints publics (sans authentification) pour le check-in par QR code et le CSAT.
 * Le pattern /public/** est en permitAll dans SecurityConfig.
 */
@Controller
@RequestMapping(ApiPath.V1 + "/public/bookings")
@RequiredArgsConstructor
public class PublicBookingController {

    private final BookingService bookingService;
    private final BookingConfirmationDocumentService confirmationDocumentService;

    @GetMapping("/{bookingNumber}")
    public ModelAndView view(@PathVariable String bookingNumber,
                             @RequestParam String token) {
        var booking = confirmationDocumentService.publicBooking(bookingNumber, token);
        ModelAndView mav = new ModelAndView("booking/confirmation-view");
        confirmationDocumentService.fillViewModel(mav.getModelMap(), booking);
        return mav;
    }

    @GetMapping("/{bookingNumber}/confirmation.pdf")
    public ResponseEntity<byte[]> confirmationPdf(@PathVariable String bookingNumber,
                                                  @RequestParam String token) {
        byte[] pdf = confirmationDocumentService.confirmationPdf(bookingNumber, token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"booking-confirmation-" + bookingNumber + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /**
     * Déclenché par le scan du QR code reçu dans l'email de confirmation.
     * Retourne une page HTML de confirmation au lieu d'une réponse JSON.
     */
    @GetMapping("/check-in/scan/{checkInToken}")
    public ModelAndView scanCheckIn(@PathVariable String checkInToken) {
        ModelAndView mav = new ModelAndView("booking/checkin-result");
        try {
            var booking = bookingService.checkInByToken(checkInToken, null);
            mav.addObject("success", true);
            mav.addObject("bookingNumber", booking.bookingNumber());
            mav.addObject("resourceName", booking.resourceName());
            mav.addObject("contactName", booking.contactName());
            mav.addObject("status", booking.status().name());
            mav.addObject("startedAt", booking.startedAt());
            mav.addObject("endedAt", booking.endedAt());
        } catch (Exception e) {
            mav.addObject("success", false);
            mav.addObject("token", checkInToken);
        }
        return mav;
    }

    /**
     * Enregistre le score CSAT cliqué depuis le lien de l'email de fin de réservation.
     */
    @GetMapping("/csat/{bookingNumber}")
    public ModelAndView recordCsat(@PathVariable String bookingNumber,
                                   @RequestParam(defaultValue = "0") int score) {
        try {
            bookingService.recordCsat(bookingNumber, score);
        } catch (Exception ignored) {
        }
        ModelAndView mav = new ModelAndView("booking/csat-thanks");
        mav.addObject("score", score);
        mav.addObject("bookingNumber", bookingNumber);
        return mav;
    }
}
