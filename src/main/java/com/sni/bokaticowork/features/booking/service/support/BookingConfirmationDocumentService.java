package com.sni.bokaticowork.features.booking.service.support;

import org.springframework.beans.factory.annotation.Autowired;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.core.generator.qrcode.QrCodeRenderer;
import com.sni.bokaticowork.core.identity.OperatorIdentityService;
import com.sni.bokaticowork.core.utils.BrandLogo;
import com.sni.bokaticowork.core.utils.pdf.PdfRenderer;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.billing.service.support.BillingTaxRuleResolver;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

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

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final BookingRepository bookingRepository;
    private final SpringTemplateEngine templateEngine;
    private final Locale appLocale;
    private final BillingTaxRuleResolver taxRuleResolver;
    private final QrCodeRenderer qrCodeRenderer;
    private final OperatorIdentityService operatorIdentity;
    private final BrandLogo brandLogo;

    @Value("${app.api-base-url:https://api.elleaose.com}")
    private String apiBaseUrl;

    private PlatformSettings platformSettings;

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    private static final String API_BASE_URL_KEY = "app.api-base-url";

    private String apiBaseUrl() {
        return platformSettings == null ? apiBaseUrl : platformSettings.text(API_BASE_URL_KEY, apiBaseUrl);
    }

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
        model.addAttribute("status", booking.getStatus() == null ? "" : booking.getStatus().name());
        model.addAttribute("bookingDate", formatDate(booking.getStartedAt()));
        model.addAttribute("startTime", formatTime(booking.getStartedAt()));
        model.addAttribute("endTime", formatTime(booking.getEndedAt()));
        model.addAttribute("durationLabel", formatDuration(booking.getDurationMinutes()));
        model.addAttribute("quantity", booking.getQuantity());
        model.addAttribute("seatsLabel", BookingSeats.of(booking).label());
        model.addAttribute("address", resolveLocationLabel(booking));
        model.addAttribute("city", resolveZone(booking));
        BookingTaxBreakdown taxes = BookingTaxBreakdown.of(booking.getTotalAmount(), taxRuleResolver.defaultTaxProfile());
        model.addAttribute("priceTTC", money(booking.getTotalAmount(), booking.getCurrency()));
        model.addAttribute("priceHT", money(taxes.base(), booking.getCurrency()));
        model.addAttribute("priceTVA", money(taxes.vat(), booking.getCurrency()));
        model.addAttribute("priceCentimeAdditionnel", money(taxes.additionalCent(), booking.getCurrency()));
        model.addAttribute("tvaPct", taxes.vatPct());
        model.addAttribute("centimeAdditionnelPct", taxes.additionalCentPct());
        model.addAttribute("paymentMode", booking.getPaymentMode() == null ? "" : booking.getPaymentMode().name());
        model.addAttribute("verificationCode", booking.getCheckInToken());
        model.addAttribute("scanUrl", scanUrl(booking));
        model.addAttribute("viewUrl", publicBookingViewUrl(booking));
        model.addAttribute("pdfUrl", publicBookingPdfUrl(booking));
    }

    public byte[] confirmationPdf(String bookingNumber, String token) {
        Booking booking = publicBooking(bookingNumber, token);
        Context ctx = new Context(appLocale);
        ctx.setVariable("generatedAt", DateTimeFormatter.ofPattern("dd/MM/yyyy").format(LocalDate.now()));
        ctx.setVariable("operator", operatorIdentity.current());
        ctx.setVariable("logo", brandLogo.base64());
        ctx.setVariable("bookingNumber", booking.getBookingNumber());
        ctx.setVariable("recipientName", valueOrDefault(booking.getContactName(), "client"));
        ctx.setVariable("resourceName", resourceName(booking));
        ctx.setVariable("resourceType", resourceType(booking));
        ctx.setVariable("status", booking.getStatus() == null ? "" : booking.getStatus().name());
        ctx.setVariable("bookingDate", formatDate(booking.getStartedAt()));
        ctx.setVariable("startTime", formatTime(booking.getStartedAt()));
        ctx.setVariable("endTime", formatTime(booking.getEndedAt()));
        ctx.setVariable("durationLabel", formatDuration(booking.getDurationMinutes()));
        ctx.setVariable("quantity", booking.getQuantity());
        ctx.setVariable("seatsLabel", BookingSeats.of(booking).label());
        ctx.setVariable("address", resolveLocationLabel(booking));
        ctx.setVariable("city", resolveZone(booking));
        BookingTaxBreakdown taxes = BookingTaxBreakdown.of(booking.getTotalAmount(), taxRuleResolver.defaultTaxProfile());
        ctx.setVariable("priceTTC", money(booking.getTotalAmount(), booking.getCurrency()));
        ctx.setVariable("priceHT", money(taxes.base(), booking.getCurrency()));
        ctx.setVariable("priceTVA", money(taxes.vat(), booking.getCurrency()));
        ctx.setVariable("priceCentimeAdditionnel", money(taxes.additionalCent(), booking.getCurrency()));
        ctx.setVariable("tvaPct", taxes.vatPct());
        ctx.setVariable("centimeAdditionnelPct", taxes.additionalCentPct());
        ctx.setVariable("paymentMode", booking.getPaymentMode() == null ? "" : booking.getPaymentMode().name());
        ctx.setVariable("verificationCode", booking.getCheckInToken());
        ctx.setVariable("qrDataUri", "data:image/png;base64," + Base64.getEncoder().encodeToString(qrBytes(booking)));
        if (templatePrinter != null) {
            com.sni.bokaticowork.features.document.template.render.DocumentData data = confirmationData(ctx, booking.getTotalAmount(), booking.getCurrency());
            java.util.Optional<byte[]> printed = templatePrinter.tryRender("BOOKING_CONFIRMATION", data);
            if (printed.isPresent()) {
                return printed.get();
            }
        }
        return renderPdf(templateEngine.process("booking/confirmation-pdf", ctx));
    }

    /** Modeles de documents (lot D5), facultatif dans les tests. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.sni.bokaticowork.features.document.template.service.DocumentTemplatePrinter templatePrinter;

    /**
     * Donnees de la confirmation de reservation (lot D5), tirees des variables du gabarit
     * d'origine. Le QR est celui de l'enregistrement a l'arrivee.
     */
    public static com.sni.bokaticowork.features.document.template.render.DocumentData confirmationData(Context ctx, java.math.BigDecimal total,
                                                                                                     String currency) {
        String number = String.valueOf(ctx.getVariable("bookingNumber"));
        com.sni.bokaticowork.features.document.template.render.DocumentData data =
                new com.sni.bokaticowork.features.document.template.render.DocumentData("BOOKING_CONFIRMATION").title("Confirmation de réservation")
                        .number(number).source(number).amount(total, currency)
                        .verify(null, (String) ctx.getVariable("qrDataUri"))
                        .fieldsFrom(ctx, "reservation", "Réservation", "bookingNumber", "client", "Au nom de", "recipientName",
                                "espace", "Espace", "resourceName", "type_espace", "Type", "resourceType", "etat", "État", "status",
                                "date", "Date", "bookingDate", "debut", "Début", "startTime", "fin", "Fin", "endTime", "duree", "Durée", "durationLabel",
                                "places", "Places", "seatsLabel", "adresse", "Adresse", "address", "ville", "Ville", "city",
                                "prix_ht", "Prix HT", "priceHT", "tva", "TVA", "priceTVA", "centimes_additionnels", "Centimes additionnels",
                                "priceCentimeAdditionnel", "prix_ttc", "Prix TTC", "priceTTC", "mode_paiement", "Paiement", "paymentMode",
                                "code_verification", "Code de vérification", "verificationCode");
        data.section("Réservation", java.util.List.of("reservation", "client", "espace", "type_espace", "etat", "places"), null)
                .section("Créneau", java.util.List.of("date", "debut", "fin", "duree", "adresse", "ville"), null)
                .section("Prix", java.util.List.of("prix_ht", "tva", "centimes_additionnels", "prix_ttc", "mode_paiement"), null)
                .section("Arrivée", java.util.List.of("code_verification"), null);
        return data;
    }

    private byte[] renderPdf(String html) {
        try {
            return PdfRenderer.render(html);
        } catch (Exception e) {
            throw new BadRequestException("Unable to generate booking confirmation PDF", e);
        }
    }

    private byte[] qrBytes(Booking booking) {
        byte[] png = qrCodeRenderer.toPng(scanUrl(booking), 260);
        if (png == null) {
            throw new BadRequestException("Unable to generate booking QR code");
        }
        return png;
    }

    private String scanUrl(Booking booking) {
        return apiBaseUrl().stripTrailing() + ApiPath.V1 + "/public/bookings/check-in/scan/" + booking.getCheckInToken();
    }

    private String publicBookingViewUrl(Booking booking) {
        return apiBaseUrl().stripTrailing()
                + ApiPath.V1
                + "/public/bookings/"
                + encode(booking.getBookingNumber())
                + "?token="
                + encode(booking.getCheckInToken());
    }

    private String publicBookingPdfUrl(Booking booking) {
        return apiBaseUrl().stripTrailing()
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
        return booking.getResource() == null ? "" : booking.getResource().getName();
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
        return dt == null ? "" : DATE_FMT.format(dt);
    }

    private String formatTime(LocalDateTime dt) {
        return dt == null ? "" : TIME_FMT.format(dt);
    }

    private String formatDuration(Integer minutes) {
        if (minutes == null || minutes <= 0) return "";
        if (minutes < 60) return minutes + " min";
        int h = minutes / 60;
        int m = minutes % 60;
        if (m == 0) return h + (h == 1 ? " heure" : " heures");
        return h + "h" + String.format("%02d", m);
    }

    private String money(BigDecimal amount, String currency) {
        if (amount == null) return "";
        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.FRENCH);
        String curr = StringUtils.hasText(currency) ? " " + currency.toUpperCase() : " XAF";
        return nf.format(amount.setScale(0, RoundingMode.HALF_UP)) + curr;
    }

    private String valueOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
