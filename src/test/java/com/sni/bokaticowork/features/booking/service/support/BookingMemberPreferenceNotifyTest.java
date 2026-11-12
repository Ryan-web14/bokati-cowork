package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.enums.EventType;
import com.sni.bokaticowork.core.enums.NotificationChannel;
import com.sni.bokaticowork.core.settings.userSettings.service.MemberPreferences;
import com.sni.bokaticowork.features.billing.service.support.BillingTaxRuleResolver;
import com.sni.bokaticowork.features.booking.enums.BookingEventType;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.TemplateEngine;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Les courriels de reservation suivent les preferences du membre : coupe par evenement, heures
 * dans son fuseau, et rien de change quand il n'a rien regle.
 */
class BookingMemberPreferenceNotifyTest {

    private final DefaultEmailSender emailSender = mock(DefaultEmailSender.class);
    private final BookingEventWriter eventWriter = mock(BookingEventWriter.class);
    private final MemberPreferences preferences = mock(MemberPreferences.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final BookingMemberPreferences bookingPreferences = new BookingMemberPreferences(preferences, memberRepository);
    private final BookingEmailNotifier notifier = new BookingEmailNotifier(
            emailSender, eventWriter, mock(TemplateEngine.class), mock(BillingTaxRuleResolver.class));
    private final Member member = Member.builder().id(7L).memberId("MBR-7").email("awa@example.com").build();

    private Booking booking() {
        return Booking.builder()
                .bookingNumber("BKG-1")
                .ownerType(SubscriberType.MEMBER)
                .ownerCode("MBR-7")
                .contactEmail("awa@example.com")
                .status(BookingStatus.CONFIRMED)
                .startedAt(LocalDateTime.of(2026, 11, 12, 10, 0))
                .endedAt(LocalDateTime.of(2026, 11, 12, 12, 0))
                .build();
    }

    @BeforeEach
    void setUp() {
        notifier.setMemberPreferences(bookingPreferences);
        when(memberRepository.findByMemberIdAndDeletedFalse("MBR-7")).thenReturn(Optional.of(member));
        when(preferences.zone(member)).thenReturn(ZoneId.of(MemberPreferences.DEFAULT_TIME_ZONE));
        when(preferences.allows(any(Member.class), any(), any())).thenReturn(true);
        when(emailSender.sendEmail(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(true));
    }

    @Test
    @DisplayName("Annulation coupee par le membre : aucun courriel, aucun echec journalise")
    void cancellationCutByMember() throws Exception {
        when(preferences.allows(member, EventType.BOOKING_CANCELLED, NotificationChannel.EMAIL)).thenReturn(false);

        notifier.notify(booking(), BookingEventType.BOOKING_CANCELLED);

        verifyNoInteractions(emailSender);
        verify(eventWriter, never()).write(any(), eq(BookingEventType.EMAIL_FAILED), any(), any(), any());
        verify(eventWriter, never()).write(any(), eq(BookingEventType.EMAIL_QUEUED), any(), any(), any());
    }

    @Test
    @DisplayName("Un message hors preferences (report) part meme si le membre a tout coupe")
    void rescheduleIsNeverCut() {
        when(preferences.allows(any(Member.class), any(), any())).thenReturn(false);

        notifier.notify(booking(), BookingEventType.BOOKING_RESCHEDULED);

        verify(emailSender).sendEmail(eq("awa@example.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("Les heures du courriel sont rendues dans le fuseau du membre")
    void timesInMemberZone() {
        when(preferences.zone(member)).thenReturn(ZoneId.of("UTC"));

        notifier.notify(booking(), BookingEventType.BOOKING_RESCHEDULED);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendEmail(anyString(), anyString(), body.capture());
        // 10:00 a Brazzaville (UTC+1) vaut 09:00 en UTC.
        assertThat(body.getValue()).contains("09:00").contains("11:00").doesNotContain("10:00");
    }

    @Test
    @DisplayName("Fuseau par defaut : les heures restent celles de l'espace")
    void defaultZoneKeepsSiteTime() {
        notifier.notify(booking(), BookingEventType.BOOKING_RESCHEDULED);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendEmail(anyString(), anyString(), body.capture());
        assertThat(body.getValue()).contains("10:00").contains("12:00");
    }

    @Test
    @DisplayName("Sans membre retrouve, tout part")
    void unknownContactIsAlwaysServed() {
        when(memberRepository.findByMemberIdAndDeletedFalse("MBR-7")).thenReturn(Optional.empty());
        when(preferences.memberByEmail("awa@example.com")).thenReturn(Optional.empty());

        assertThat(bookingPreferences.allows(booking(), EventType.BOOKING_CANCELLED, NotificationChannel.EMAIL)).isTrue();
        assertThat(bookingPreferences.zone(booking())).isEqualTo(ZoneId.of(MemberPreferences.DEFAULT_TIME_ZONE));
    }
}
