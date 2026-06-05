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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEmailNotifier {

    private static final String QR_CONTENT_ID = "qr-booking";

    private final DefaultEmailSender emailSender;
    private final BookingEventWriter eventWriter;
    private final TemplateEngine templateEngine;

    @Value("${app.api-base-url:https://1612-102-129-68-124.ngrok-free.app}")
    private String apiBaseUrl;

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

    private String subject(BookingEventType eventType, Booking booking) {
        return switch (eventType) {
            case BOOKING_CONFIRMED, BOOKING_CREATED -> "Confirmation reservation " + booking.getBookingNumber();
            case BOOKING_CANCELLED -> "Annulation reservation " + booking.getBookingNumber();
            case BOOKING_REJECTED -> "Reservation rejetee " + booking.getBookingNumber();
            case BOOKING_STARTED -> "Reservation demarree " + booking.getBookingNumber();
            case BOOKING_CHECKED_IN -> "Check-in reservation " + booking.getBookingNumber();
            case BOOKING_CHECKED_OUT -> "Check-out reservation " + booking.getBookingNumber();
            case BOOKING_COMPLETED -> "Reservation terminee " + booking.getBookingNumber();
            case BOOKING_NO_SHOW -> "Absence reservation " + booking.getBookingNumber();
            default -> "Mise a jour reservation " + booking.getBookingNumber();
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

    private String body(BookingEventType eventType, Booking booking, boolean includeQr) {
        Context context = new Context(Locale.FRENCH);
        context.setVariable("recipientName", valueOrDefault(booking.getContactName(), "client"));
        context.setVariable("bookingNumber", booking.getBookingNumber());
        context.setVariable("resourceName", booking.getResource() == null ? "-" : booking.getResource().getName());
        context.setVariable("status", booking.getStatus() == null ? "-" : booking.getStatus().name());
        context.setVariable("startedAt", booking.getStartedAt());
        context.setVariable("endedAt", booking.getEndedAt());
        context.setVariable("quantity", booking.getQuantity());
        context.setVariable("paymentMode", booking.getPaymentMode() == null ? "-" : booking.getPaymentMode().name());
        context.setVariable("checkInToken", booking.getCheckInToken());
        context.setVariable("showQr", includeQr && StringUtils.hasText(booking.getCheckInToken()));
        context.setVariable("reason", eventReason(eventType, booking));
        context.setVariable("eventTag", eventTag(eventType));
        context.setVariable("eventTitle", eventTitle(eventType));
        context.setVariable("eventSubtitle", eventSubtitle(eventType));
        context.setVariable("eventMessage", eventMessage(eventType, booking));
        if (eventType == BookingEventType.BOOKING_COMPLETED) {
            context.setVariable("csatUrl", apiBaseUrl.stripTrailing()
                    + ApiPath.V1 + "/public/bookings/csat/" + booking.getBookingNumber() + "?score=");
        }
        String template = template(eventType);
        return templateEngine.process(template, context);
    }

    private String template(BookingEventType eventType) {
        return switch (eventType) {
            case BOOKING_CREATED, BOOKING_CONFIRMED -> "email/booking-confirmation";
            case BOOKING_CANCELLED -> "email/booking-cancelled";
            case BOOKING_COMPLETED -> "email/booking-completed";
            default -> "email/booking-event";
        };
    }

    private String eventTag(BookingEventType eventType) {
        return switch (eventType) {
            case BOOKING_REJECTED -> "Rejet";
            case BOOKING_STARTED -> "Demarrage";
            case BOOKING_CHECKED_IN -> "Check-in";
            case BOOKING_CHECKED_OUT -> "Check-out";
            case BOOKING_COMPLETED -> "Terminee";
            case BOOKING_NO_SHOW -> "No-show";
            default -> "Reservation";
        };
    }

    private String eventTitle(BookingEventType eventType) {
        return switch (eventType) {
            case BOOKING_REJECTED -> "Reservation rejetee";
            case BOOKING_STARTED -> "Reservation demarree";
            case BOOKING_CHECKED_IN -> "Check-in enregistre";
            case BOOKING_CHECKED_OUT -> "Check-out enregistre";
            case BOOKING_COMPLETED -> "Reservation terminee";
            case BOOKING_NO_SHOW -> "Absence enregistree";
            default -> "Mise a jour reservation";
        };
    }

    private String eventSubtitle(BookingEventType eventType) {
        return switch (eventType) {
            case BOOKING_REJECTED -> "La demande de reservation n'a pas ete approuvee";
            case BOOKING_STARTED -> "Le creneau reserve est maintenant en cours";
            case BOOKING_CHECKED_IN -> "La presence a ete confirmee a l'arrivee";
            case BOOKING_CHECKED_OUT -> "La sortie a ete confirmee";
            case BOOKING_COMPLETED -> "Le creneau reserve est cloture";
            case BOOKING_NO_SHOW -> "La reservation a ete marquee comme absence";
            default -> "Une mise a jour a ete enregistree";
        };
    }

    private String eventMessage(BookingEventType eventType, Booking booking) {
        String bookingNumber = booking.getBookingNumber();
        return switch (eventType) {
            case BOOKING_REJECTED -> "Votre reservation " + bookingNumber + " a ete rejetee.";
            case BOOKING_STARTED -> "Votre reservation " + bookingNumber + " a demarre.";
            case BOOKING_CHECKED_IN -> "Le check-in de la reservation " + bookingNumber + " a ete enregistre.";
            case BOOKING_CHECKED_OUT -> "Le check-out de la reservation " + bookingNumber + " a ete enregistre.";
            case BOOKING_COMPLETED -> "Votre reservation " + bookingNumber + " est maintenant terminee.";
            case BOOKING_NO_SHOW -> "Votre reservation " + bookingNumber + " a ete marquee comme absence.";
            default -> "Une mise a jour a ete enregistree sur votre reservation " + bookingNumber + ".";
        };
    }

    private String eventReason(BookingEventType eventType, Booking booking) {
        return switch (eventType) {
            case BOOKING_CANCELLED -> booking.getCancellationReason();
            case BOOKING_REJECTED -> booking.getRejectionReason();
            default -> null;
        };
    }

    private String plainTextBody(BookingEventType eventType, Booking booking) {
        return """
                Booking: %s
                Resource: %s
                Status: %s
                Start: %s
                End: %s
                Event: %s
                """.formatted(
                booking.getBookingNumber(),
                booking.getResource() == null ? "-" : booking.getResource().getName(),
                booking.getStatus(),
                booking.getStartedAt(),
                booking.getEndedAt(),
                eventType
        );
    }

    private byte[] generateQrBytes(Booking booking) {
        if (!StringUtils.hasText(booking.getCheckInToken())) {
            return null;
        }
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

    private String valueOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
