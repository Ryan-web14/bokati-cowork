package com.sni.bokaticowork.features.booking.service.implementation;

import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.ressource.model.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingServiceImplTest {

    private final BookingServiceImpl service = new BookingServiceImpl(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
    );

    @Test
    void shouldMarkSubscriptionResourceLineAsCoveredBySubscription() {
        Booking booking = Booking.builder()
                .resource(Resource.builder().name("Bureau Prive").build())
                .paymentMode(BookingPaymentMode.SUBSCRIPTION)
                .subscriptionNumber("SUB-0001")
                .build();

        String description = ReflectionTestUtils.invokeMethod(service, "resourceLineDescription", booking);

        assertEquals("Booking resource - Bureau Prive (Abonne - couvert par abonnement SUB-0001)", description);
    }

    @Test
    void shouldMarkSubscriptionEntitlementLineAsSubscriberUsage() {
        Booking booking = Booking.builder()
                .paymentMode(BookingPaymentMode.SUBSCRIPTION)
                .entitlementCode("ENT-ACC-HOU")
                .build();

        String description = ReflectionTestUtils.invokeMethod(service, "entitlementLineDescription", booking);

        assertEquals("Abonne - consommation de l'abonnement ENT-ACC-HOU", description);
    }
}
