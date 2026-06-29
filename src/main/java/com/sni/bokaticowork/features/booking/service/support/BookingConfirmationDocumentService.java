package com.sni.bokaticowork.features.booking.service.support;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingConfirmationDocumentService {

    private static final BigDecimal TVA_RATE = new BigDecimal("0.184");
    private static final BigDecimal CENTIME_ADDITIONNEL_RATE = new BigDecimal("0.05");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final BookingRepository bookingRepository;
    private final SpringTemplateEngine templateEngine;
    private final Locale appLocale;

    @Value("${app.api-base-url:https://api.elleaose.com}")
    private String apiBaseUrl;

    public Booking publicBooking(String bookingNumber, String token) {
        if (!StringUtils.hasText(bookingNumber) || !StringUtils.hasText(token)) {
            throw new BadRequestException("Booking number and token are required");
        }
        Booking booking = bookingRepository.findPublicByBookingNumberWithResource(bookingNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Booking " + bookingNumber + " not found"));
        if (!StringUtils.hasText(booking.getCheckInToken()) || !booking.getCheckInToken().equals(token.trim())) {
            throw new ResourceNotFoundException("Booking public access token not found");
        }
        return booking;
    }

    public void fillViewModel(org.springframework.ui.ModelMap model, Booking booking) {
        model.addAttribute("booking", booking);
        model.addAttribute("bookingNumber", booking.getBookingNumber());
        model.addAttribute("recipientName", valueOrDefault(booking.getContactName(), "client"));
        model.addAttribute("resourceName", resourceName(booking));
        model.addAttribute("resourceType", resourceType(booking));
        model.addAttribute("status", booking.getStatus() == null ? "—" : booking.getStatus().name());
        model.addAttribute("bookingDate", formatDate(booking.getStartedAt()));
        model.addAttribute("startTime", formatTime(booking.getStartedAt()));
        model.addAttribute("endTime", formatTime(booking.getEndedAt()));
        model.addAttribute("durationLabel", formatDuration(booking.getDurationMinutes()));
        model.addAttribute("quantity", booking.getQuantity());
        model.addAttribute("address", resolveLocationLabel(booking));
        model.addAttribute("city", resolveZone(booking));
        BigDecimal taxableBase = computeTaxIncludedBase(booking.getTotalAmount());
        model.addAttribute("priceTTC", money(booking.getTotalAmount(), booking.getCurrency()));
        model.addAttribute("priceHT", money(taxableBase, booking.getCurrency()));
        model.addAttribute("priceTVA", money(computeTaxAmount(taxableBase, TVA_RATE), booking.getCurrency()));
        model.addAttribute("priceCentimeAdditionnel", money(computeTaxAmount(taxableBase, CENTIME_ADDITIONNEL_RATE), booking.getCurrency()));
        model.addAttribute("tvaPct", "18.4 %");
        model.addAttribute("centimeAdditionnelPct", "5 %");
        model.addAttribute("paymentMode", booking.getPaymentMode() == null ? "—" : booking.getPaymentMode().name());
        model.addAttribute("verificationCode", booking.getCheckInToken());
        model.addAttribute("scanUrl", scanUrl(booking));
        model.addAttribute("viewUrl", publicBookingViewUrl(booking));
        model.addAttribute("pdfUrl", publicBookingPdfUrl(booking));
    }

    public byte[] confirmationPdf(String bookingNumber, String token) {
        Booking booking = publicBooking(bookingNumber, token);
        Context ctx = new Context(appLocale);
        ctx.setVariable("generatedAt", LocalDate.now());
        ctx.setVariable("bookingNumber", booking.getBookingNumber());
        ctx.setVariable("recipientName", valueOrDefault(booking.getContactName(), "client"));
        ctx.setVariable("resourceName", resourceName(booking));
        ctx.setVariable("resourceType", resourceType(booking));
        ctx.setVariable("status", booking.getStatus() == null ? "—" : booking.getStatus().name());
        ctx.setVariable("bookingDate", formatDate(booking.getStartedAt()));
        ctx.setVariable("startTime", formatTime(booking.getStartedAt()));
        ctx.setVariable("endTime", formatTime(booking.getEndedAt()));
        ctx.setVariable("durationLabel", formatDuration(booking.getDurationMinutes()));
        ctx.setVariable("quantity", booking.getQuantity());
        ctx.setVariable("address", resolveLocationLabel(booking));
        ctx.setVariable("city", resolveZone(booking));
        BigDecimal taxableBase = computeTaxIncludedBase(booking.getTotalAmount());
        ctx.setVariable("priceTTC", money(booking.getTotalAmount(), booking.getCurrency()));
        ctx.setVariable("priceHT", money(taxableBase, booking.getCurrency()));
        ctx.setVariable("priceTVA", money(computeTaxAmount(taxableBase, TVA_RATE), booking.getCurrency()));
        ctx.setVariable("priceCentimeAdditionnel", money(computeTaxAmount(taxableBase, CENTIME_ADDITIONNEL_RATE), booking.getCurrency()));
        ctx.setVariable("tvaPct", "18.4 %");
        ctx.setVariable("centimeAdditionnelPct", "5 %");
        ctx.setVariable("paymentMode", booking.getPaymentMode() == null ? "—" : booking.getPaymentMode().name());
        ctx.setVariable("verificationCode", booking.getCheckInToken());
        ctx.setVariable("qrDataUri", "data:image/png;base64," + Base64.getEncoder().encodeToString(qrBytes(booking)));
        return renderPdf(templateEngine.process("booking/confirmation-pdf", ctx));
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html);
            doc.outputSettings()
                    .syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml)
                    .escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml)
                    .charset(java.nio.charset.StandardCharsets.UTF_8)
                    .prettyPrint(false);
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withW3cDocument(new org.jsoup.helper.W3CDom().fromJsoup(doc), null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BadRequestException("Unable to generate booking confirmation PDF", e);
        }
    }

    private byte[] qrBytes(Booking booking) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(scanUrl(booking), BarcodeFormat.QR_CODE, 260, 260);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(MatrixToImageWriter.toBufferedImage(matrix), "png", output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new BadRequestException("Unable to generate booking QR code", e);
        }
    }

    private String scanUrl(Booking booking) {
        return apiBaseUrl.stripTrailing() + ApiPath.V1 + "/public/bookings/check-in/scan/" + booking.getCheckInToken();
    }

    private String publicBookingViewUrl(Booking booking) {
        return apiBaseUrl.stripTrailing()
                + ApiPath.V1
                + "/public/bookings/"
                + encode(booking.getBookingNumber())
                + "?token="
                + encode(booking.getCheckInToken());
    }

    private String publicBookingPdfUrl(Booking booking) {
        return apiBaseUrl.stripTrailing()
                + ApiPath.V1
                + "/public/bookings/"
                + encode(booking.getBookingNumber())
                + "/confirmation.pdf?token="
                + encode(booking.getCheckInToken());
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String resourceName(Booking booking) {
        return booking.getResource() == null ? "—" : booking.getResource().getName();
    }

    private String resourceType(Booking booking) {
        try {
            return booking.getResource() != null && booking.getResource().getResourceType() != null
                    ? booking.getResource().getResourceType().getName()
                    : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveLocationLabel(Booking booking) {
        try {
            if (booking.getResource() != null && StringUtils.hasText(booking.getResource().getLocationLabel())) {
                return booking.getResource().getLocationLabel();
            }
        } catch (Exception ignored) {
        }
        return "84 Boulevard du Général de Gaulle";
    }

    private String resolveZone(Booking booking) {
        try {
            if (booking.getResource() != null && StringUtils.hasText(booking.getResource().getZone())) {
                return booking.getResource().getZone();
            }
        } catch (Exception ignored) {
        }
        return "Espace de coworking";
    }

    private String formatDate(LocalDateTime dt) {
        return dt == null ? "—" : DATE_FMT.format(dt);
    }

    private String formatTime(LocalDateTime dt) {
        return dt == null ? "—" : TIME_FMT.format(dt);
    }

    private String formatDuration(Integer minutes) {
        if (minutes == null || minutes <= 0) return "—";
        if (minutes < 60) return minutes + " min";
        int h = minutes / 60;
        int m = minutes % 60;
        if (m == 0) return h + (h == 1 ? " heure" : " heures");
        return h + "h" + String.format("%02d", m);
    }

    private String money(BigDecimal amount, String currency) {
        if (amount == null) return "—";
        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.FRENCH);
        String curr = StringUtils.hasText(currency) ? " " + currency.toUpperCase() : " XAF";
        return nf.format(amount.setScale(0, RoundingMode.HALF_UP)) + curr;
    }

    private BigDecimal computeTaxIncludedBase(BigDecimal totalAmount) {
        if (totalAmount == null) return null;
        BigDecimal divisor = BigDecimal.ONE.add(TVA_RATE).add(CENTIME_ADDITIONNEL_RATE);
        return totalAmount.divide(divisor, 8, RoundingMode.HALF_UP);
    }

    private BigDecimal computeTaxAmount(BigDecimal taxableBase, BigDecimal rate) {
        if (taxableBase == null || rate == null) return null;
        return taxableBase.multiply(rate);
    }

    private String valueOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
