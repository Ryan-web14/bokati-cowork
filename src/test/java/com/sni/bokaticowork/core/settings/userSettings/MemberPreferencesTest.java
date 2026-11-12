package com.sni.bokaticowork.core.settings.userSettings;

import com.sni.bokaticowork.core.enums.EventType;
import com.sni.bokaticowork.core.enums.NotificationChannel;
import com.sni.bokaticowork.core.enums.ReceiptPreference;
import com.sni.bokaticowork.core.settings.userSettings.model.MemberSettings;
import com.sni.bokaticowork.core.settings.userSettings.model.NotificationPreferences;
import com.sni.bokaticowork.core.settings.userSettings.repository.MemberSettingRepository;
import com.sni.bokaticowork.core.settings.userSettings.repository.NotificationPreferenceRepository;
import com.sni.bokaticowork.core.settings.userSettings.service.MemberPreferences;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Sans preference, le comportement d'avant ; une lecture en echec ne coupe rien. */
class MemberPreferencesTest {

    private final MemberSettingRepository settings = mock(MemberSettingRepository.class);
    private final NotificationPreferenceRepository notifications = mock(NotificationPreferenceRepository.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final MemberPreferences preferences = new MemberPreferences(settings, notifications, members);
    private final Member member = Member.builder().id(3L).memberId("MBR-3").email("awa@exemple.cg").build();

    @Test
    @DisplayName("Par defaut tout part, au fuseau de l'exploitant, sans plafond, recu a chaque operation")
    void defaults() {
        when(settings.findByMember_Id(3L)).thenReturn(Optional.empty());
        when(notifications.findByMember_IdAndEventTypeAndNotificationChannel(anyLong(), any(), any())).thenReturn(Optional.empty());

        assertThat(preferences.allows(member, EventType.INVOICE_ISSUED, NotificationChannel.EMAIL)).isTrue();
        assertThat(preferences.zone(member)).isEqualTo(ZoneId.of("Africa/Brazzaville"));
        assertThat(preferences.dailySpendingCap(member)).isEmpty();
        assertThat(preferences.walletReceipt(member)).isEqualTo(ReceiptPreference.EVERY_TRANSACTION);
        assertThat(preferences.billingRecipient(member, "awa@exemple.cg")).isEqualTo("awa@exemple.cg");
    }

    @Test
    @DisplayName("Ce que le membre a choisi est rendu")
    void choices() {
        MemberSettings stored = new MemberSettings();
        stored.setTimeZone("Africa/Kinshasa");
        stored.setBillingEmail(" compta@exemple.cg ");
        stored.setWalletSpendingWeeklyLimit(200_000);
        when(settings.findByMember_Id(3L)).thenReturn(Optional.of(stored));
        NotificationPreferences off = new NotificationPreferences();
        off.setEnabled(false);
        when(notifications.findByMember_IdAndEventTypeAndNotificationChannel(3L, EventType.PAYMENT_RECEIVED, NotificationChannel.EMAIL))
                .thenReturn(Optional.of(off));
        when(members.findByEmailIgnoreCaseAndDeletedFalse("awa@exemple.cg")).thenReturn(Optional.of(member));

        assertThat(preferences.allowsEmail("awa@exemple.cg", EventType.PAYMENT_RECEIVED)).isFalse();
        assertThat(preferences.allowsEmail("inconnu@exemple.cg", EventType.PAYMENT_RECEIVED)).isTrue();
        assertThat(preferences.zone(member)).isEqualTo(ZoneId.of("Africa/Kinshasa"));
        assertThat(preferences.billingRecipient(member, "awa@exemple.cg")).isEqualTo("compta@exemple.cg");
        assertThat(preferences.weeklySpendingCap(member)).contains(200_000L);
    }

    @Test
    @DisplayName("Une base injoignable ne coupe aucun envoi")
    void failureKeepsSending() {
        when(notifications.findByMember_IdAndEventTypeAndNotificationChannel(anyLong(), any(), any()))
                .thenThrow(new IllegalStateException("base injoignable"));
        when(settings.findByMember_Id(anyLong())).thenThrow(new IllegalStateException("base injoignable"));

        assertThat(preferences.allows(member, EventType.BOOKING_REMINDER, NotificationChannel.EMAIL)).isTrue();
        assertThat(preferences.zone(member)).isEqualTo(ZoneId.of("Africa/Brazzaville"));
    }
}
