package com.sni.bokaticowork.core.settings.userSettings;

import com.sni.bokaticowork.core.audit.repository.SettingsAuditLogsRepository;
import com.sni.bokaticowork.core.enums.EventType;
import com.sni.bokaticowork.core.enums.NotificationChannel;
import com.sni.bokaticowork.core.enums.ReceiptPreference;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.settings.userSettings.dto.request.BillingPreferences;
import com.sni.bokaticowork.core.settings.userSettings.dto.request.UpdateMemberSettingsRequest;
import com.sni.bokaticowork.core.settings.userSettings.dto.request.UpdateNotificationPreferenceRequest;
import com.sni.bokaticowork.core.settings.userSettings.dto.request.WalletPreferencesRequest;
import com.sni.bokaticowork.core.settings.userSettings.dto.response.MemberSettingsResponse;
import com.sni.bokaticowork.core.settings.userSettings.dto.response.NotificationPreferenceResponse;
import com.sni.bokaticowork.core.settings.userSettings.model.MemberSettings;
import com.sni.bokaticowork.core.settings.userSettings.model.NotificationPreferences;
import com.sni.bokaticowork.core.settings.userSettings.repository.MemberSettingRepository;
import com.sni.bokaticowork.core.settings.userSettings.repository.NotificationPreferenceRepository;
import com.sni.bokaticowork.core.settings.userSettings.service.implementation.MemberSettingsServiceImpl;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.service.interfaces.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Un PATCH ne change que ce qu'il nomme ; les notifications partent tant que le membre ne les coupe pas. */
class MemberSettingsPatchTest {

    private final MemberService memberService = mock(MemberService.class);
    private final MemberSettingRepository settingsRepository = mock(MemberSettingRepository.class);
    private final NotificationPreferenceRepository notificationRepository = mock(NotificationPreferenceRepository.class);
    private final SettingsAuditLogsRepository auditRepository = mock(SettingsAuditLogsRepository.class);
    private final MemberSettingsServiceImpl service =
            new MemberSettingsServiceImpl(memberService, settingsRepository, notificationRepository, auditRepository);

    private final Member member = Member.builder().id(7L).memberId("MBR-7").build();
    private MemberSettings stored;

    @BeforeEach
    void setUp() {
        stored = new MemberSettings();
        stored.setMember(member);
        stored.setLangage("fr");
        stored.setTimeZone("Africa/Brazzaville");
        stored.setShowEmail(true);
        stored.setBillingEmail("compta@exemple.cg");
        stored.setWalletSpendingDailyLimit(50_000);
        when(memberService.getByMemberIdForService("MBR-7")).thenReturn(member);
        when(settingsRepository.findByMember_Id(7L)).thenReturn(Optional.of(stored));
        when(notificationRepository.findAllByMember_IdOrderByEventTypeAscNotificationChannelAsc(anyLong())).thenReturn(List.of());
        when(notificationRepository.findByMember_IdAndEventTypeAndNotificationChannel(anyLong(), any(), any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Un champ absent garde sa valeur")
    void omittedFieldsAreKept() {
        service.updateSettings("MBR-7", UpdateMemberSettingsRequest.builder().showPhone(true).build(), 1L, null);

        assertThat(stored.isShowPhone()).isTrue();
        assertThat(stored.isShowEmail()).isTrue();
        assertThat(stored.getLangage()).isEqualTo("fr");
        assertThat(stored.getBillingEmail()).isEqualTo("compta@exemple.cg");
        assertThat(stored.getWalletSpendingDailyLimit()).isEqualTo(50_000);
    }

    @Test
    @DisplayName("Une chaine vide retire l'adresse de facturation, 0 retire un plafond")
    void blankAndZeroClear() {
        service.updateSettings("MBR-7", UpdateMemberSettingsRequest.builder()
                .billingPref(new BillingPreferences(null, null, "", null))
                .walletPref(new WalletPreferencesRequest(null, null, null, 0, null, ReceiptPreference.INVOICE_ONLY))
                .build(), 1L, null);

        assertThat(stored.getBillingEmail()).isNull();
        assertThat(stored.getWalletSpendingDailyLimit()).isNull();
        assertThat(stored.getWalletReceiptPreference()).isEqualTo(ReceiptPreference.INVOICE_ONLY);
    }

    @Test
    @DisplayName("Fuseau inconnu et plafond journalier au-dessus de l'hebdomadaire sont refuses")
    void invalidValuesAreRefused() {
        assertThatThrownBy(() -> service.updateSettings("MBR-7",
                UpdateMemberSettingsRequest.builder().timeZone("Mars/Olympus").build(), 1L, null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.updateSettings("MBR-7", UpdateMemberSettingsRequest.builder()
                .walletPref(new WalletPreferencesRequest(null, null, null, 90_000, 60_000, null)).build(), 1L, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Sans ligne, une notification est activee ; la liste du PATCH est appliquee")
    void notificationsDefaultToEnabledAndPatchListApplies() {
        MemberSettingsResponse response = service.getSettings("MBR-7");
        assertThat(response.getNotificationPref()).isNotEmpty().allMatch(NotificationPreferenceResponse::enabled);
        assertThat(response.getTimeZone()).isEqualTo("Africa/Brazzaville");

        service.updateSettings("MBR-7", UpdateMemberSettingsRequest.builder()
                .notificationPref(List.of(new UpdateNotificationPreferenceRequest(EventType.INVOICE_ISSUED, NotificationChannel.EMAIL, false)))
                .build(), 1L, "Le membre ne veut plus les factures par courriel");

        ArgumentCaptor<NotificationPreferences> saved = ArgumentCaptor.forClass(NotificationPreferences.class);
        verify(notificationRepository).save(saved.capture());
        assertThat(saved.getValue().isEnabled()).isFalse();
        assertThat(saved.getValue().getEventType()).isEqualTo(EventType.INVOICE_ISSUED);
    }

    @Test
    @DisplayName("Une preference incomplete est refusee au lieu d'etre coupee")
    void incompletePreferenceIsRefused() {
        assertThatThrownBy(() -> service.updateNotificationPreference("MBR-7",
                new UpdateNotificationPreferenceRequest(EventType.PAYMENT_RECEIVED, NotificationChannel.EMAIL, null), 1L, null))
                .isInstanceOf(BadRequestException.class);
    }
}
