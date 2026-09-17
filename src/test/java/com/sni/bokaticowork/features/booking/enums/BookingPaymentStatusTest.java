package com.sni.bokaticowork.features.booking.enums;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Ce que le client lit sur sa reservation.
 *
 * <p>Le cas qui justifie cette derivation est celui du portefeuille : les fonds sont saisis a la
 * confirmation, le debit ne vient qu'une minute plus tard, et pendant tout cet intervalle
 * rien ne permettait de dire au client que sa reservation etait payee. Il voyait « confirmee » et
 * devait deviner.</p>
 */
class BookingPaymentStatusTest {

    private static final Instant CONFIRMED_AT = Instant.parse("2026-09-17T09:00:00Z");

    @Test
    void aConfirmedWalletBookingIsPaidEvenBeforeTheBackgroundDebit() {
        assertEquals(BookingPaymentStatus.PAID, BookingPaymentStatus.of(
                BookingPaymentMode.WALLET, BookingStatus.CONFIRMED, CONFIRMED_AT));
    }

    @Test
    void aWalletBookingStillAwaitingApprovalIsNotPaid() {
        // Aucun blocage n'a encore ete pose : le blocage a lieu dans la transaction de
        // confirmation, et cette reservation n'y est pas arrivee.
        assertEquals(BookingPaymentStatus.PENDING, BookingPaymentStatus.of(
                BookingPaymentMode.WALLET, BookingStatus.PENDING_APPROVAL, null));
    }

    @Test
    void anExternalPaymentIsPendingUntilTheBookingIsConfirmed() {
        assertEquals(BookingPaymentStatus.PENDING, BookingPaymentStatus.of(
                BookingPaymentMode.DIRECT, BookingStatus.PENDING_PAYMENT, null));
        assertEquals(BookingPaymentStatus.PAID, BookingPaymentStatus.of(
                BookingPaymentMode.DIRECT, BookingStatus.CONFIRMED, CONFIRMED_AT));
    }

    @Test
    void anEntitlementBookingOwesNothing() {
        assertEquals(BookingPaymentStatus.COVERED, BookingPaymentStatus.of(
                BookingPaymentMode.SUBSCRIPTION, BookingStatus.CONFIRMED, CONFIRMED_AT));
        assertEquals(BookingPaymentStatus.COVERED, BookingPaymentStatus.of(
                BookingPaymentMode.PASS, BookingStatus.IN_PROGRESS, CONFIRMED_AT));
    }

    @Test
    void aCancelledBookingOwesNothingWhateverItWasPaidWith() {
        // Y compris apres confirmation : le blocage a ete rendu a l'annulation, donc annoncer
        // « paye » serait faux, et annoncer « en attente » serait inquietant sans raison.
        assertEquals(BookingPaymentStatus.CANCELLED, BookingPaymentStatus.of(
                BookingPaymentMode.WALLET, BookingStatus.CANCELLED, CONFIRMED_AT));
        assertEquals(BookingPaymentStatus.CANCELLED, BookingPaymentStatus.of(
                BookingPaymentMode.DIRECT, BookingStatus.REJECTED, null));
    }

    @Test
    void everyStatusCarriesItsOwnFrenchLabel() {
        assertEquals("Payé", BookingPaymentStatus.PAID.getLabel());
        assertEquals("En attente de paiement", BookingPaymentStatus.PENDING.getLabel());
        assertEquals("Couvert", BookingPaymentStatus.COVERED.getLabel());
        assertEquals("Annulé", BookingPaymentStatus.CANCELLED.getLabel());
    }
}
