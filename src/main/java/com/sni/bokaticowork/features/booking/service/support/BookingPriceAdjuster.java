package com.sni.bokaticowork.features.booking.service.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.reference.service.ReferenceLists;
import com.sni.bokaticowork.core.reference.service.SessionActor;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.approval.dto.ApprovalDtos;
import com.sni.bokaticowork.features.approval.enums.ApprovalStatus;
import com.sni.bokaticowork.features.approval.model.ApprovalRequest;
import com.sni.bokaticowork.features.approval.service.ApprovalEngine;
import com.sni.bokaticowork.features.booking.dto.request.BookingApprovalRequest;
import com.sni.bokaticowork.features.booking.dto.request.BookingPriceAdjustmentRequest;
import com.sni.bokaticowork.features.booking.enums.BookingEventType;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Lot B4 (decision D4) : ajustement manuel du prix d'une reservation par l'equipe.
 *
 * <p>Motif obligatoire (liste {@code BOOKING_PRICE_ADJUSTMENT_REASON}). Le titulaire du droit
 * {@code BOOKING:PRICE_ADJUST} ajuste directement jusqu'au plafond {@code bookings.price-adjustment.max-rate}
 * (20 % par defaut) ; au-dela, ou sans ce droit, une demande part au moteur d'approbation (centre
 * {@code BOOKING}, type {@code PRICE_ADJUSTMENT}, circuit N+1 avec {@code BOOKING:PRICE_APPROVE}).
 * A la creation, la reservation attend alors l'approbation avant d'etre confirmee ; un refus la
 * laisse continuer au prix calcule. Trace : prix d'origine, remise, motif et auteur sur la
 * reservation, ligne de remise, libelle de la facture.</p>
 */
@Slf4j
@Component
public class BookingPriceAdjuster {

    public static final String CENTER = "BOOKING";
    public static final String TYPE = "PRICE_ADJUSTMENT";
    public static final String REASON_LIST = "BOOKING_PRICE_ADJUSTMENT_REASON";
    static final String MAX_RATE_KEY = "bookings.price-adjustment.max-rate";
    static final Set<String> RIGHTS = Set.of("BOOKING:PRICE_ADJUST", "BOOKING_PRICE_ADJUST", "ROLE_SUPER_ADMIN");

    public static final String APPLIED = "APPLIED";
    public static final String PENDING = "PENDING";
    public static final String REJECTED = "REJECTED";

    /** Ce que l'ajustement donnerait. */
    public record Plan(BigDecimal before, BigDecimal after, BigDecimal discount, BigDecimal rate, boolean needsApproval,
                       String reasonCode, String reasonText) {
    }

    private final BookingRepository bookings;
    private final ObjectProvider<ApprovalEngine> approvals;
    private final ObjectProvider<BookingService> bookingService;
    private final ObjectProvider<BookingExtras> extras;
    private final BookingEventWriter events;

    private ReferenceLists lists;
    private SessionActor actor;
    private PlatformSettings settings;

    public BookingPriceAdjuster(BookingRepository bookings, ObjectProvider<ApprovalEngine> approvals, ObjectProvider<BookingService> bookingService,
                                ObjectProvider<BookingExtras> extras, BookingEventWriter events) {
        this.bookings = bookings;
        this.approvals = approvals;
        this.bookingService = bookingService;
        this.extras = extras;
        this.events = events;
    }

    @Autowired(required = false)
    void setOptional(ReferenceLists lists, SessionActor actor, PlatformSettings settings) {
        this.lists = lists;
        this.actor = actor;
        this.settings = settings;
    }

    /** Calcule et controle l'ajustement sur un total ; rien n'est ecrit. */
    public Plan plan(BigDecimal total, BookingPriceAdjustmentRequest request) {
        if (request == null) {
            return null;
        }
        boolean byRate = request.discountRate() != null;
        boolean byAmount = request.newAmount() != null;
        if (byRate == byAmount) {
            throw BadRequestException.coded("AJUSTEMENT_INVALIDE", "Indiquez soit une remise en % (discountRate), soit un nouveau total (newAmount)");
        }
        BigDecimal before = total == null ? BigDecimal.ZERO : total;
        if (before.signum() <= 0) {
            throw BadRequestException.coded("AJUSTEMENT_INVALIDE", "Rien à ajuster : la réservation est gratuite ou couverte par un droit");
        }
        BigDecimal after = byRate
                ? before.subtract(before.multiply(request.discountRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                : request.newAmount().setScale(2, RoundingMode.HALF_UP);
        if (after.compareTo(before) >= 0) {
            throw BadRequestException.coded("AJUSTEMENT_INVALIDE", "Un ajustement baisse le prix : " + plain(after) + " n'est pas inférieur à " + plain(before)
                    + " (une hausse passe par l'unité ou la grille tarifaire)");
        }
        if (after.signum() < 0) {
            after = BigDecimal.ZERO;
        }
        if (!StringUtils.hasText(request.reasonCode()) && !StringUtils.hasText(request.reasonText())) {
            throw BadRequestException.coded("MOTIF_REQUIS", "Choisissez le motif de l'ajustement");
        }
        if (StringUtils.hasText(request.reasonCode()) && lists != null) {
            var value = lists.require(REASON_LIST, request.reasonCode());
            if (Boolean.TRUE.equals(value.getRequiresText()) && !StringUtils.hasText(request.reasonText())) {
                throw BadRequestException.coded("PRECISION_REQUISE", "Précisez « " + value.getLabel() + " »");
            }
        }
        BigDecimal discount = before.subtract(after);
        BigDecimal rate = discount.multiply(BigDecimal.valueOf(100)).divide(before, 2, RoundingMode.HALF_UP);
        int max = settings == null ? 20 : settings.integer(MAX_RATE_KEY, 20);
        boolean needsApproval = !hasRight() || rate.compareTo(BigDecimal.valueOf(max)) > 0;
        return new Plan(before, after, discount, rate, needsApproval, request.reasonCode(), request.reasonText());
    }

    /** Applique tout de suite (dans le plafond) : ligne de remise, montant baisse ou avoir, trace. */
    @Transactional
    public void apply(Booking booking, Plan plan, String by) {
        String reason = reasonLabel(plan, true);
        recordPlan(booking, plan, reason, by);
        booking.setPriceAdjustmentStatus(APPLIED);
        bookings.save(booking);
        extras.getObject().applyManualDiscount(booking, plan.discount(), "Prix ajusté : " + reason);
    }

    /** Au-dela du plafond : demande d'approbation ; la reservation garde son prix en attendant. */
    @Transactional
    public void requestApproval(Booking booking, Plan plan, String by) {
        ApprovalEngine engine = approvals.getIfAvailable();
        if (engine == null) {
            throw new ConflictException("Ajustement au-delà du plafond : le circuit d'approbation n'est pas disponible");
        }
        String reason = reasonLabel(plan, false);
        recordPlan(booking, plan, reason, by);
        booking.setPriceAdjustmentStatus(PENDING);
        bookings.save(booking);
        ApprovalRequest request = engine.submit(new ApprovalDtos.Submission(CENTER, TYPE, "BOOKING", booking.getBookingNumber(),
                "Ajustement du prix de " + booking.getBookingNumber() + " : -" + plain(plan.rate()) + " % (" + reason + ")",
                plan.after(), booking.getCurrency(), null, details(booking, plan, reason), null, null));
        booking.setPriceAdjustmentApprovalNumber(request.getNumber());
        bookings.save(booking);
        events.write(booking, BookingEventType.BOOKING_PRICE_ADJUSTED, "Ajustement de prix soumis à approbation",
                "-" + plain(plan.discount()) + " " + booking.getCurrency() + " (" + plain(plan.rate()) + " %), demande " + request.getNumber(), null);
        if (request.getStatus() == ApprovalStatus.APPROVED) {
            onDecision(booking.getBookingNumber(), true, "Accord immédiat du circuit");
        }
    }

    /** Ajustement d'une reservation existante, hors creation. */
    @Transactional
    public Booking adjust(String bookingNumber, BookingPriceAdjustmentRequest request) {
        Booking booking = bookings.findByBookingNumber(bookingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable : " + bookingNumber));
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED
                || booking.getStatus() == BookingStatus.EXPIRED || booking.getStatus() == BookingStatus.NO_SHOW
                || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new ConflictException("Une réservation " + booking.getStatus().label() + " ne s'ajuste plus");
        }
        if (APPLIED.equals(booking.getPriceAdjustmentStatus()) || PENDING.equals(booking.getPriceAdjustmentStatus())) {
            throw new ConflictException("Un ajustement de prix est déjà " + (APPLIED.equals(booking.getPriceAdjustmentStatus())
                    ? "appliqué" : "en attente d'approbation") + " sur cette réservation");
        }
        Plan plan = plan(booking.getTotalAmount(), request);
        String by = actor == null ? null : actor.resolve(null);
        if (plan.needsApproval()) {
            requestApproval(booking, plan, by);
        } else {
            apply(booking, plan, by);
        }
        return bookings.findByBookingNumber(bookingNumber).orElse(booking);
    }

    /**
     * Decision du circuit. Accord : remise appliquee. Refus : prix calcule garde. Dans les deux
     * cas, une reservation qui n'attendait l'approbation que pour son prix poursuit son cours.
     */
    @Transactional
    public void onDecision(String bookingNumber, boolean approved, String comment) {
        Booking booking = bookings.findByBookingNumber(bookingNumber).orElse(null);
        if (booking == null || !PENDING.equals(booking.getPriceAdjustmentStatus())) {
            return;
        }
        if (approved) {
            Plan plan = new Plan(booking.getOriginalAmount(), booking.getOriginalAmount().subtract(booking.getPriceAdjustmentAmount()),
                    booking.getPriceAdjustmentAmount(), booking.getPriceAdjustmentRate(), false, booking.getPriceAdjustmentReasonCode(), null);
            booking.setPriceAdjustmentStatus(APPLIED);
            bookings.save(booking);
            extras.getObject().applyManualDiscount(booking, plan.discount(), "Prix ajusté : " + booking.getPriceAdjustmentReason());
            if (lists != null && StringUtils.hasText(booking.getPriceAdjustmentReasonCode())) {
                try {
                    lists.describe(REASON_LIST, booking.getPriceAdjustmentReasonCode(), null);
                } catch (RuntimeException ignored) {
                    // Motif retire entre-temps : l'ajustement reste, seul le decompte manque.
                }
            }
        } else {
            booking.setPriceAdjustmentStatus(REJECTED);
            bookings.save(booking);
            events.write(booking, BookingEventType.BOOKING_PRICE_ADJUSTED, "Ajustement de prix refusé",
                    "Prix calculé maintenu" + (StringUtils.hasText(comment) ? " : " + comment : ""), null);
        }
        if (booking.getStatus() == BookingStatus.PENDING_APPROVAL && Boolean.TRUE.equals(booking.getApprovalForPriceOnly())) {
            bookingService.getObject().approve(bookingNumber, new BookingApprovalRequest(null,
                    approved ? "Ajustement de prix approuvé" : "Ajustement de prix refusé, prix calculé maintenu", Boolean.TRUE));
        }
    }

    /** L'utilisateur a-t-il le droit d'ajuster jusqu'au plafond ? */
    public static boolean hasRight() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (authority != null && RIGHTS.contains(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private void recordPlan(Booking booking, Plan plan, String reason, String by) {
        booking.setOriginalAmount(plan.before());
        booking.setPriceAdjustmentAmount(plan.discount());
        booking.setPriceAdjustmentRate(plan.rate());
        booking.setPriceAdjustmentReason(reason);
        booking.setPriceAdjustmentReasonCode(StringUtils.hasText(plan.reasonCode()) ? ReferenceLists.normalize(plan.reasonCode()) : null);
        booking.setPriceAdjustmentBy(by);
    }

    /** Libelle du motif ; l'emploi n'est compte qu'a l'application. */
    private String reasonLabel(Plan plan, boolean count) {
        if (StringUtils.hasText(plan.reasonCode()) && lists != null) {
            if (count) {
                return lists.describe(REASON_LIST, plan.reasonCode(), plan.reasonText());
            }
            String label = lists.label(REASON_LIST, plan.reasonCode());
            return StringUtils.hasText(plan.reasonText()) ? label + " : " + plan.reasonText().trim() : label;
        }
        return plan.reasonText() == null ? "" : plan.reasonText().trim();
    }

    private static String details(Booking booking, Plan plan, String reason) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("bookingNumber", booking.getBookingNumber());
        details.put("resourceCode", booking.getResource() == null ? null : booking.getResource().getCode());
        details.put("owner", booking.getOwnerType() == null ? null : booking.getOwnerType().name() + ":" + booking.getOwnerCode());
        details.put("contactName", booking.getContactName());
        details.put("startedAt", booking.getStartedAt() == null ? null : booking.getStartedAt().toString());
        details.put("currency", booking.getCurrency());
        details.put("amountBefore", plan.before());
        details.put("amountAfter", plan.after());
        details.put("discount", plan.discount());
        details.put("rate", plan.rate());
        details.put("reasonCode", plan.reasonCode());
        details.put("reason", reason);
        try {
            return new ObjectMapper().writeValueAsString(details);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
