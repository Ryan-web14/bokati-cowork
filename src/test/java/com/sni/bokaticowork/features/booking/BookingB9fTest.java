package com.sni.bokaticowork.features.booking;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.booking.enums.BookingChannel;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import com.sni.bokaticowork.features.booking.service.support.BookingExtras;
import com.sni.bokaticowork.features.booking.service.support.BookingFilling;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot B9f : derniere minute et prolongation. */
class BookingB9fTest {

    private final BookingService bookingService = mock(BookingService.class);
    private final BookingExtras extras = mock(BookingExtras.class);
    private final PlatformSettings settings = mock(PlatformSettings.class);

    @SuppressWarnings("unchecked")
    private BookingFilling filling(int rate) {
        ObjectProvider<BookingService> service = mock(ObjectProvider.class);
        when(service.getObject()).thenReturn(bookingService);
        ObjectProvider<BookingExtras> extrasProvider = mock(ObjectProvider.class);
        when(extrasProvider.getObject()).thenReturn(extras);
        BookingFilling filling = new BookingFilling(mock(BookingRepository.class), service, extrasProvider);
        ReflectionTestUtils.setField(filling, "settings", settings);
        when(settings.integer(eq("bookings.last-minute.discount-rate"), anyInt())).thenReturn(rate);
        when(settings.integer(eq("bookings.last-minute.hours"), anyInt())).thenReturn(24);
        return filling;
    }

    private static Booking booking(BookingChannel channel, LocalDateTime start) {
        return Booking.builder().bookingNumber("BKG-1").channel(channel).startedAt(start).totalAmount(new BigDecimal("20000")).build();
    }

    @Test
    void aPortalBookingStartingSoonGetsTheLastMinuteDiscount() {
        BookingFilling filling = filling(25);
        filling.applyLastMinute(booking(BookingChannel.PORTAL, LocalDateTime.now().plusHours(3)));
        verify(extras).applyManualDiscount(any(), eq(new BigDecimal("5000")), anyString());
        assertThat(filling.discounted(new BigDecimal("20000"))).isEqualByComparingTo("15000");
    }

    @Test
    void noDiscountOutsideTheWindowForTheTeamOrWhenSwitchedOff() {
        filling(25).applyLastMinute(booking(BookingChannel.PORTAL, LocalDateTime.now().plusDays(3)));
        filling(25).applyLastMinute(booking(BookingChannel.TEAM, LocalDateTime.now().plusHours(3)));
        filling(0).applyLastMinute(booking(BookingChannel.PORTAL, LocalDateTime.now().plusHours(3)));
        verify(extras, never()).applyManualDiscount(any(), any(), anyString());
    }

    @Test
    void anExtensionIsBoundedAndGoesThroughTheService() {
        BookingFilling filling = filling(0);
        assertThatThrownBy(() -> filling.extend("BKG-1", 0, null)).isInstanceOf(BadRequestException.class);
        filling.extend("BKG-1", 30, Boolean.TRUE);
        verify(bookingService).extend("BKG-1", 30, Boolean.TRUE);
    }
}
