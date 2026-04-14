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
        NotificationPreferences preference = notificationPreferenceRepository
                .findByMember_IdAndEventTypeAndNotificationChannel(member.getId(), request.eventType(), request.channel())
                .orElseGet(() -> createDefaultPreference(member, request.eventType(), request.channel()));

        boolean newEnabled = Boolean.TRUE.equals(request.enabled());
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

        return buildPreferenceResponses(member.getId());
    }

    private void applySettingsUpdate(MemberSettings settings,
                                     UpdateMemberSettingsRequest request,
                                     Long actorId,
                                     String reason) {
        updateField("langage", settings.getLangage(), request.getLangage(), actorId, reason, settings::setLangage);
        updateField("timeZone", settings.getTimeZone(), request.getTimeZone(), actorId, reason, settings::setTimeZone);
        updateField("showNameOnDisplays", settings.isShowNameOnDisplays(), Boolean.TRUE.equals(request.getShowNameOnDisplay()), actorId, reason, settings::setShowNameOnDisplays);
        updateField("showEmail", settings.isShowEmail(), Boolean.TRUE.equals(request.getShowEmail()), actorId, reason, settings::setShowEmail);
        updateField("showPhone", settings.isShowPhone(), Boolean.TRUE.equals(request.getShowPhone()), actorId, reason, settings::setShowPhone);

        BillingPreferences billing = request.getBillingPref();
        if (billing != null) {
            updateField("billingEntityType", settings.getEntityType(), billing.billingEntityType(), actorId, reason, settings::setEntityType);
            updateField("billingCompanyName", settings.getBillingCompanyName(), billing.billingCompanyName(), actorId, reason, settings::setBillingCompanyName);
            updateField("billingEmail", settings.getBillingEmail(), billing.billingEmail(), actorId, reason, settings::setBillingEmail);
            updateField("billingAddress", settings.getBillingAddress(), billing.billlingAddress(), actorId, reason, settings::setBillingAddress);
        }

        WalletPreferencesRequest wallet = request.getWalletPref();
        if (wallet != null) {
            updateField("walletAutoPopUp", settings.isWalletAutoPopUp(), Boolean.TRUE.equals(wallet.autoTopupEnabled()), actorId, reason, settings::setWalletAutoPopUp);
            updateField("walletAutoTopupThreshold", settings.getWalletAutoTopupThreshold(), wallet.autoTopupThreshold(), actorId, reason, settings::setWalletAutoTopupThreshold);
            updateField("walletAutoTopupAmount", settings.getWalletAutoTopupAmount(), wallet.autoTopupAmount(), actorId, reason, settings::setWalletAutoTopupAmount);
            updateField("walletSpendingDailyLimit", settings.getWalletSpendingDailyLimit(), wallet.spendingDailyLimit(), actorId, reason, settings::setWalletSpendingDailyLimit);
            updateField("walletSpendingWeeklyLimit", settings.getWalletSpendingWeeklyLimit(), wallet.spendingWeeklyLimit(), actorId, reason, settings::setWalletSpendingWeeklyLimit);
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
                        match != null && match.isEnabled()
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
        preference.setEnabled(false);
        preference.setPreferenceName(defaultPreferenceName(eventType, channel));
        preference.setPreferenceDescription(defaultPreferenceDescription(eventType, channel));
        return preference;
    }

    private MemberSettings defaultSettings(Member member) {
        MemberSettings settings = new MemberSettings();
        settings.setMember(member);
        settings.setLangage("fr");
        settings.setTimeZone("Africa/Lagos");
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
                        settings.getWalletSpendingWeeklyLimit()
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

    private String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
