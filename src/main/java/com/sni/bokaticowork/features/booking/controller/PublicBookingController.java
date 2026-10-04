package com.sni.bokaticowork.features.booking.controller;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.config.BookingCheckInProperties;
import com.sni.bokaticowork.features.booking.service.support.BookingConfirmationDocumentService;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import com.sni.bokaticowork.features.booking.service.support.ScannerTerminalGate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/**
 * Endpoints publics (sans authentification) pour le check-in, le QR code scanner et le CSAT.
 * Le pattern /public/** est en permitAll dans SecurityConfig.
 */
@Controller
@RequestMapping(ApiPath.V1 + "/public/bookings")
@RequiredArgsConstructor
public class PublicBookingController {

    private final BookingService bookingService;
    private final BookingConfirmationDocumentService confirmationDocumentService;
    private final BookingCheckInProperties checkInProperties;
    private final ScannerTerminalGate scannerTerminalGate;

    @GetMapping("/{bookingNumber}")
    public ModelAndView view(@PathVariable String bookingNumber,
                             @RequestParam String token) {
        var booking = confirmationDocumentService.publicBooking(bookingNumber, token);
        ModelAndView mav = new ModelAndView("booking/confirmation-view");
        confirmationDocumentService.fillViewModel(mav.getModelMap(), booking);
        mav.addObject("checkInToken", booking.getCheckInToken());
        mav.addObject("selfCheckInUrl", ApiPath.V1 + "/public/bookings/check-in/self");
        mav.addObject("locationRequired", checkInProperties.isLocationConfigured());
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
     * La page que le QR code ouvre · elle ne pointe personne.
     *
     * <p>Le pointage se faisait ici, sur un GET, avec pour seule autorite un cookie envoye a tout
     * le domaine : une page quelconque visitee par le terminal pouvait donc declencher des
     * pointages avec un simple {@code <img src>}, la protection CSRF etant desactivee
     * globalement. Un GET ne doit rien changer · celui-ci affiche la reservation et demande une
     * confirmation, qui part en POST.</p>
     */
    @GetMapping("/check-in/scan/{checkInToken}")
    public ModelAndView scanCheckIn(@PathVariable String checkInToken,
                                    HttpServletRequest request) {
        if (!scannerTerminalGate.isAuthorizedTerminal(request)) {
            return adminCodePage(checkInToken, null);
        }
        ModelAndView mav = new ModelAndView("booking/checkin-confirm");
        mav.addObject("checkInToken", checkInToken);
        mav.addObject("confirmUrl", ApiPath.V1 + "/public/bookings/check-in/scan");
        return mav;
    }

    /**
     * Le pointage lui-meme · POST, et seulement depuis un poste autorise.
     */
    @PostMapping("/check-in/scan")
    public ModelAndView confirmScanCheckIn(@RequestParam String checkInToken,
                                           HttpServletRequest request) {
        if (!scannerTerminalGate.isAuthorizedTerminal(request)) {
            // L'autorisation a expire entre l'affichage et la confirmation · on redemande la cle.
            return adminCodePage(checkInToken, null);
        }
        return performScanCheckIn(checkInToken);
    }

    /**
     * Valide le code du poste, pose l'autorisation, puis effectue le pointage.
     *
     * <p>Le pointage a lieu ici sans confirmation supplementaire : la requete est un POST et la
     * cle vient d'etre prouvee dans cette meme requete.</p>
     */
    @PostMapping("/check-in/scanner-verify")
    public ModelAndView scannerVerify(@RequestParam String checkInToken,
                                      @RequestParam String adminCode,
                                      HttpServletResponse response) {
        if (!checkInProperties.isScannerKeyValid(adminCode)) {
            return adminCodePage(checkInToken, "Code invalide. Veuillez réessayer.");
        }
        scannerTerminalGate.authorizeTerminal(response);
        return performScanCheckIn(checkInToken);
    }

    /**
     * Page de configuration du terminal scanner.
     */
    @GetMapping("/check-in/scanner-setup")
    public ModelAndView scannerSetupPage() {
        ModelAndView mav = new ModelAndView("booking/checkin-scanner-setup");
        mav.addObject("scannerConfigured", checkInProperties.isScannerKeyConfigured());
        mav.addObject("setupUrl", ApiPath.V1 + "/public/bookings/check-in/scanner-setup");
        return mav;
    }

    @PostMapping("/check-in/scanner-setup")
    public ModelAndView scannerSetup(@RequestParam String setupKey,
                                     HttpServletResponse response) {
        if (!checkInProperties.isScannerKeyValid(setupKey)) {
            ModelAndView mav = new ModelAndView("booking/checkin-scanner-setup");
            mav.addObject("scannerConfigured", checkInProperties.isScannerKeyConfigured());
            mav.addObject("setupUrl", ApiPath.V1 + "/public/bookings/check-in/scanner-setup");
            mav.addObject("error", "Clé invalide. Contactez l'administrateur système.");
            return mav;
        }
        scannerTerminalGate.authorizeTerminal(response);
        ModelAndView mav = new ModelAndView("booking/checkin-scanner-setup");
        mav.addObject("scannerConfigured", true);
        mav.addObject("setupSuccess", true);
        mav.addObject("setupUrl", ApiPath.V1 + "/public/bookings/check-in/scanner-setup");
        mav.addObject("grantValidityDays", checkInProperties.grantValidity().toDays());
        return mav;
    }

    private ModelAndView adminCodePage(String checkInToken, String error) {
        ModelAndView mav = new ModelAndView("booking/checkin-admin-code");
        mav.addObject("checkInToken", checkInToken);
        mav.addObject("scannerConfigured", checkInProperties.isScannerKeyConfigured());
        mav.addObject("verifyUrl", ApiPath.V1 + "/public/bookings/check-in/scanner-verify");
        if (error != null) {
            mav.addObject("error", error);
        }
        return mav;
    }

    /**
     * Client self-check-in depuis la page de confirmation (bouton dans l'email).
     * Valide la fenêtre de temps et la géolocalisation si configurée.
     */
    @PostMapping(value = "/check-in/self", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> clientSelfCheckIn(
            @RequestParam String token,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {

        if (checkInProperties.isLocationConfigured() && (lat == null || lng == null)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "location_required",
                    "message", "La géolocalisation est requise pour effectuer le check-in depuis la page de réservation."
            ));
        }
        if (lat != null && lng != null && !checkInProperties.isWithinRange(lat, lng)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "error", "location_invalid",
                    "message", "Vous devez être présent dans l'espace de coworking pour effectuer le check-in."
            ));
        }
        try {
            var booking = bookingService.checkInByToken(token, null);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "bookingNumber", booking.bookingNumber(),
                    "resourceName", booking.resourceName(),
                    "status", booking.status().name()
            ));
        } catch (ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "error", "time_invalid",
                    "message", e.getMessage()
            ));
        } catch (ConflictException | BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "checkin_failed",
                    "message", e.getMessage()
            ));
        }
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

    private ModelAndView performScanCheckIn(String checkInToken) {
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
}
