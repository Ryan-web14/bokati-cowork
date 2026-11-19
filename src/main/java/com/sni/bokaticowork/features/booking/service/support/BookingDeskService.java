package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.utils.time.AppTime;
import com.sni.bokaticowork.features.booking.dto.request.BookingRescheduleRequest;
import com.sni.bokaticowork.features.booking.dto.request.BookingStatusChangeRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingResponse;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentStatus;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.mapper.interfaces.BookingMapper;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lot B9b : planning d'accueil du jour et series recurrentes.
 *
 * <p>Planning : toutes les reservations du jour par ressource, avec arrivee, retard (debut passe
 * sans arrivee), reglement et rendez-vous a regler sur place. Series : liste d'une serie, annulation
 * ou deplacement en bloc a partir d'une date ; chaque occurrence suit ses regles (politique,
 * capacite, prix), une occurrence refusee est rapportee sans arreter les autres.</p>
 */
@Component
@RequiredArgsConstructor
public class BookingDeskService {

    private static final Set<BookingStatus> ON_PLANNING = Set.of(BookingStatus.PENDING_APPROVAL, BookingStatus.PENDING_PAYMENT,
            BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED, BookingStatus.NO_SHOW);
    private static final Set<BookingStatus> CHANGEABLE = Set.of(BookingStatus.PENDING_APPROVAL, BookingStatus.PENDING_PAYMENT,
            BookingStatus.CONFIRMED, BookingStatus.DRAFT);

    public record PlanningItem(String bookingNumber, String contactName, String contactPhone, LocalDateTime startedAt, LocalDateTime endedAt,
                               BookingStatus status, String statusLabel, java.time.Instant checkedInAt, boolean late, long lateMinutes,
                               BookingPaymentStatus paymentStatus, boolean payOnSite, String attendanceAnswer, Integer quantity) {
    }

    public record PlanningResource(String resourceCode, String resourceName, List<PlanningItem> bookings) {
    }

    public record Planning(LocalDate date, int bookings, int arrived, int late, int toCollect, List<PlanningResource> resources) {
    }

    public record SeriesOutcome(String recurrenceGroupNumber, int changed, int skipped, List<String> changedBookings, Map<String, String> refused) {
    }

    private final BookingRepository bookings;
    private final ObjectProvider<BookingService> bookingService;
    private final BookingMapper mapper;

    @Transactional(readOnly = true)
    public Planning planning(LocalDate date, String resourceCode) {
        LocalDate day = date == null ? AppTime.today() : date;
        LocalDateTime now = AppTime.now();
        Map<String, PlanningResource> byResource = new LinkedHashMap<>();
        int arrived = 0;
        int late = 0;
        int toCollect = 0;
        int count = 0;
        for (Booking booking : bookings.findForDay(day.atStartOfDay(), day.plusDays(1).atStartOfDay())) {
            if (!ON_PLANNING.contains(booking.getStatus())) {
                continue;
            }
            if (resourceCode != null && !resourceCode.isBlank() && !booking.getResource().getCode().equalsIgnoreCase(resourceCode.trim())) {
                continue;
            }
            boolean isLate = booking.getCheckedInAt() == null && booking.getStatus() == BookingStatus.CONFIRMED && booking.getStartedAt().isBefore(now);
            long lateMinutes = isLate ? Duration.between(booking.getStartedAt(), now).toMinutes() : 0;
            BookingPaymentStatus payment = BookingPaymentStatus.of(booking.getPaymentMode(), booking.getStatus(), booking.getPaidAt());
            PlanningItem item = new PlanningItem(booking.getBookingNumber(), booking.getContactName(), booking.getContactPhone(),
                    booking.getStartedAt(), booking.getEndedAt(), booking.getStatus(), booking.getStatus().label(), booking.getCheckedInAt(),
                    isLate, lateMinutes, payment, Boolean.TRUE.equals(booking.getPayOnSite()), booking.getAttendanceAnswer(), booking.getQuantity());
            byResource.computeIfAbsent(booking.getResource().getCode(),
                    code -> new PlanningResource(code, booking.getResource().getName(), new ArrayList<>())).bookings().add(item);
            count++;
            if (booking.getCheckedInAt() != null) {
                arrived++;
            }
            if (isLate) {
                late++;
            }
            if (payment == BookingPaymentStatus.PENDING) {
                toCollect++;
            }
        }
        return new Planning(day, count, arrived, late, toCollect, List.copyOf(byResource.values()));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> series(String groupNumber) {
        List<Booking> found = bookings.findByRecurrenceGroupNumberOrderByStartedAtAsc(groupNumber);
        if (found.isEmpty()) {
            throw new ResourceNotFoundException("Série introuvable : " + groupNumber);
        }
        return mapper.toListResponses(found);
    }

    /** Annule les occurrences de la serie a partir d'une date (toutes a venir si vide). */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SeriesOutcome cancelSeries(String groupNumber, LocalDate from, String reason) {
        List<String> changed = new ArrayList<>();
        Map<String, String> refused = new LinkedHashMap<>();
        int skipped = 0;
        for (Booking booking : occurrences(groupNumber, from)) {
            if (!CHANGEABLE.contains(booking.getStatus())) {
                skipped++;
                continue;
            }
            try {
                bookingService.getObject().cancel(booking.getBookingNumber(), new BookingStatusChangeRequest(null,
                        reason == null || reason.isBlank() ? "Série annulée" : reason.trim(), Boolean.TRUE));
                changed.add(booking.getBookingNumber());
            } catch (RuntimeException ex) {
                refused.put(booking.getBookingNumber(), ex.getMessage());
            }
        }
        return new SeriesOutcome(groupNumber, changed.size(), skipped, changed, refused);
    }

    /**
     * Deplace les occurrences a partir d'une date : nouvel horaire le meme jour ({@code newStart},
     * {@code newEnd}) ou decalage en jours ({@code shiftDays}). Une hausse de prix n'est acceptee
     * que si {@code acceptPriceChange}.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SeriesOutcome rescheduleSeries(String groupNumber, LocalDate from, LocalTime newStart, LocalTime newEnd, Integer shiftDays,
                                          Boolean acceptPriceChange) {
        if ((newStart == null) != (newEnd == null)) {
            throw new BadRequestException("Donnez l'heure de début et l'heure de fin, ou aucune des deux");
        }
        if (newStart == null && (shiftDays == null || shiftDays == 0)) {
            throw new BadRequestException("Indiquez un nouvel horaire ou un décalage en jours");
        }
        List<String> changed = new ArrayList<>();
        Map<String, String> refused = new LinkedHashMap<>();
        int skipped = 0;
        for (Booking booking : occurrences(groupNumber, from)) {
            if (!CHANGEABLE.contains(booking.getStatus())) {
                skipped++;
                continue;
            }
            LocalDate day = booking.getStartedAt().toLocalDate().plusDays(shiftDays == null ? 0 : shiftDays);
            LocalDateTime start = newStart == null ? day.atTime(booking.getStartedAt().toLocalTime()) : day.atTime(newStart);
            LocalDateTime end = newEnd == null ? start.plus(Duration.between(booking.getStartedAt(), booking.getEndedAt())) : day.atTime(newEnd);
            try {
                bookingService.getObject().reschedule(booking.getBookingNumber(),
                        new BookingRescheduleRequest(start, end, null, "Série déplacée", Boolean.TRUE, acceptPriceChange));
                changed.add(booking.getBookingNumber());
            } catch (RuntimeException ex) {
                refused.put(booking.getBookingNumber(), ex.getMessage());
            }
        }
        return new SeriesOutcome(groupNumber, changed.size(), skipped, changed, refused);
    }

    private List<Booking> occurrences(String groupNumber, LocalDate from) {
        List<Booking> found = bookings.findByRecurrenceGroupNumberOrderByStartedAtAsc(groupNumber);
        if (found.isEmpty()) {
            throw new ResourceNotFoundException("Série introuvable : " + groupNumber);
        }
        LocalDateTime threshold = from == null ? AppTime.now() : from.atStartOfDay();
        return found.stream().filter(booking -> !booking.getStartedAt().isBefore(threshold)).toList();
    }
}
