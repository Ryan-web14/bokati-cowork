package com.sni.bokaticowork.features.booking.service.support;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.enums.BookingEventType;
import com.sni.bokaticowork.features.booking.model.Booking;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEmailNotifier {

    private static final String QR_CONTENT_ID = "qr-booking";
    private static final BigDecimal TVA_RATE = new BigDecimal("0.184");
    private static final BigDecimal CENTIME_ADDITIONNEL_RATE = new BigDecimal("0.05");

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    private final DefaultEmailSender emailSender;
    private final BookingEventWriter eventWriter;
    private final TemplateEngine templateEngine;

    @Value("${app.api-base-url:https://api.elleaose.com}")
    private String apiBaseUrl;

    @Value("${app.frontend-url:https://elleaose.com}")
    private String frontendUrl;

    public void notify(Booking booking, BookingEventType eventType) {
        if (!StringUtils.hasText(booking.getContactEmail())) {
            return;
        }
        String subject = subject(eventType, booking);
        boolean templated = isTemplatedEvent(eventType);
        boolean isConfirmation = eventType == BookingEventType.BOOKING_CREATED
                || eventType == BookingEventType.BOOKING_CONFIRMED;

        String body = templated ? body(eventType, booking, isConfirmation) : plainTextBody(eventType, booking);
        eventWriter.write(booking, BookingEventType.EMAIL_QUEUED, "Email queued", subject, null);
        try {
            CompletableFuture<Boolean> future;
            if (isConfirmation && StringUtils.hasText(booking.getCheckInToken())) {
                byte[] qrBytes = generateQrBytes(booking);
                if (qrBytes != null) {
                    future = emailSender.sendHtmlEmailWithInlineImage(
                            booking.getContactEmail(), subject, body, QR_CONTENT_ID, qrBytes);
                } else {
                    future = emailSender.sendHtmlEmail(booking.getContactEmail(), subject, body);
                }
            } else if (templated) {
                future = emailSender.sendHtmlEmail(booking.getContactEmail(), subject, body);
            } else {
                future = emailSender.sendEmail(booking.getContactEmail(), subject, body);
            }
            future.thenAccept(sent -> eventWriter.write(
                    booking,
                    sent ? BookingEventType.EMAIL_SENT : BookingEventType.EMAIL_FAILED,
                    sent ? "Email sent" : "Email failed",
                    subject,
                    null
            ));
        } catch (MessagingException ex) {
            eventWriter.write(booking, BookingEventType.EMAIL_FAILED, "Email failed", subject, null);
            log.warn("Failed to queue booking email {} for {}", eventType, booking.getBookingNumber(), ex);
        }
    }

    private String body(BookingEventType eventType, Booking booking, boolean includeQr) {
        Context ctx = new Context(Locale.FRENCH);
        String resourceName = booking.getResource() == null ? "—" : booking.getResource().getName();
        String durationLabel = formatDuration(booking.getDurationMinutes());
        boolean showQr = includeQr && StringUtils.hasText(booking.getCheckInToken());

        // ── Variables communes à tous les templates ─────────────────
        ctx.setVariable("recipientName",  valueOrDefault(booking.getContactName(), "client"));
        ctx.setVariable("bookingNumber",  booking.getBookingNumber());
        ctx.setVariable("bookingRef",     booking.getBookingNumber());
        ctx.setVariable("resourceName",   resourceName);
        ctx.setVariable("spaceName",      resourceName);
        ctx.setVariable("status",         booking.getStatus() == null ? "—" : booking.getStatus().name());
        ctx.setVariable("startedAt",      formatDateTime(booking.getStartedAt()));
        ctx.setVariable("endedAt",        formatDateTime(booking.getEndedAt()));
        ctx.setVariable("quantity",       booking.getQuantity());
        ctx.setVariable("paymentMode",    booking.getPaymentMode() == null ? "—" : booking.getPaymentMode().name());
        ctx.setVariable("checkInToken",   booking.getCheckInToken());
        ctx.setVariable("verificationCode", StringUtils.hasText(booking.getCheckInToken()) ? booking.getCheckInToken() : booking.getBookingNumber());
        ctx.setVariable("showQr",         showQr);
        ctx.setVariable("reason",         eventReason(eventType, booking));
        ctx.setVariable("eventTag",       eventTag(eventType));
        ctx.setVariable("eventTitle",     eventTitle(eventType));
        ctx.setVariable("eventSubtitle",  eventSubtitle(eventType));
        ctx.setVariable("eventMessage",   eventMessage(eventType, booking));

        // ── Variables pour booking-confirmation ─────────────────────
        ctx.setVariable("bookingDate",    formatDate(booking.getStartedAt()));
        ctx.setVariable("startTime",      formatTime(booking.getStartedAt()));
        ctx.setVariable("endTime",        formatTime(booking.getEndedAt()));
        ctx.setVariable("durationLabel",  durationLabel);
        ctx.setVariable("duration",       durationLabel);
        ctx.setVariable("resourceType",   resolveResourceType(booking));
        ctx.setVariable("resourceCapacity", resolveCapacity(booking));
        ctx.setVariable("address",        resolveLocationLabel(booking));
        ctx.setVariable("city",           resolveZone(booking));
        BigDecimal taxableBase = computeTaxIncludedBase(booking.getTotalAmount());
        ctx.setVariable("priceTTC",       formatAmount(booking.getTotalAmount(), booking.getCurrency()));
        ctx.setVariable("priceHT",        formatAmount(taxableBase, booking.getCurrency()));
        ctx.setVariable("priceTVA",       formatAmount(computeTaxAmount(taxableBase, TVA_RATE), booking.getCurrency()));
        ctx.setVariable("priceCentimeAdditionnel", formatAmount(computeTaxAmount(taxableBase, CENTIME_ADDITIONNEL_RATE), booking.getCurrency()));
        ctx.setVariable("priceFees",      null);
        ctx.setVariable("tvaPct",         "18.4 %");
        ctx.setVariable("centimeAdditionnelPct", "5 %");
        ctx.setVariable("paymentMethod",  formatPaymentMode(booking.getPaymentMode()));
        ctx.setVariable("paymentLast4",   null);
        ctx.setVariable("paymentDate",    "—");
        ctx.setVariable("qrCodeUrl",      showQr ? "cid:" + QR_CONTENT_ID : null);
        ctx.setVariable("bookingUrl",     publicBookingViewUrl(booking));
        ctx.setVariable("invoiceUrl",     publicBookingPdfUrl(booking));
        ctx.setVariable("accountUrl",     frontendUrl);

        // ── CSAT pour booking-completed ─────────────────────────────
        if (eventType == BookingEventType.BOOKING_COMPLETED) {
            ctx.setVariable("csatUrl", apiBaseUrl.stripTrailing()
                    + ApiPath.V1 + "/public/bookings/csat/" + booking.getBookingNumber() + "?score=");
        }

        return templateEngine.process(template(eventType), ctx);
    }

    // ── Routing ──────────────────────────────────────────────────────

    private String template(BookingEventType eventType) {
        return switch (eventType) {
            case BOOKING_CREATED, BOOKING_CONFIRMED -> "email/booking-confirmation";
            case BOOKING_CANCELLED                  -> "email/booking-cancelled";
            case BOOKING_COMPLETED                  -> "email/booking-completed";
            default                                 -> "email/booking-event";
        };
    }

    private String subject(BookingEventType eventType, Booking booking) {
        return switch (eventType) {
            case BOOKING_CONFIRMED, BOOKING_CREATED -> "Confirmation réservation " + booking.getBookingNumber();
            case BOOKING_CANCELLED  -> "Annulation réservation " + booking.getBookingNumber();
            case BOOKING_REJECTED   -> "Réservation rejetée " + booking.getBookingNumber();
            case BOOKING_STARTED    -> "Réservation démarrée " + booking.getBookingNumber();
            case BOOKING_CHECKED_IN -> "Check-in réservation " + booking.getBookingNumber();
            case BOOKING_CHECKED_OUT -> "Check-out réservation " + booking.getBookingNumber();
            case BOOKING_COMPLETED  -> "Réservation terminée " + booking.getBookingNumber();
            case BOOKING_NO_SHOW    -> "Absence réservation " + booking.getBookingNumber();
            default -> "Mise à jour réservation " + booking.getBookingNumber();
        };
    }

    private boolean isTemplatedEvent(BookingEventType eventType) {
        return eventType == BookingEventType.BOOKING_CREATED
                || eventType == BookingEventType.BOOKING_CONFIRMED
                || eventType == BookingEventType.BOOKING_CANCELLED
                || eventType == BookingEventType.BOOKING_REJECTED
                || eventType == BookingEventType.BOOKING_STARTED
                || eventType == BookingEventType.BOOKING_CHECKED_IN
                || eventType == BookingEventType.BOOKING_CHECKED_OUT
                || eventType == BookingEventType.BOOKING_COMPLETED
                || eventType == BookingEventType.BOOKING_NO_SHOW;
    }

    // ── Labels ───────────────────────────────────────────────────────

    private String eventTag(BookingEventType eventType) {
        return switch (eventType) {
            case BOOKING_REJECTED   -> "Rejeté";
            case BOOKING_STARTED    -> "Démarré";
            case BOOKING_CHECKED_IN -> "Check-in";
            case BOOKING_CHECKED_OUT -> "Check-out";
            case BOOKING_COMPLETED  -> "Terminé";
            case BOOKING_NO_SHOW    -> "No-show";
            default -> "Réservation";
        };
    }

    private String eventTitle(BookingEventType eventType) {
        return switch (eventType) {
            case BOOKING_REJECTED   -> "Réservation rejetée";
            case BOOKING_STARTED    -> "Réservation démarrée";
            case BOOKING_CHECKED_IN -> "Check-in enregistré";
            case BOOKING_CHECKED_OUT -> "Check-out enregistré";
            case BOOKING_COMPLETED  -> "Réservation terminée";
            case BOOKING_NO_SHOW    -> "Absence enregistrée";
            default -> "Mise à jour réservation";
        };
    }

    private String eventSubtitle(BookingEventType eventType) {
        return switch (eventType) {
            case BOOKING_REJECTED   -> "La demande de réservation n'a pas été approuvée";
            case BOOKING_STARTED    -> "Le créneau réservé est maintenant en cours";
            case BOOKING_CHECKED_IN -> "La présence a été confirmée à l'arrivée";
            case BOOKING_CHECKED_OUT -> "La sortie a été confirmée";
            case BOOKING_COMPLETED  -> "Le créneau réservé est clôturé";
            case BOOKING_NO_SHOW    -> "La réservation a été marquée comme absence";
            default -> "Une mise à jour a été enregistrée";
        };
    }

    private String eventMessage(BookingEventType eventType, Booking booking) {
        String n = booking.getBookingNumber();
        return switch (eventType) {
            case BOOKING_REJECTED   -> "Votre réservation " + n + " a été rejetée.";
            case BOOKING_STARTED    -> "Votre réservation " + n + " a démarré.";
            case BOOKING_CHECKED_IN -> "Le check-in de la réservation " + n + " a été enregistré.";
            case BOOKING_CHECKED_OUT -> "Le check-out de la réservation " + n + " a été enregistré.";
            case BOOKING_COMPLETED  -> "Votre réservation " + n + " est maintenant terminée.";
            case BOOKING_NO_SHOW    -> "Votre réservation " + n + " a été marquée comme absence.";
            default -> "Une mise à jour a été enregistrée sur votre réservation " + n + ".";
        };
    }

    private String eventReason(BookingEventType eventType, Booking booking) {
        return switch (eventType) {
            case BOOKING_CANCELLED -> booking.getCancellationReason();
            case BOOKING_REJECTED  -> booking.getRejectionReason();
            default -> null;
        };
    }

    // ── Formatters ───────────────────────────────────────────────────

    private String formatDate(LocalDateTime dt) {
        if (dt == null) return "—";
        return DATE_FMT.format(dt);
    }

    private String formatTime(LocalDateTime dt) {
        if (dt == null) return "—";
        return TIME_FMT.format(dt);
    }

    private String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "—";
        return DATE_FMT.format(dt) + " à " + TIME_FMT.format(dt);
    }

    private String formatDuration(Integer minutes) {
        if (minutes == null || minutes <= 0) return "—";
        if (minutes < 60) return minutes + " min";
        int h = minutes / 60;
        int m = minutes % 60;
        if (m == 0) return h + (h == 1 ? " heure" : " heures");
        return h + "h" + String.format("%02d", m);
    }

    private String formatAmount(BigDecimal amount, String currency) {
        if (amount == null) return "—";
        NumberFormat fmt = NumberFormat.getIntegerInstance(Locale.FRENCH);
        String curr = StringUtils.hasText(currency) ? " " + currency.toUpperCase() : " XAF";
        return fmt.format(amount.setScale(0, java.math.RoundingMode.HALF_UP)) + curr;
    }

    private BigDecimal computeTaxIncludedBase(BigDecimal totalAmount) {
        if (totalAmount == null) return null;
        BigDecimal divisor = BigDecimal.ONE.add(TVA_RATE).add(CENTIME_ADDITIONNEL_RATE);
        return totalAmount.divide(divisor, 8, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal computeTaxAmount(BigDecimal taxableBase, BigDecimal rate) {
        if (taxableBase == null || rate == null) return null;
        return taxableBase.multiply(rate);
    }

    private String formatPaymentMode(com.sni.bokaticowork.features.booking.enums.BookingPaymentMode mode) {
        if (mode == null) return "—";
        return switch (mode) {
            case DIRECT       -> "Paiement direct";
            case SUBSCRIPTION -> "Abonnement";
            case PASS         -> "Pass";
            default           -> mode.name();
        };
    }

    private String resolveResourceType(Booking booking) {
        try {
            if (booking.getResource() == null) return null;
            var type = booking.getResource().getResourceType();
            return type != null ? type.getName() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer resolveCapacity(Booking booking) {
        try {
            return booking.getResource() != null ? booking.getResource().getCapacity() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private String resolveLocationLabel(Booking booking) {
        try {
            if (booking.getResource() == null || !StringUtils.hasText(booking.getResource().getLocationLabel())) {
                return "84 Boulevard du Général de Gaulle";
            }
            return booking.getResource().getLocationLabel();
        } catch (Exception ex) {
            return "84 Boulevard du Général de Gaulle";
        }
    }

    private String resolveZone(Booking booking) {
        try {
            if (booking.getResource() == null || !StringUtils.hasText(booking.getResource().getZone())) {
                return "Espace de coworking";
            }
            return booking.getResource().getZone();
        } catch (Exception ex) {
            return "Espace de coworking";
        }
    }

    // ── QR code ──────────────────────────────────────────────────────

    private byte[] generateQrBytes(Booking booking) {
        if (!StringUtils.hasText(booking.getCheckInToken())) return null;
        String scanUrl = apiBaseUrl.stripTrailing()
                + ApiPath.V1 + "/public/bookings/check-in/scan/"
                + booking.getCheckInToken();
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(scanUrl, BarcodeFormat.QR_CODE, 280, 280);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(MatrixToImageWriter.toBufferedImage(matrix), "png", output);
            return output.toByteArray();
        } catch (Exception ex) {
            log.warn("Failed to generate booking QR code for {}", booking.getBookingNumber(), ex);
            return null;
        }
    }

    private String publicBookingViewUrl(Booking booking) {
        return publicBookingBaseUrl(booking);
    }

    private String publicBookingPdfUrl(Booking booking) {
        return apiBaseUrl.stripTrailing()
                + ApiPath.V1
                + "/public/bookings/"
                + encode(booking.getBookingNumber())
                + "/confirmation.pdf?token="
                + encode(booking.getCheckInToken());
    }

    private String publicBookingBaseUrl(Booking booking) {
        return apiBaseUrl.stripTrailing()
                + ApiPath.V1
                + "/public/bookings/"
                + encode(booking.getBookingNumber())
                + "?token="
                + encode(booking.getCheckInToken());
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    // ── Plain text fallback ───────────────────────────────────────────

    private String plainTextBody(BookingEventType eventType, Booking booking) {
        return """
                Réservation : %s
                Ressource   : %s
                Statut      : %s
                Début       : %s
                Fin         : %s
                Événement   : %s
                """.formatted(
                booking.getBookingNumber(),
                booking.getResource() == null ? "—" : booking.getResource().getName(),
                booking.getStatus(),
                formatDateTime(booking.getStartedAt()),
                formatDateTime(booking.getEndedAt()),
                eventType
        );
    }

    private String valueOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
