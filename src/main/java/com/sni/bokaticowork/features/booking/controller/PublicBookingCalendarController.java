package com.sni.bokaticowork.features.booking.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.service.support.BookingCalendarFeed;
import com.sni.bokaticowork.features.booking.service.support.BookingReviews;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

/** Lot B9g : agenda synchronise d'un membre (ICS a jeton) et commentaire de l'avis apres utilisation. */
@RestController
@RequestMapping(ApiPath.V1 + "/public/bookings")
@RequiredArgsConstructor
public class PublicBookingCalendarController {

    private final BookingCalendarFeed feed;
    private final BookingReviews reviews;

    @GetMapping(value = "/calendar/{token}.ics")
    public ResponseEntity<String> calendar(@PathVariable String token) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
                .body(feed.calendar(token));
    }

    /** Commentaire de l'avis, envoye depuis la page de remerciement (jeton de la reservation). */
    @PostMapping("/csat/{bookingNumber}/comment")
    public ModelAndView comment(@PathVariable String bookingNumber, @RequestParam(required = false) String token,
                                @RequestParam(required = false) String comment) {
        ModelAndView page = new ModelAndView("booking/csat-thanks");
        page.addObject("bookingNumber", bookingNumber);
        try {
            page.addObject("score", reviews.comment(bookingNumber, token, comment));
            page.addObject("commented", true);
        } catch (RuntimeException ex) {
            page.addObject("score", 0);
        }
        return page;
    }
}
