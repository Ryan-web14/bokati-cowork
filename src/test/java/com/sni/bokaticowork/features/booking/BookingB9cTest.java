package com.sni.bokaticowork.features.booking;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.booking.repository.BookingPayOnSiteGrantRepository;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.support.BookingPayOnSite;
import com.sni.bokaticowork.features.booking.service.support.BookingReliability;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Lot B9c : fiabilite et acompte. */
class BookingB9cTest {

    private final BookingRepository bookings = mock(BookingRepository.class);
    private final PlatformSettings settings = mock(PlatformSettings.class);

    private BookingReliability reliability(String mode) {
        BookingReliability reliability = new BookingReliability(bookings);
        ReflectionTestUtils.setField(reliability, "settings", settings);
        when(settings.integer(anyString(), anyInt())).thenAnswer(call -> call.getArgument(1));
        when(settings.text(eq("bookings.reliability.mode"), anyString())).thenReturn(mode);
        return reliability;
    }

    private void history(long noShows, long lateCancellations) {
        when(bookings.countForOwnerBetween(eq("MEMBER"), eq("MBR-1"), any(), any())).thenReturn(10L);
        when(bookings.countNoShowsForOwnerBetween(eq("MEMBER"), eq("MBR-1"), any(), any())).thenReturn(noShows);
        when(bookings.countLateCancellations(eq("MEMBER"), eq("MBR-1"), any(), anyInt(), anyString())).thenReturn(lateCancellations);
    }

    @Test
    void aReliableClientNeedsNoGuarantee() {
        history(1, 1);
        BookingReliability.Score score = reliability("DEPOSIT").score(SubscriberType.MEMBER, "MBR-1");
        assertThat(score.score()).isEqualTo(70);
        assertThat(score.guaranteeRequired()).isFalse();
        assertThat(reliability("DEPOSIT").depositFor(SubscriberType.MEMBER, "MBR-1", new BigDecimal("50000"))).isNull();
    }

    @Test
    void belowTheThresholdADepositIsAskedOrPrepaymentImposed() {
        history(2, 1);
        BookingReliability deposit = reliability("DEPOSIT");
        assertThat(deposit.score(SubscriberType.MEMBER, "MBR-1").score()).isEqualTo(50);
        assertThat(deposit.depositFor(SubscriberType.MEMBER, "MBR-1", new BigDecimal("50000"))).isEqualByComparingTo("25000");
        assertThat(deposit.prepayRequired(SubscriberType.MEMBER, "MBR-1")).isFalse();

        BookingReliability prepay = reliability("PREPAY");
        assertThat(prepay.prepayRequired(SubscriberType.MEMBER, "MBR-1")).isTrue();
        BookingPayOnSite payOnSite = new BookingPayOnSite(mock(BookingPayOnSiteGrantRepository.class), bookings);
        ReflectionTestUtils.setField(payOnSite, "reliability", prepay);
        assertThatThrownBy(() -> payOnSite.assertPortalAllowed(SubscriberType.MEMBER, "MBR-1")).isInstanceOf(ConflictException.class)
                .hasMessageContaining("en ligne");
    }

    @Test
    void theDepositIsRoundedAndCappedAtTheTotal() {
        assertThat(BookingReliability.deposit(new BigDecimal("33333"), new BigDecimal("30"))).isEqualByComparingTo("10000");
        assertThat(BookingReliability.deposit(new BigDecimal("1000"), new BigDecimal("150"))).isEqualByComparingTo("1000");
        assertThat(BookingReliability.deposit(BigDecimal.ZERO, new BigDecimal("30"))).isNull();
    }
}
