package com.sni.bokaticowork.features.booking;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.service.implementation.BookingServiceImpl;
import com.sni.bokaticowork.features.booking.service.support.BookingRepricer;
import com.sni.bokaticowork.features.ressource.model.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Lot B2b : reports et changements, ecart de prix, marquage. */
class BookingB2bTest {

    private final LocalDateTime start = LocalDateTime.of(2026, 12, 1, 9, 0);
    private final LocalDateTime end = start.plusHours(2);

    private BookingServiceImpl service(BigDecimal current, BigDecimal next) {
        BookingServiceImpl service = mock(BookingServiceImpl.class, CALLS_REAL_METHODS);
        BookingRepricer repricer = mock(BookingRepricer.class);
        when(repricer.preview(any(), any(), any(), any()))
                .thenReturn(new BookingRepricer.Preview(current, next, next.subtract(current)));
        ReflectionTestUtils.setField(service, "repricer", repricer);
        return service;
    }

    private void decide(BookingServiceImpl service, Booking booking, Boolean accepted) throws Throwable {
        Method method = BookingServiceImpl.class.getDeclaredMethod("requirePriceDecision", Booking.class, Resource.class,
                LocalDateTime.class, LocalDateTime.class, Boolean.class);
        method.setAccessible(true);
        try {
            method.invoke(service, booking, new Resource(), start, end, accepted);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    private Booking booking() {
        return Booking.builder().bookingNumber("BK-1").paymentMode(BookingPaymentMode.DIRECT).currency("XAF").build();
    }

    @Test
    void aDearerSlotMustBeConfirmed() {
        BookingServiceImpl service = service(new BigDecimal("10000"), new BigDecimal("15000"));
        Booking booking = booking();

        assertThatCode(() -> decide(service, booking, null)).isInstanceOf(ConflictException.class)
                .hasMessageContaining("10000 → 15000").hasMessageContaining("+5000");
        assertThatCode(() -> decide(service, booking, true)).doesNotThrowAnyException();
    }

    @Test
    void aCheaperSlotGoesThroughWithoutConfirmation() {
        BookingServiceImpl service = service(new BigDecimal("15000"), new BigDecimal("10000"));
        assertThatCode(() -> decide(service, booking(), null)).doesNotThrowAnyException();
    }

    @Test
    void aModificationIsMarkedOnTheBooking() throws Exception {
        BookingServiceImpl service = mock(BookingServiceImpl.class, CALLS_REAL_METHODS);
        Method mark = BookingServiceImpl.class.getDeclaredMethod("markModified", Booking.class, String.class, BookingRepricer.Outcome.class);
        mark.setAccessible(true);
        Booking booking = booking();
        booking.setPaidAt(java.time.Instant.now());

        mark.invoke(service, booking, "Report au 2026-12-01T09:00 – 2026-12-01T11:00",
                new BookingRepricer.Outcome(new BigDecimal("10000"), new BigDecimal("15000"), new BigDecimal("5000"), "FAC-9"));

        assertThat(booking.getModificationCount()).isEqualTo(1);
        assertThat(booking.getLastModifiedAt()).isNotNull();
        assertThat(booking.getLastModification()).contains("+5000 XAF").contains("FAC-9");
        // Complement emis : il reste a encaisser.
        assertThat(booking.getPaidAt()).isNull();
    }
}
