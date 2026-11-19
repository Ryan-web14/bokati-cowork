package com.sni.bokaticowork.features.booking.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.dto.response.BookingResponse;
import com.sni.bokaticowork.features.booking.service.support.BookingDeskService;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Lot B9b : planning d'accueil du jour et series recurrentes. */
@RestController
@RequestMapping(ApiPath.V1 + "/bookings")
@RequiredArgsConstructor
public class BookingDeskController {

    private final BookingDeskService desk;

    public record SeriesCancelRequest(@Size(max = 500) String reason) {
    }

    public record SeriesRescheduleRequest(LocalTime newStartTime, LocalTime newEndTime, Integer shiftDays, Boolean acceptPriceChange) {
    }

    /** Planning du jour (aujourd'hui par defaut), par ressource ; {@code resourceCode} restreint. */
    @GetMapping("/planning")
    public ResponseEntity<BookingDeskService.Planning> planning(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                                @RequestParam(required = false) String resourceCode) {
        return ResponseEntity.ok(desk.planning(date, resourceCode));
    }

    @GetMapping("/series/{recurrenceGroupNumber}")
    public ResponseEntity<List<BookingResponse>> series(@PathVariable String recurrenceGroupNumber) {
        return ResponseEntity.ok(desk.series(recurrenceGroupNumber));
    }

    /** Annule la serie a partir de {@code from} (toutes les occurrences a venir sans date). */
    @PatchMapping("/series/{recurrenceGroupNumber}/cancel")
    public ResponseEntity<BookingDeskService.SeriesOutcome> cancelSeries(@PathVariable String recurrenceGroupNumber,
                                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                         @RequestBody(required = false) SeriesCancelRequest request) {
        return ResponseEntity.ok(desk.cancelSeries(recurrenceGroupNumber, from, request == null ? null : request.reason()));
    }

    /** Deplace la serie a partir de {@code from} : nouvel horaire le meme jour, ou decalage en jours. */
    @PatchMapping("/series/{recurrenceGroupNumber}/reschedule")
    public ResponseEntity<BookingDeskService.SeriesOutcome> rescheduleSeries(@PathVariable String recurrenceGroupNumber,
                                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                             @RequestBody SeriesRescheduleRequest request) {
        return ResponseEntity.ok(desk.rescheduleSeries(recurrenceGroupNumber, from, request.newStartTime(), request.newEndTime(),
                request.shiftDays(), request.acceptPriceChange()));
    }
}
