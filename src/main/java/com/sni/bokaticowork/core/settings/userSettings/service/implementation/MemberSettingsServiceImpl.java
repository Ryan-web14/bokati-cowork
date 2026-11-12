package com.sni.bokaticowork.core.settings.userSettings.service.implementation;

import com.sni.bokaticowork.core.audit.model.SettingsAuditLogs;
import com.sni.bokaticowork.core.audit.repository.SettingsAuditLogsRepository;
import com.sni.bokaticowork.core.enums.BillingEntityType;
import com.sni.bokaticowork.core.enums.EventType;
import com.sni.bokaticowork.core.enums.NotificationChannel;
import com.sni.bokaticowork.core.enums.ReceiptPreference;
import com.sni.bokaticowork.core.settings.userSettings.dto.request.BillingPreferences;
import com.sni.bokaticowork.core.settings.userSettings.dto.request.UpdateMemberSettingsRequest;
import com.sni.bokaticowork.core.settings.userSettings.dto.request.UpdateNotificationPreferenceRequest;
import com.sni.bokaticowork.core.settings.userSettings.dto.request.WalletPreferencesRequest;
import com.sni.bokaticowork.core.settings.userSettings.dto.response.BillingPreferencesResponse;
import com.sni.bokaticowork.core.settings.userSettings.dto.response.MemberSettingsResponse;
import com.sni.bokaticowork.core.settings.userSettings.dto.response.NotificationPreferenceResponse;
import com.sni.bokaticowork.core.settings.userSettings.dto.response.WalletPreferencesResponse;
import com.sni.bokaticowork.core.settings.userSettings.model.MemberSettings;
import com.sni.bokaticowork.core.settings.userSettings.model.NotificationPreferences;
import com.sni.bokaticowork.core.settings.userSettings.repository.MemberSettingRepository;
import com.sni.bokaticowork.core.settings.userSettings.repository.NotificationPreferenceRepository;
import com.sni.bokaticowork.core.settings.userSettings.service.interfaces.MemberSettingsService;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.service.interfaces.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberSettingsServiceImpl implements MemberSettingsService {

    private final MemberService memberService;
    private final MemberSettingRepository memberSettingRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final SettingsAuditLogsRepository settingsAuditLogsRepository;

    @Override
    @Transactional(readOnly = true)
    public MemberSettingsResponse getSettings(String memberId) {
        Member member = memberService.getByMemberIdForService(memberId);
        MemberSettings settings = memberSettingRepository.findByMember_Id(member.getId())
                .orElseGet(() -> defaultSettings(member));
        return toResponse(settings, buildPreferenceResponses(member.getId()));
    }

    @Override
    public MemberSettingsResponse updateSettings(String memberId,
                                                 UpdateMemberSettingsRequest request,
                                                 Long actorId,
                                                 String reason) {
        Member member = memberService.getByMemberIdForService(memberId);
        MemberSettings settings = memberSettingRepository.findByMember_Id(member.getId())
                .orElseGet(() -> defaultSettings(member));

        applySettingsUpdate(settings, request, actorId, reason);
        memberSettingRepository.save(settings);
        if (request.getNotificationPref() != null) {
            for (UpdateNotificationPreferenceRequest preference : request.getNotificationPref()) {
                applyNotificationPreference(member, preference, actorId, reason);
            }
        }
        return toResponse(settings, buildPreferenceResponses(member.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> getNotificationPreferences(String memberId) {
        Member member = memberService.getByMemberIdForService(memberId);
        return buildPreferenceResponses(member.getId());
    }

    @Override
    public List<NotificationPreferenceResponse> updateNotificationPreference(String memberId,
                                                                             UpdateNotificationPreferenceRequest request,
                                                                             Long actorId,
                                                                             String reason) {
        Member member = memberService.getByMemberIdForService(memberId);
        applyNotificationPreference(member, request, actorId, reason);
        return buildPreferenceResponses(member.getId());
    }

    /** Un couple evenement et canal ; les trois champs sont exiges. */
    private void applyNotificationPreference(Member member, UpdateNotificationPreferenceRequest request, Long actorId, String reason) {
        if (request == null || request.eventType() == null || request.channel() == null || request.enabled() == null) {
            throw new com.sni.bokaticowork.core.exception.customs.BadRequestException(
                    "Une préférence de notification demande eventType, channel et enabled.");
        }
        NotificationPreferences preference = notificationPreferenceRepository
                .findByMember_IdAndEventTypeAndNotificationChannel(member.getId(), request.eventType(), request.channel())
                .orElseGet(() -> createDefaultPreference(member, request.eventType(), request.channel()));
        boolean newEnabled = request.enabled();
        boolean previousEnabled = preference.isEnabled();
        preference.setEnabled(newEnabled);
        notificationPreferenceRepository.save(preference);

        if (previousEnabled != newEnabled) {
            saveAudit(
                    "notification." + request.eventType().name() + "." + request.channel().name(),
                    String.valueOf(previousEnabled),
                    String.valueOf(newEnabled),
                    actorId,
                    reason
            );
        }
    }

    private void applySettingsUpdate(MemberSettings settings,
                                     UpdateMemberSettingsRequest request,
                                     Long actorId,
                                     String reason) {
        // PATCH : un champ absent laisse la valeur telle quelle.
        if (request.getLangage() != null) {
            String language = request.getLangage().trim();
            if (language.isEmpty() || language.length() > 50) {
                throw new com.sni.bokaticowork.core.exception.customs.BadRequestException("La langue doit être un code comme fr ou en.");
            }
            updateField("langage", settings.getLangage(), language, actorId, reason, settings::setLangage);
        }
        if (request.getTimeZone() != null) {
            String zone = request.getTimeZone().trim();
            try {
                java.time.ZoneId.of(zone);
            } catch (java.time.DateTimeException ex) {
                throw new com.sni.bokaticowork.core.exception.customs.BadRequestException("Fuseau horaire inconnu : " + zone + ". Exemple : Africa/Brazzaville.");
            }
            updateField("timeZone", settings.getTimeZone(), zone, actorId, reason, settings::setTimeZone);
        }
        if (request.getShowNameOnDisplay() != null) {
            updateField("showNameOnDisplays", settings.isShowNameOnDisplays(), request.getShowNameOnDisplay(), actorId, reason, settings::setShowNameOnDisplays);
        }
        if (request.getShowEmail() != null) {
            updateField("showEmail", settings.isShowEmail(), request.getShowEmail(), actorId, reason, settings::setShowEmail);
        }
        if (request.getShowPhone() != null) {
            updateField("showPhone", settings.isShowPhone(), request.getShowPhone(), actorId, reason, settings::setShowPhone);
        }

        BillingPreferences billing = request.getBillingPref();
        if (billing != null) {
            if (billing.billingEntityType() != null) {
                updateField("billingEntityType", settings.getEntityType(), billing.billingEntityType(), actorId, reason, settings::setEntityType);
            }
            if (billing.billingCompanyName() != null) {
                updateField("billingCompanyName", settings.getBillingCompanyName(), blankToNull(billing.billingCompanyName()), actorId, reason, settings::setBillingCompanyName);
            }
            // Une ou plusieurs adresses separees par des virgules.
            if (org.springframework.util.StringUtils.hasText(billing.billingEmail())
                    && !com.sni.bokaticowork.core.utils.validation.EmailAddresses.isValidList(billing.billingEmail())) {
                throw new com.sni.bokaticowork.core.exception.customs.BadRequestException(
                        "Le courriel de facturation n'est pas valide : une ou plusieurs adresses séparées par des virgules");
            }
            // Une chaine vide retire l'adresse de facturation : les factures repartent a l'adresse du compte.
            if (billing.billingEmail() != null) {
                updateField("billingEmail", settings.getBillingEmail(),
                        blankToNull(com.sni.bokaticowork.core.utils.validation.EmailAddresses.normalize(billing.billingEmail())),
                        actorId, reason, settings::setBillingEmail);
            }
            if (billing.billlingAddress() != null) {
                updateField("billingAddress", settings.getBillingAddress(), blankToNull(billing.billlingAddress()), actorId, reason, settings::setBillingAddress);
            }
        }

        WalletPreferencesRequest wallet = request.getWalletPref();
        if (wallet != null) {
            if (wallet.autoTopupEnabled() != null) {
                updateField("walletAutoPopUp", settings.isWalletAutoPopUp(), wallet.autoTopupEnabled(), actorId, reason, settings::setWalletAutoPopUp);
            }
            if (wallet.autoTopupThreshold() != null) {
                updateField("walletAutoTopupThreshold", settings.getWalletAutoTopupThreshold(), zeroToNull(wallet.autoTopupThreshold()), actorId, reason, settings::setWalletAutoTopupThreshold);
            }
            if (wallet.autoTopupAmount() != null) {
                updateField("walletAutoTopupAmount", settings.getWalletAutoTopupAmount(), zeroToNull(wallet.autoTopupAmount()), actorId, reason, settings::setWalletAutoTopupAmount);
            }
            // 0 retire le plafond personnel.
            if (wallet.spendingDailyLimit() != null) {
                updateField("walletSpendingDailyLimit", settings.getWalletSpendingDailyLimit(), zeroToNull(wallet.spendingDailyLimit()), actorId, reason, settings::setWalletSpendingDailyLimit);
            }
            if (wallet.spendingWeeklyLimit() != null) {
                updateField("walletSpendingWeeklyLimit", settings.getWalletSpendingWeeklyLimit(), zeroToNull(wallet.spendingWeeklyLimit()), actorId, reason, settings::setWalletSpendingWeeklyLimit);
            }
            Integer daily = settings.getWalletSpendingDailyLimit();
            Integer weekly = settings.getWalletSpendingWeeklyLimit();
            if (daily != null && weekly != null && daily > weekly) {
                throw new com.sni.bokaticowork.core.exception.customs.BadRequestException("Le plafond journalier ne peut pas dépasser le plafond hebdomadaire.");
            }
            if (wallet.receiptPreference() != null) {
                updateField("walletReceiptPreference", settings.getWalletReceiptPreference(), wallet.receiptPreference(), actorId, reason, settings::setWalletReceiptPreference);
            }
        }
    }

    private List<NotificationPreferenceResponse> buildPreferenceResponses(Long memberId) {
        List<NotificationPreferences> persisted = notificationPreferenceRepository
                .findAllByMember_IdOrderByEventTypeAscNotificationChannelAsc(memberId);
        List<NotificationPreferenceResponse> responses = new ArrayList<>();

        for (EventType eventType : EventType.values()) {
            for (NotificationChannel channel : NotificationChannel.values()) {
                NotificationPreferences match = persisted.stream()
                        .filter(item -> item.getEventType() == eventType && item.getNotificationChannel() == channel)
                        .findFirst()
                        .orElse(null);

                responses.add(new NotificationPreferenceResponse(
                        defaultPreferenceName(eventType, channel),
                        defaultPreferenceDescription(eventType, channel),
                        eventType.name(),
                        channel.name(),
                        match == null || match.isEnabled()
                ));
            }
        }

        responses.sort(Comparator.comparing(NotificationPreferenceResponse::eventType)
                .thenComparing(NotificationPreferenceResponse::channel));
        return responses;
    }

    private NotificationPreferences createDefaultPreference(Member member, EventType eventType, NotificationChannel channel) {
        NotificationPreferences preference = new NotificationPreferences();
        preference.setMember(member);
        preference.setEventType(eventType);
        preference.setNotificationChannel(channel);
        preference.setEnabled(true);
        preference.setPreferenceName(defaultPreferenceName(eventType, channel));
        preference.setPreferenceDescription(defaultPreferenceDescription(eventType, channel));
        return preference;
    }

    private MemberSettings defaultSettings(Member member) {
        MemberSettings settings = new MemberSettings();
        settings.setMember(member);
        settings.setLangage("fr");
        settings.setTimeZone(com.sni.bokaticowork.core.settings.userSettings.service.MemberPreferences.DEFAULT_TIME_ZONE);
        settings.setShowNameOnDisplays(false);
        settings.setShowEmail(false);
        settings.setShowPhone(false);
        settings.setEntityType(BillingEntityType.PERSONAL);
        settings.setWalletAutoPopUp(false);
        settings.setWalletReceiptPreference(ReceiptPreference.EVERY_TRANSACTION);
        return settings;
    }

    private MemberSettingsResponse toResponse(MemberSettings settings, List<NotificationPreferenceResponse> notifications) {
        return MemberSettingsResponse.builder()
                .langage(settings.getLangage())
                .timeZone(settings.getTimeZone())
                .showNameOnDisplay(settings.isShowNameOnDisplays())
                .showEmail(settings.isShowEmail())
                .showPhone(settings.isShowPhone())
                .billingPref(new BillingPreferencesResponse(
                        settings.getEntityType() == null ? null : settings.getEntityType().name(),
                        settings.getBillingCompanyName(),
                        settings.getBillingEmail(),
                        settings.getBillingAddress()
                ))
                .walletPref(new WalletPreferencesResponse(
                        settings.isWalletAutoPopUp(),
                        settings.getWalletAutoTopupThreshold(),
                        settings.getWalletAutoTopupAmount(),
                        settings.getWalletSpendingDailyLimit(),
                        settings.getWalletSpendingWeeklyLimit(),
                        settings.getWalletReceiptPreference() == null ? null : settings.getWalletReceiptPreference().name()
                ))
                .notificationPref(notifications)
                .build();
    }

    private String defaultPreferenceName(EventType eventType, NotificationChannel channel) {
        return eventType.name() + "_" + channel.name();
    }

    private String defaultPreferenceDescription(EventType eventType, NotificationChannel channel) {
        return "Notification " + channel.name() + " pour l'evenement " + eventType.name();
    }

    private <T> void updateField(String key,
                                 T oldValue,
                                 T newValue,
                                 Long actorId,
                                 String reason,
                                 java.util.function.Consumer<T> setter) {
        if (!Objects.equals(oldValue, newValue)) {
            setter.accept(newValue);
            saveAudit(key, stringify(oldValue), stringify(newValue), actorId, reason);
        }
    }

    private void saveAudit(String key, String oldValue, String newValue, Long actorId, String reason) {
        SettingsAuditLogs log = SettingsAuditLogs.builder()
                .settingKey(key)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedBy(actorId)
                .changedAt(Instant.now())
                .reason(reason)
                .build();
        settingsAuditLogsRepository.save(log);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Integer zeroToNull(Integer value) {
        return value == null || value == 0 ? null : value;
    }

    private String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
