package com.sni.bokaticowork.features.booking.controller;

import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.service.support.BookingClientReport;
import com.sni.bokaticowork.features.booking.service.support.BookingInsights;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Lot B9g : tableau de bord, qualite par ressource et rapport imprimable par client. */
@RestController
@RequestMapping(ApiPath.V1 + "/bookings")
@RequiredArgsConstructor
public class BookingInsightsController {

    private final BookingInsights insights;
    private final BookingClientReport report;
    private final ObjectProvider<PlatformSettings> settings;

    /** Tableau de bord de la periode (mois en cours par defaut). */
    @GetMapping("/dashboard")
    public ResponseEntity<BookingInsights.Dashboard> dashboard(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        PlatformSettings s = settings.getIfAvailable();
        return ResponseEntity.ok(insights.dashboard(from, to, s == null ? 24 : s.integer("bookings.reliability.late-cancel-hours", 24)));
    }

    /** Avis apres utilisation par ressource (trois derniers mois par defaut), la plus mal notee d'abord. */
    @GetMapping("/reviews")
    public ResponseEntity<List<BookingInsights.ReviewLine>> reviews(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                                    @RequestParam(required = false) String resourceCode) {
        PlatformSettings s = settings.getIfAvailable();
        return ResponseEntity.ok(insights.reviews(from, to, resourceCode, s == null ? 2 : s.integer("bookings.reviews.alert-threshold", 2)));
    }

    /** Rapport de reservations d'un client, en PDF (mois en cours par defaut). */
    @GetMapping(value = "/client-report.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> clientReport(@RequestParam SubscriberType ownerType, @RequestParam String ownerCode,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] pdf = report.pdf(ownerType, ownerCode, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"reservations-" + ownerCode.trim() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
