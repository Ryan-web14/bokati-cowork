package com.sni.bokaticowork.features.booking.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.service.support.BookingAttendance;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Lot B9a : liens du courriel de rappel, a jeton (celui de la reservation). « Je viens » est
 * enregistre a l'ouverture du lien ; « J'annule » demande une confirmation (formulaire POST) pour
 * qu'un apercu automatique du lien n'annule rien.
 */
@RestController
@RequestMapping(ApiPath.V1 + "/public/bookings")
@RequiredArgsConstructor
public class PublicBookingAttendanceController {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH:mm", Locale.FRENCH);

    private final BookingAttendance attendance;

    @GetMapping("/{bookingNumber}/attendance")
    public ModelAndView answer(@PathVariable String bookingNumber, @RequestParam(required = false) String token,
                               @RequestParam(defaultValue = "COMING") String answer) {
        ModelAndView page = new ModelAndView("booking/attendance");
        page.addObject("bookingNumber", bookingNumber);
        try {
            if ("CANCEL".equalsIgnoreCase(answer)) {
                Booking booking = attendance.verified(bookingNumber, token);
                fill(page, booking);
                page.addObject("state", "ASK_CANCEL");
                page.addObject("token", token);
                page.addObject("cancelAction", ApiPath.V1 + "/public/bookings/" + bookingNumber + "/attendance/cancel");
            } else {
                fill(page, attendance.coming(bookingNumber, token));
                page.addObject("state", "COMING");
            }
        } catch (RuntimeException ex) {
            page.addObject("state", "ERROR");
            page.addObject("message", ex.getMessage());
        }
        return page;
    }

    @PostMapping("/{bookingNumber}/attendance/cancel")
    public ModelAndView cancel(@PathVariable String bookingNumber, @RequestParam(required = false) String token) {
        ModelAndView page = new ModelAndView("booking/attendance");
        page.addObject("bookingNumber", bookingNumber);
        try {
            fill(page, attendance.cancel(bookingNumber, token));
            page.addObject("state", "CANCELLED");
        } catch (RuntimeException ex) {
            page.addObject("state", "ERROR");
            page.addObject("message", ex.getMessage());
        }
        return page;
    }

    private static void fill(ModelAndView page, Booking booking) {
        page.addObject("resourceName", booking.getResource() == null ? null : booking.getResource().getName());
        page.addObject("startLabel", booking.getStartedAt() == null ? null : WHEN.format(booking.getStartedAt()));
    }
}
