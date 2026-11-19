package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.utils.pdf.PdfRenderer;
import com.sni.bokaticowork.core.utils.time.AppTime;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentStatus;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lot B9g : rapport de reservations d'un client, imprimable (PDF) : toutes ses reservations de la
 * periode, avec statut, duree, montant, reglement, et les totaux (heures, montant, absences).
 */
@Component
@RequiredArgsConstructor
public class BookingClientReport {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH);

    public record Row(String bookingNumber, String day, String hours, String resource, String status, String duration, String amount,
                      String payment) {
    }

    private final JdbcTemplate jdbc;
    private final com.sni.bokaticowork.features.booking.repository.BookingRepository bookings;
    private final TemplateEngine templateEngine;

    @Transactional(readOnly = true)
    public byte[] pdf(SubscriberType ownerType, String ownerCode, LocalDate from, LocalDate to) {
        if (ownerType == null || !StringUtils.hasText(ownerCode)) {
            throw new BadRequestException("Le client (ownerType, ownerCode) est requis");
        }
        LocalDate start = from == null ? AppTime.today().withDayOfMonth(1) : from;
        LocalDate end = to == null ? AppTime.today() : to;
        if (end.isBefore(start)) {
            throw new BadRequestException("La fin de la période doit être après le début");
        }
        List<String> numbers = jdbc.queryForList("""
                SELECT booking_number FROM booking
                WHERE deleted = false AND owner_type = ? AND owner_code = ? AND started_at >= ? AND started_at < ?
                ORDER BY started_at ASC LIMIT 2000
                """, String.class, ownerType.name(), ownerCode.trim(), Timestamp.valueOf(start.atStartOfDay()),
                Timestamp.valueOf(end.plusDays(1).atStartOfDay()));
        List<Row> rows = new ArrayList<>();
        long minutes = 0;
        long noShows = 0;
        BigDecimal total = BigDecimal.ZERO;
        String contact = null;
        String currency = "XAF";
        for (String number : numbers) {
            Booking booking = bookings.findByBookingNumber(number).orElse(null);
            if (booking == null) {
                continue;
            }
            contact = contact == null ? booking.getContactName() : contact;
            currency = booking.getCurrency() == null ? currency : booking.getCurrency();
            boolean counted = booking.getStatus() != BookingStatus.CANCELLED && booking.getStatus() != BookingStatus.REJECTED
                    && booking.getStatus() != BookingStatus.EXPIRED;
            if (counted) {
                minutes += booking.getDurationMinutes() == null ? 0 : booking.getDurationMinutes();
                total = total.add(booking.getTotalAmount() == null ? BigDecimal.ZERO : booking.getTotalAmount());
            }
            if (booking.getStatus() == BookingStatus.NO_SHOW) {
                noShows++;
            }
            BookingPaymentStatus payment = BookingPaymentStatus.of(booking.getPaymentMode(), booking.getStatus(), booking.getPaidAt());
            rows.add(new Row(booking.getBookingNumber(), DAY.format(booking.getStartedAt()),
                    HOUR.format(booking.getStartedAt()) + " – " + HOUR.format(booking.getEndedAt()),
                    booking.getResource() == null ? "" : booking.getResource().getName(), capitalize(booking.getStatus().label()),
                    duration(booking.getDurationMinutes()), money(booking.getTotalAmount()) + " " + currency, payment.getLabel()));
        }
        Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("ownerType", ownerType.name());
        ctx.setVariable("ownerCode", ownerCode.trim());
        ctx.setVariable("contactName", contact);
        ctx.setVariable("periodLabel", DAY.format(start) + " au " + DAY.format(end));
        ctx.setVariable("rows", rows);
        ctx.setVariable("count", rows.size());
        ctx.setVariable("hoursLabel", duration((int) minutes));
        ctx.setVariable("totalLabel", money(total) + " " + currency);
        ctx.setVariable("noShows", noShows);
        ctx.setVariable("generatedAt", DAY.format(AppTime.today()));
        return PdfRenderer.render(templateEngine.process("booking/client-report", ctx));
    }

    private static String duration(Integer minutes) {
        if (minutes == null || minutes <= 0) {
            return "0 h";
        }
        int h = minutes / 60;
        int m = minutes % 60;
        return m == 0 ? h + " h" : h + " h " + String.format("%02d", m);
    }

    private static String money(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        return new java.text.DecimalFormat("#,##0", symbols).format(amount);
    }

    private static String capitalize(String text) {
        return text == null || text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
