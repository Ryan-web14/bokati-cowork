package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.reference.service.SessionActor;
import com.sni.bokaticowork.features.booking.dto.request.BookingCallerRequest;
import com.sni.bokaticowork.features.booking.enums.BookingEventType;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.crm.dto.CrmDtos;
import com.sni.bokaticowork.features.crm.enums.LeadActivityType;
import com.sni.bokaticowork.features.crm.enums.LeadInterest;
import com.sni.bokaticowork.features.crm.enums.LeadSource;
import com.sni.bokaticowork.features.crm.enums.LeadStage;
import com.sni.bokaticowork.features.crm.model.Lead;
import com.sni.bokaticowork.features.crm.repository.LeadRepository;
import com.sni.bokaticowork.features.crm.service.interfaces.CrmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;

/**
 * Lot B5 (decision D5) : rendez-vous pris au telephone.
 *
 * <p>A la reservation : note d'appel gardee, appelant inconnu enregistre comme prospect du CRM
 * (source {@code PHONE}) ou prospect existant relie, activite « Appel » sur sa fiche. A la venue
 * (arrivee pointee) ou au paiement : le prospect est converti, rattache au titulaire de la
 * reservation. Le CRM est facultatif : sans lui, la note reste sur la reservation.</p>
 */
@Slf4j
@Component
public class BookingPhoneIntake {

    private final BookingRepository bookings;
    private final BookingEventWriter events;
    private final ObjectProvider<CrmService> crm;
    private final ObjectProvider<LeadRepository> leads;

    private SessionActor actor;

    public BookingPhoneIntake(BookingRepository bookings, BookingEventWriter events, ObjectProvider<CrmService> crm,
                              ObjectProvider<LeadRepository> leads) {
        this.bookings = bookings;
        this.events = events;
        this.crm = crm;
        this.leads = leads;
    }

    @Autowired(required = false)
    void setActor(SessionActor actor) {
        this.actor = actor;
    }

    /**
     * Apres la creation d'une reservation prise au telephone.
     *
     * @param knownOwner le titulaire est un membre, un client ou une entreprise designe (pas un invite)
     */
    public void onBooked(Booking booking, BookingCallerRequest caller, boolean knownOwner) {
        if (caller == null) {
            return;
        }
        if (StringUtils.hasText(caller.callNote())) {
            booking.setCallNote(caller.callNote().trim());
        }
        Lead lead = link(booking, caller, knownOwner);
        if (lead != null) {
            booking.setLeadNumber(lead.getLeadNumber());
        }
        bookings.save(booking);
        events.write(booking, BookingEventType.BOOKING_PHONE_CALL, "Rendez-vous pris au téléphone",
                (StringUtils.hasText(booking.getCallNote()) ? booking.getCallNote() : "Sans note d'appel")
                        + (lead == null ? "" : " · prospect " + lead.getLeadNumber()), null);
    }

    /** A la venue ou au paiement : le prospect relie devient client (rattache au titulaire). */
    public void convert(Booking booking, String moment) {
        if (booking == null || !StringUtils.hasText(booking.getLeadNumber())) {
            return;
        }
        CrmService service = crm.getIfAvailable();
        LeadRepository repository = leads.getIfAvailable();
        if (service == null || repository == null) {
            return;
        }
        try {
            Lead lead = repository.findByLeadNumber(booking.getLeadNumber()).orElse(null);
            if (lead == null || lead.getStage() == LeadStage.WON || lead.getStage() == LeadStage.LOST) {
                return;
            }
            service.addActivity(lead.getId(), new CrmDtos.AddLeadActivityRequest(LeadActivityType.VISIT,
                    moment + " : réservation " + booking.getBookingNumber(), null, by(), Instant.now()));
            service.convert(lead.getId(), new CrmDtos.ConvertLeadRequest(booking.getOwnerType() == null ? "CUSTOMER" : booking.getOwnerType().name(),
                    booking.getOwnerCode()));
            events.write(booking, BookingEventType.BOOKING_LEAD_CONVERTED, "Prospect converti",
                    "Prospect " + lead.getLeadNumber() + " converti (" + moment.toLowerCase(java.util.Locale.ROOT) + ")", null);
        } catch (RuntimeException ex) {
            log.warn("Prospect {} de la reservation {} non converti : {}", booking.getLeadNumber(), booking.getBookingNumber(), ex.getMessage());
        }
    }

    private Lead link(Booking booking, BookingCallerRequest caller, boolean knownOwner) {
        CrmService service = crm.getIfAvailable();
        LeadRepository repository = leads.getIfAvailable();
        if (service == null || repository == null) {
            return null;
        }
        try {
            Lead lead = null;
            if (StringUtils.hasText(caller.leadNumber())) {
                lead = repository.findByLeadNumber(caller.leadNumber().trim()).orElse(null);
            } else if (!knownOwner && !Boolean.FALSE.equals(caller.createLead())) {
                String name = StringUtils.hasText(caller.fullName()) ? caller.fullName().trim()
                        : StringUtils.hasText(booking.getContactName()) ? booking.getContactName() : "Appelant " + booking.getBookingNumber();
                var created = service.create(new CrmDtos.CreateLeadRequest(name,
                        first(caller.email(), booking.getContactEmail()), first(caller.phone(), booking.getContactPhone()),
                        caller.company(), "Rendez-vous " + booking.getBookingNumber() + " pris au téléphone", LeadSource.PHONE,
                        interest(booking), booking.getTotalAmount(), null, null, null));
                lead = repository.findByLeadNumber(created.leadNumber()).orElse(null);
            }
            if (lead == null) {
                return null;
            }
            service.addActivity(lead.getId(), new CrmDtos.AddLeadActivityRequest(LeadActivityType.CALL,
                    "Rendez-vous " + booking.getBookingNumber() + " (" + booking.getResource().getName() + ", " + booking.getStartedAt() + ")",
                    caller.callNote(), by(), Instant.now()));
            return lead;
        } catch (RuntimeException ex) {
            log.warn("Prospect de la reservation {} non enregistre : {}", booking.getBookingNumber(), ex.getMessage());
            return null;
        }
    }

    private static LeadInterest interest(Booking booking) {
        String type = booking.getResource() == null || booking.getResource().getResourceType() == null ? ""
                : (booking.getResource().getResourceType().getName() + " " + booking.getResource().getResourceType().getCode()).toLowerCase(java.util.Locale.ROOT);
        if (type.contains("réunion") || type.contains("reunion") || type.contains("meeting") || type.contains("salle")) {
            return LeadInterest.MEETING_ROOM;
        }
        if (type.contains("bureau") || type.contains("office")) {
            return LeadInterest.PRIVATE_OFFICE;
        }
        return LeadInterest.WORKSPACE;
    }

    private String by() {
        return actor == null ? null : actor.resolve(null);
    }

    private static String first(String a, String b) {
        return StringUtils.hasText(a) ? a.trim() : b;
    }
}
