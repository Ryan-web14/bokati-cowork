package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.reference.service.SessionActor;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.model.BookingPayOnSiteGrant;
import com.sni.bokaticowork.features.booking.repository.BookingPayOnSiteGrantRepository;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Lot B3 : rendez-vous sans paiement, regle sur place.
 *
 * <p>Une reservation « reglee sur place » ({@code Booking.payOnSite}) est une reservation
 * {@code DIRECT} en attente de paiement : creneau retenu jusqu'a l'echeance (regle D1 du lot B1),
 * rappel au client avant l'echeance, puis expiration automatique avec l'equipe prevenue (D2).
 * L'equipe en pose toujours ; le client du portail seulement si l'option lui est ouverte (D3) :
 * option par client posee par l'equipe, sinon reglage global {@code bookings.pay-on-site.portal-enabled}
 * (coupe par defaut). L'option est retiree d'office apres {@code bookings.pay-on-site.max-no-shows}
 * absences sur des rendez-vous regles sur place, dans les {@code ...no-show-window-days} derniers jours.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingPayOnSite {

    static final String PORTAL_KEY = "bookings.pay-on-site.portal-enabled";
    static final String MAX_NO_SHOWS_KEY = "bookings.pay-on-site.max-no-shows";
    static final String WINDOW_KEY = "bookings.pay-on-site.no-show-window-days";

    /** D'ou vient la decision pour un client. */
    public enum Source { CLIENT_OPTION, GLOBAL_SETTING }

    public record Access(boolean allowed, Source source, String reason) {
    }

    public record Grant(SubscriberType ownerType, String ownerCode, boolean enabled, String reason, boolean revokedAutomatically,
                        String updatedBy, Instant updatedAt) {
    }

    private final BookingPayOnSiteGrantRepository grants;
    private final BookingRepository bookings;

    private PlatformSettings settings;
    private SessionActor actor;

    private BookingReliability reliability;

    @Autowired(required = false)
    void setReliability(BookingReliability reliability) {
        this.reliability = reliability;
    }

    @Autowired(required = false)
    void setOptional(PlatformSettings settings, SessionActor actor) {
        this.settings = settings;
        this.actor = actor;
    }

    /** Le client peut-il prendre au portail un rendez-vous regle sur place ? */
    @Transactional(readOnly = true)
    public Access access(SubscriberType ownerType, String ownerCode) {
        if (ownerType != null && StringUtils.hasText(ownerCode)) {
            BookingPayOnSiteGrant grant = grants.findByOwnerTypeAndOwnerCode(ownerType, ownerCode.trim()).orElse(null);
            if (grant != null) {
                return new Access(Boolean.TRUE.equals(grant.getEnabled()), Source.CLIENT_OPTION, grant.getReason());
            }
        }
        boolean global = settings != null && settings.bool(PORTAL_KEY, false);
        return new Access(global, Source.GLOBAL_SETTING, null);
    }

    /** Refus au portail : {@code PAY_ON_SITE_NOT_ALLOWED}. */
    public void assertPortalAllowed(SubscriberType ownerType, String ownerCode) {
        if (reliability != null && reliability.prepayRequired(ownerType, ownerCode)) {
            throw ConflictException.coded("PREPAYMENT_REQUIRED",
                    "Réglez votre réservation en ligne : le rendez-vous réglé sur place n'est pas disponible pour votre compte");
        }
        if (!access(ownerType, ownerCode).allowed()) {
            throw ConflictException.coded("PAY_ON_SITE_NOT_ALLOWED",
                    "Le rendez-vous réglé sur place n'est pas ouvert à votre compte : réglez en ligne, ou contactez l'accueil");
        }
    }

    /** Option posee par l'equipe pour un client ; elle l'emporte sur le reglage global. */
    @Transactional
    public Grant set(SubscriberType ownerType, String ownerCode, boolean enabled, String reason) {
        BookingPayOnSiteGrant grant = grants.findByOwnerTypeAndOwnerCode(ownerType, ownerCode.trim())
                .orElseGet(() -> BookingPayOnSiteGrant.builder().ownerType(ownerType).ownerCode(ownerCode.trim()).build());
        grant.setEnabled(enabled);
        grant.setReason(StringUtils.hasText(reason) ? reason.trim() : null);
        grant.setRevokedAutomatically(false);
        grant.setUpdatedBy(actor == null ? null : actor.resolve(null));
        return view(grants.save(grant));
    }

    @Transactional(readOnly = true)
    public List<Grant> list() {
        return grants.findAllByOrderByUpdatedAtDesc().stream().map(BookingPayOnSite::view).toList();
    }

    /**
     * Apres une absence sur un rendez-vous regle sur place : au-dela du seuil, l'option du client
     * est retiree d'office (le portail lui demandera de regler en ligne). Rien si le seuil est 0.
     */
    @Transactional
    public boolean onNoShow(Booking booking) {
        if (booking == null || !Boolean.TRUE.equals(booking.getPayOnSite()) || booking.getOwnerType() == null
                || !StringUtils.hasText(booking.getOwnerCode())) {
            return false;
        }
        int max = settings == null ? 2 : settings.integer(MAX_NO_SHOWS_KEY, 2);
        if (max <= 0) {
            return false;
        }
        int days = Math.max(1, settings == null ? 90 : settings.integer(WINDOW_KEY, 90));
        long noShows = bookings.countPayOnSiteNoShows(booking.getOwnerType().name(), booking.getOwnerCode(),
                com.sni.bokaticowork.core.utils.time.AppTime.now().minus(days, ChronoUnit.DAYS));
        if (noShows < max) {
            return false;
        }
        BookingPayOnSiteGrant grant = grants.findByOwnerTypeAndOwnerCode(booking.getOwnerType(), booking.getOwnerCode())
                .orElseGet(() -> BookingPayOnSiteGrant.builder().ownerType(booking.getOwnerType()).ownerCode(booking.getOwnerCode()).build());
        if (grant.getId() != null && !Boolean.TRUE.equals(grant.getEnabled())) {
            return false;
        }
        grant.setEnabled(false);
        grant.setRevokedAutomatically(true);
        grant.setReason("Retiré d'office après " + noShows + " absences en " + days + " jours");
        grant.setUpdatedBy(SessionActor.SYSTEM);
        grants.save(grant);
        log.info("Rendez-vous réglé sur place retiré d'office pour {}/{} ({} absences)", booking.getOwnerType(), booking.getOwnerCode(), noShows);
        return true;
    }

    private static Grant view(BookingPayOnSiteGrant grant) {
        return new Grant(grant.getOwnerType(), grant.getOwnerCode(), Boolean.TRUE.equals(grant.getEnabled()), grant.getReason(),
                Boolean.TRUE.equals(grant.getRevokedAutomatically()), grant.getUpdatedBy(), grant.getUpdatedAt());
    }
}
