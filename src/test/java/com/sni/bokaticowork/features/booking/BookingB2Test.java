package com.sni.bokaticowork.features.booking;

import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.service.support.BookingCancellationRefundSupport;
import com.sni.bokaticowork.features.booking.service.support.BookingPartialCoverage;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

/** Lot B2 : argent des reservations. */
class BookingB2Test {

    @Test
    void theUncoveredPartIsBilledProRata() {
        // 4 h a 20 000, 1 h 30 couvertes par le droit : 2 h 30 facturees.
        assertThat(BookingPartialCoverage.uncoveredAmount(BigDecimal.valueOf(20000), 240, 90)).isEqualByComparingTo("12500");
        assertThat(BookingPartialCoverage.uncoveredAmount(BigDecimal.valueOf(20000), 240, 240)).isEqualByComparingTo("0");
    }

    @Test
    void theResourcePolicyGridDecidesTheRefund() throws Exception {
        BookingCancellationRefundSupport support = mock(BookingCancellationRefundSupport.class, CALLS_REAL_METHODS);
        Method percent = BookingCancellationRefundSupport.class.getDeclaredMethod("refundPercent", Booking.class, long.class);
        percent.setAccessible(true);
        Booking booking = Booking.builder().resource(Resource.builder()
                .resourcePolicy(ResourcePolicy.builder().active(true).refundRules("72:100, 24:50, 0:0").build()).build()).build();

        assertThat((BigDecimal) percent.invoke(support, booking, 100L)).isEqualByComparingTo("100");
        assertThat((BigDecimal) percent.invoke(support, booking, 30L)).isEqualByComparingTo("50");
        assertThat((BigDecimal) percent.invoke(support, booking, 5L)).isEqualByComparingTo("0");
    }
}
