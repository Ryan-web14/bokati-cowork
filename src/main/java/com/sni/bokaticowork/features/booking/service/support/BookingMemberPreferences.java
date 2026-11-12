package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.enums.EventType;
import com.sni.bokaticowork.core.enums.NotificationChannel;
import com.sni.bokaticowork.core.settings.userSettings.service.MemberPreferences;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Les preferences du membre destinataire d'un message de reservation.
 *
 * <p>Le membre est le titulaire de la reservation quand c'est un membre, sinon le membre dont
 * l'adresse est celle du contact. Sans membre retrouve, tout part comme avant.</p>
 *
 * <p>Les heures d'une reservation sont saisies a l'heure de l'espace (fuseau de l'exploitant) ;
 * elles sont rendues dans le fuseau choisi par le membre.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingMemberPreferences {

    /** Le fuseau dans lequel les heures de reservation sont saisies : celui de l'espace. */
    static final ZoneId SITE_ZONE = ZoneId.of(MemberPreferences.DEFAULT_TIME_ZONE);

    private final MemberPreferences memberPreferences;
    private final MemberRepository memberRepository;

    public Optional<Member> member(Booking booking) {
        if (booking == null) {
            return Optional.empty();
        }
        try {
            if (booking.getOwnerType() == SubscriberType.MEMBER && StringUtils.hasText(booking.getOwnerCode())) {
                Optional<Member> owner = memberRepository.findByMemberIdAndDeletedFalse(booking.getOwnerCode().trim());
                if (owner.isPresent()) {
                    return owner;
                }
            }
            return memberPreferences.memberByEmail(booking.getContactEmail());
        } catch (RuntimeException ex) {
            log.debug("Membre de la reservation {} introuvable : {}", booking.getBookingNumber(), ex.getMessage());
            return Optional.empty();
        }
    }

    /** Vrai par defaut ; faux seulement si le membre a coupe cet evenement sur ce canal. */
    public boolean allows(Booking booking, EventType event, NotificationChannel channel) {
        if (event == null) {
            return true;
        }
        return allows(member(booking), event, channel);
    }

    public boolean allows(Optional<Member> member, EventType event, NotificationChannel channel) {
        if (event == null || member == null) {
            return true;
        }
        return member.map(found -> memberPreferences.allows(found, event, channel)).orElse(true);
    }

    public ZoneId zone(Booking booking) {
        return zone(member(booking));
    }

    public ZoneId zone(Optional<Member> member) {
        if (member == null) {
            return SITE_ZONE;
        }
        return member.map(memberPreferences::zone).orElse(SITE_ZONE);
    }

    /** Une heure de reservation (heure de l'espace) rendue dans le fuseau donne. */
    public static LocalDateTime inZone(LocalDateTime siteTime, ZoneId zone) {
        if (siteTime == null || zone == null) {
            return siteTime;
        }
        return siteTime.atZone(SITE_ZONE).withZoneSameInstant(zone).toLocalDateTime();
    }
}
