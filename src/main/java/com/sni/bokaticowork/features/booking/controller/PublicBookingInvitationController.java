package com.sni.bokaticowork.features.booking.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.model.BookingParticipant;
import com.sni.bokaticowork.features.booking.service.support.BookingGuests;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Lot B9d : reponse d'un invite par le lien de son invitation (jeton propre a l'invite). */
@RestController
@RequestMapping(ApiPath.V1 + "/public/bookings/invitations")
@RequiredArgsConstructor
public class PublicBookingInvitationController {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH:mm", Locale.FRENCH);

    private final BookingGuests guests;

    @GetMapping("/{token}")
    public ModelAndView answer(@PathVariable String token, @RequestParam(defaultValue = "ACCEPT") String answer) {
        ModelAndView page = new ModelAndView("booking/invitation");
        try {
            BookingParticipant participant = guests.respond(token, !"DECLINE".equalsIgnoreCase(answer));
            page.addObject("state", "DECLINE".equalsIgnoreCase(answer) ? "DECLINED" : "ACCEPTED");
            page.addObject("resourceName", participant.getBooking().getResource().getName());
            page.addObject("startLabel", WHEN.format(participant.getBooking().getStartedAt()));
            page.addObject("bookingNumber", participant.getBooking().getBookingNumber());
        } catch (RuntimeException ex) {
            page.addObject("state", "ERROR");
            page.addObject("message", ex.getMessage());
        }
        return page;
    }
}
