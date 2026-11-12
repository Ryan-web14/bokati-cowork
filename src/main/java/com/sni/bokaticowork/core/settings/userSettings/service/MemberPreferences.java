package com.sni.bokaticowork.core.settings.userSettings.service;

import com.sni.bokaticowork.core.enums.EventType;
import com.sni.bokaticowork.core.enums.NotificationChannel;
import com.sni.bokaticowork.core.enums.ReceiptPreference;
import com.sni.bokaticowork.core.settings.userSettings.model.MemberSettings;
import com.sni.bokaticowork.core.settings.userSettings.model.NotificationPreferences;
import com.sni.bokaticowork.core.settings.userSettings.repository.MemberSettingRepository;
import com.sni.bokaticowork.core.settings.userSettings.repository.NotificationPreferenceRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;

/**
 * Les preferences d'un membre, telles que les autres modules les appliquent.
 *
 * <p>Une preference absente vaut le comportement par defaut : notification envoyee, facture a
 * l'adresse du compte, recu a chaque operation, fuseau de l'exploitant, aucun plafond personnel.
 * Une lecture qui echoue ne bloque jamais un envoi ni une operation : elle rend le defaut.</p>
 *
 * <p>Seuls les evenements de {@link EventType} se coupent. Les relances d'impayes, les messages de
 * securite (codes, mots de passe, connexions), les contrats et les messages de conformite ne passent
 * pas par ici et partent toujours. Couper l'envoi d'une facture ou d'un recu ne la retire pas du
 * portail.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class MemberPreferences {

    public static final String DEFAULT_TIME_ZONE = "Africa/Brazzaville";
    public static final String DEFAULT_LANGUAGE = "fr";

    private final MemberSettingRepository settingsRepository;
    private final NotificationPreferenceRepository notificationRepository;
    private final MemberRepository memberRepository;

    /** Le membre accepte-t-il cet evenement sur ce canal ? Oui par defaut. */
    public boolean allows(Member member, EventType event, NotificationChannel channel) {
        if (member == null || event == null || channel == null) {
            return true;
        }
        try {
            return notificationRepository
                    .findByMember_IdAndEventTypeAndNotificationChannel(member.getId(), event, channel)
                    .map(NotificationPreferences::isEnabled)
                    .orElse(true);
        } catch (RuntimeException ex) {
            log.warn("Preference {} {} du membre {} illisible, envoi maintenu : {}", event, channel, member.getMemberId(), ex.getMessage());
            return true;
        }
    }

    /** Meme question quand l'envoi ne connait que l'adresse : une adresse sans membre est toujours servie. */
    public boolean allowsEmail(String email, EventType event) {
        return memberByEmail(email).map(member -> allows(member, event, NotificationChannel.EMAIL)).orElse(true);
    }

    public boolean allows(String memberId, EventType event, NotificationChannel channel) {
        return member(memberId).map(member -> allows(member, event, channel)).orElse(true);
    }

    /** L'adresse de facturation choisie par le membre, si elle est renseignee. */
    public Optional<String> billingEmail(Member member) {
        return settings(member).map(MemberSettings::getBillingEmail).filter(StringUtils::hasText).map(String::trim);
    }

    public Optional<String> billingEmail(String memberId) {
        return member(memberId).flatMap(this::billingEmail);
    }

    /** L'adresse ou envoyer une facture ou un recu : celle de facturation du membre, sinon celle donnee. */
    public String billingRecipient(Member member, String fallback) {
        return billingEmail(member).orElse(fallback);
    }

    public ZoneId zone(Member member) {
        String zone = settings(member).map(MemberSettings::getTimeZone).filter(StringUtils::hasText).orElse(DEFAULT_TIME_ZONE);
        try {
            return ZoneId.of(zone.trim());
        } catch (DateTimeException ex) {
            return ZoneId.of(DEFAULT_TIME_ZONE);
        }
    }

    public Locale locale(Member member) {
        String language = settings(member).map(MemberSettings::getLangage).filter(StringUtils::hasText).orElse(DEFAULT_LANGUAGE);
        return Locale.forLanguageTag(language.trim());
    }

    public ReceiptPreference walletReceipt(Member member) {
        return settings(member).map(MemberSettings::getWalletReceiptPreference).orElse(ReceiptPreference.EVERY_TRANSACTION);
    }

    /** Plafond personnel de depense par jour, si le membre en a fixe un. */
    public Optional<Long> dailySpendingCap(Member member) {
        return settings(member).map(MemberSettings::getWalletSpendingDailyLimit).filter(limit -> limit > 0).map(Integer::longValue);
    }

    public Optional<Long> weeklySpendingCap(Member member) {
        return settings(member).map(MemberSettings::getWalletSpendingWeeklyLimit).filter(limit -> limit > 0).map(Integer::longValue);
    }

    public Optional<Member> memberByEmail(String email) {
        if (!StringUtils.hasText(email) || email.contains(",")) {
            return Optional.empty();
        }
        try {
            return memberRepository.findByEmailIgnoreCaseAndDeletedFalse(email.trim());
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private Optional<Member> member(String memberId) {
        if (!StringUtils.hasText(memberId)) {
            return Optional.empty();
        }
        try {
            return memberRepository.findByMemberIdAndDeletedFalse(memberId.trim());
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private Optional<MemberSettings> settings(Member member) {
        if (member == null) {
            return Optional.empty();
        }
        try {
            return settingsRepository.findByMember_Id(member.getId());
        } catch (RuntimeException ex) {
            log.warn("Preferences du membre {} illisibles, valeurs par defaut : {}", member.getMemberId(), ex.getMessage());
            return Optional.empty();
        }
    }
}
