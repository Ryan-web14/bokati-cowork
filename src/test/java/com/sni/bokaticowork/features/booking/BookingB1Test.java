package com.sni.bokaticowork.features.booking;

import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentStatus;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.service.support.BookingPaymentDue;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Lot B1 : echeance de paiement (D1) et reglement constate. */
class BookingB1Test {

    private final BookingPaymentDue due = new BookingPaymentDue();

    @Test
    void twoHoursByDefault() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 18, 9, 0); // mercredi
        assertThat(due.dueAt(now, now.plusHours(8))).isEqualTo(now.plusHours(2));
    }

    @Test
    void aSoonSlotIsDueThirtyMinutesBeforeItsStart() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 18, 9, 0);
        assertThat(due.dueAt(now, now.plusMinutes(90))).isEqualTo(now.plusMinutes(60));
        // Deja dans les 30 dernieres minutes : reglement a l'arrivee.
        assertThat(due.dueAt(now, now.plusMinutes(20))).isEqualTo(now.plusMinutes(20));
    }

    @Test
    void aFarSlotGetsADay() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 18, 9, 0);
        assertThat(due.dueAt(now, now.plusDays(5))).isEqualTo(now.plusHours(24));
    }

    @Test
    void aWeekendDueMovesToMondayAtTen() {
        LocalDateTime friday = LocalDateTime.of(2026, 11, 20, 9, 0);
        // Echeance a 24 h : samedi 9:00, reportee au lundi 23 a 10:00.
        assertThat(due.dueAt(friday, friday.plusDays(10))).isEqualTo(LocalDateTime.of(2026, 11, 23, 10, 0));
    }

    @Test
    void paidMeansPaymentReceivedNotJustConfirmed() {
        assertThat(BookingPaymentStatus.of(BookingPaymentMode.DIRECT, BookingStatus.CONFIRMED, null)).isEqualTo(BookingPaymentStatus.PENDING);
        assertThat(BookingPaymentStatus.of(BookingPaymentMode.DIRECT, BookingStatus.CONFIRMED, Instant.now())).isEqualTo(BookingPaymentStatus.PAID);
        assertThat(BookingPaymentStatus.of(BookingPaymentMode.DIRECT, BookingStatus.EXPIRED, null)).isEqualTo(BookingPaymentStatus.CANCELLED);
        assertThat(BookingPaymentStatus.of(BookingPaymentMode.SUBSCRIPTION, BookingStatus.CONFIRMED, null)).isEqualTo(BookingPaymentStatus.COVERED);
    }
}
