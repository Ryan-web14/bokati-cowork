package com.sni.bokaticowork.features.portal.notification.service;

import com.sni.bokaticowork.core.enums.EventType;
import com.sni.bokaticowork.core.enums.NotificationChannel;
import com.sni.bokaticowork.core.settings.userSettings.service.MemberPreferences;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.notification.service.interfaces.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberInAppNotifierPreferenceTest {

    private final NotificationService notificationService = mock(NotificationService.class);
    private final MemberPreferences preferences = mock(MemberPreferences.class);
    private final MemberInAppNotifier notifier = new MemberInAppNotifier(notificationService);
    private final Member member = Member.builder().id(7L).memberId("MBR-7").email("awa@example.com").build();

    @BeforeEach
    void setUp() {
        notifier.setMemberPreferences(preferences);
        when(preferences.memberByEmail("awa@example.com")).thenReturn(Optional.of(member));
    }

    @Test
    @DisplayName("Le membre a coupe la confirmation dans l'application : rien n'est cree")
    void cutByMember() {
        when(preferences.allows(member, EventType.BOOKING_CONFIRMED, NotificationChannel.IN_APP)).thenReturn(false);

        notifier.notify("BOOKING_CONFIRMED", "BOOKING", "BKG-1", "awa@example.com", "Awa", "MBR-7",
                "Reservation confirmee", Map.of());

        verify(notificationService, never()).send(any());
    }

    @Test
    @DisplayName("Sans preference contraire, la notification part comme avant")
    void sentByDefault() {
        when(preferences.allows(member, EventType.BOOKING_CONFIRMED, NotificationChannel.IN_APP)).thenReturn(true);

        notifier.notify("BOOKING_CONFIRMED", "BOOKING", "BKG-1", "awa@example.com", "Awa", "MBR-7",
                "Reservation confirmee", Map.of());

        verify(notificationService).send(any());
    }

    @Test
    @DisplayName("Une relance d'impaye ne se coupe jamais, meme si le membre a tout coupe")
    void dunningIsNeverCut() {
        when(preferences.allows(any(Member.class), any(), any())).thenReturn(false);

        notifier.notify("BILLING_DUNNING_REMINDER", "BILLING_DOCUMENT", "FAC-1", "awa@example.com", "Awa", "MBR-7",
                "Relance", Map.of());

        verify(notificationService).send(any());
    }

    @Test
    @DisplayName("Sans le service de preferences, tout part")
    void withoutPreferencesEverythingIsSent() {
        MemberInAppNotifier bare = new MemberInAppNotifier(notificationService);

        bare.notify("BOOKING_CANCELLED", "BOOKING", "BKG-1", "awa@example.com", "Awa", "MBR-7",
                "Reservation annulee", Map.of());

        verify(notificationService).send(any());
    }
}
