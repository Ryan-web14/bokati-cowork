package com.sni.bokaticowork.features.booking;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.booking.enums.BookingEventType;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.model.BookingPayOnSiteGrant;
import com.sni.bokaticowork.features.booking.repository.BookingPayOnSiteGrantRepository;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.implementation.BookingServiceImpl;
import com.sni.bokaticowork.features.booking.service.support.BookingEventWriter;
import com.sni.bokaticowork.features.booking.service.support.BookingPayOnSite;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot B3 : rendez-vous sans paiement, regle sur place. */
class BookingB3Test {

    private final BookingPayOnSiteGrantRepository grants = mock(BookingPayOnSiteGrantRepository.class);
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final PlatformSettings settings = mock(PlatformSettings.class);

    private BookingPayOnSite payOnSite() {
        BookingPayOnSite service = new BookingPayOnSite(grants, bookings);
        ReflectionTestUtils.setField(service, "settings", settings);
        when(settings.integer(anyString(), anyInt())).thenAnswer(call -> call.getArgument(1));
        return service;
    }

    @Test
    void theClientOptionWinsOverTheGlobalSettingWhichIsOffByDefault() {
        BookingPayOnSite service = payOnSite();
        when(settings.bool("bookings.pay-on-site.portal-enabled", false)).thenReturn(false);
        assertThat(service.access(SubscriberType.MEMBER, "MBR-1").allowed()).isFalse();
        assertThatThrownBy(() -> service.assertPortalAllowed(SubscriberType.MEMBER, "MBR-1")).isInstanceOf(ConflictException.class)
                .hasMessageContaining("réglé sur place");

        when(grants.findByOwnerTypeAndOwnerCode(SubscriberType.MEMBER, "MBR-1"))
                .thenReturn(Optional.of(BookingPayOnSiteGrant.builder().enabled(true).reason("Client fidèle").build()));
        BookingPayOnSite.Access access = service.access(SubscriberType.MEMBER, "MBR-1");
        assertThat(access.allowed()).isTrue();
        assertThat(access.source()).isEqualTo(BookingPayOnSite.Source.CLIENT_OPTION);

        when(settings.bool("bookings.pay-on-site.portal-enabled", false)).thenReturn(true);
        when(grants.findByOwnerTypeAndOwnerCode(SubscriberType.MEMBER, "MBR-2"))
                .thenReturn(Optional.of(BookingPayOnSiteGrant.builder().enabled(false).build()));
        assertThat(service.access(SubscriberType.MEMBER, "MBR-2").allowed()).isFalse();
        assertThat(service.access(SubscriberType.MEMBER, "MBR-3").allowed()).isTrue();
    }

    @Test
    void theOptionIsWithdrawnAfterTooManyNoShowsOnPayOnSiteAppointments() {
        BookingPayOnSite service = payOnSite();
        Booking booking = Booking.builder().bookingNumber("BKG-1").ownerType(SubscriberType.MEMBER).ownerCode("MBR-1").payOnSite(true).build();
        when(grants.findByOwnerTypeAndOwnerCode(SubscriberType.MEMBER, "MBR-1")).thenReturn(Optional.empty());

        when(bookings.countPayOnSiteNoShows(eq("MEMBER"), eq("MBR-1"), any(LocalDateTime.class))).thenReturn(1L);
        assertThat(service.onNoShow(booking)).isFalse();

        when(bookings.countPayOnSiteNoShows(eq("MEMBER"), eq("MBR-1"), any(LocalDateTime.class))).thenReturn(2L);
        assertThat(service.onNoShow(booking)).isTrue();
        ArgumentCaptor<BookingPayOnSiteGrant> saved = ArgumentCaptor.forClass(BookingPayOnSiteGrant.class);
        verify(grants).save(saved.capture());
        assertThat(saved.getValue().getEnabled()).isFalse();
        assertThat(saved.getValue().getRevokedAutomatically()).isTrue();
        assertThat(saved.getValue().getReason()).startsWith("Retiré d'office après 2 absences");

        Booking paid = Booking.builder().ownerType(SubscriberType.MEMBER).ownerCode("MBR-1").payOnSite(false).build();
        assertThat(service.onNoShow(paid)).isFalse();
    }

    @Test
    void aReminderIsSentOnceBeforeThePaymentDeadline() {
        BookingServiceImpl service = mock(BookingServiceImpl.class, CALLS_REAL_METHODS);
        BookingEventWriter events = mock(BookingEventWriter.class);
        ReflectionTestUtils.setField(service, "bookingRepository", bookings);
        ReflectionTestUtils.setField(service, "eventWriter", events);
        ReflectionTestUtils.setField(service, "platformSettings", settings);
        when(settings.integer("bookings.payment-due.reminder-minutes", 30)).thenReturn(30);
        Booking booking = Booking.builder().bookingNumber("BKG-2").status(BookingStatus.PENDING_PAYMENT).payOnSite(true)
                .paymentDueAt(LocalDateTime.now().plusMinutes(20)).build();
        when(bookings.findPaymentRemindersDue(any(), any(), eq(100))).thenReturn(List.of(booking));

        assertThat(service.remindPaymentsDue(100)).isEqualTo(1);
        assertThat(booking.getPaymentReminderSentAt()).isNotNull();
        verify(events).write(eq(booking), eq(BookingEventType.BOOKING_PAYMENT_REMINDER), anyString(), anyString(), any(), eq(true));

        when(settings.integer("bookings.payment-due.reminder-minutes", 30)).thenReturn(0);
        assertThat(service.remindPaymentsDue(100)).isZero();
        verify(bookings, never()).findPaymentRemindersDue(any(), any(), eq(5));
    }
}
