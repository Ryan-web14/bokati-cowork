package com.sni.bokaticowork.features.notification.service.support;

import com.sni.bokaticowork.core.enums.EventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MemberNotificationEventsTest {

    @Test
    @DisplayName("Les evenements de reservation se rattachent a la preference du membre")
    void bookingEventsAreMapped() {
        assertThat(MemberNotificationEvents.of("BOOKING_CREATED")).contains(EventType.BOOKING_CONFIRMED);
        assertThat(MemberNotificationEvents.of("BOOKING_CONFIRMED")).contains(EventType.BOOKING_CONFIRMED);
        assertThat(MemberNotificationEvents.of("BOOKING_CANCELLED")).contains(EventType.BOOKING_CANCELLED);
        assertThat(MemberNotificationEvents.of("BOOKING_REJECTED")).contains(EventType.BOOKING_FAILED);
        assertThat(MemberNotificationEvents.of("MOBILE_MONEY_DEPOSIT_FAILED")).contains(EventType.PAYMENT_FAILED);
    }

    @Test
    @DisplayName("Une facture se coupe, un devis jamais")
    void invoiceButNotQuote() {
        assertThat(MemberNotificationEvents.of("BILLING_DOCUMENT_ISSUED", Map.of("documentType", "INVOICE")))
                .contains(EventType.INVOICE_ISSUED);
        assertThat(MemberNotificationEvents.of("BILLING_DOCUMENT_ISSUED", Map.of("documentType", "QUOTE"))).isEmpty();
    }

    @Test
    @DisplayName("Relances, contrats, securite et messages hors liste ne se coupent jamais")
    void mandatoryMessagesAreNeverMapped() {
        assertThat(MemberNotificationEvents.of("BILLING_DUNNING_REMINDER")).isEmpty();
        assertThat(MemberNotificationEvents.of("CONTRACT_SIGNING_REQUESTED")).isEmpty();
        assertThat(MemberNotificationEvents.of("COMPANY_SPEND_APPROVAL_REQUESTED")).isEmpty();
        assertThat(MemberNotificationEvents.of("MOBILE_MONEY_DEPOSIT_PENDING_REVIEW")).isEmpty();
        assertThat(MemberNotificationEvents.of("BOOKING_RESCHEDULED")).isEmpty();
        assertThat(MemberNotificationEvents.of(null)).isEmpty();
    }
}
