package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.ressource.enums.ResourceStatus;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.ressource.model.ResourcePricingRule;
import com.sni.bokaticowork.features.ressource.model.ResourceType;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourcePricingRuleRepository;
import com.sni.bokaticowork.features.ressource.service.support.ResourceSlotPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Une salle se loue entière, la quantité n'y est qu'indicative.
 *
 * <p>Le modèle traitait la quantité comme un nombre de places consommées, ce qui est juste pour un
 * open space et faux pour une salle. Trois conséquences, toutes visibles pour le client : une
 * réservation à six personnes refusée, un prix multiplié par six, et une salle déjà louée qui
 * gardait des places libres pour quelqu'un d'autre.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WholeResourceBookingTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ResourcePricingRuleRepository pricingRuleRepository;

    private final ResourceSlotPolicy slotPolicy = new ResourceSlotPolicy();

    private BookingResourceGuard guard() {
        return new BookingResourceGuard(bookingRepository, slotPolicy);
    }

    // -------------------------------------------------------------------------------------
    // 1. La réservation n'est plus refusée
    // -------------------------------------------------------------------------------------

    @Test
    void acceptsSixAttendeesInATwelveSeatMeetingRoom() {
        Resource room = meetingRoom(12);

        assertDoesNotThrow(() -> guard().validateBookable(room, start(), start().plusHours(1), 6));
    }

    @Test
    void refusesMoreAttendeesThanTheRoomCanSeat() {
        Resource room = meetingRoom(12);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> guard().validateBookable(room, start(), start().plusHours(1), 20));

        assertTrue(ex.getMessage().contains("seating capacity"));
    }

    /** Un open space garde l'ancienne règle : on y prend des places, une à la fois. */
    @Test
    void stillLimitsQuantityToTheBookableSlotsOfASharedResource() {
        Resource openSpace = openSpace(8);

        assertDoesNotThrow(() -> guard().validateBookable(openSpace, start(), start().plusHours(1), 3));
        assertThrows(ConflictException.class,
                () -> guard().validateBookable(openSpace, start(), start().plusHours(1), 9));
    }

    // -------------------------------------------------------------------------------------
    // 2. Le prix est celui de la pièce
    // -------------------------------------------------------------------------------------

    @Test
    void chargesTheRoomOnceWhateverTheNumberOfAttendees() {
        Resource room = meetingRoom(12);
        BookingPricingCalculator calculator = calculatorPricedAt(room, 20000);

        BookingPricingCalculator.Price forOne = calculator.calculate(room, start(), start().plusHours(1), 1);
        BookingPricingCalculator.Price forSix = calculator.calculate(room, start(), start().plusHours(1), 6);

        assertEquals(forOne.amount(), forSix.amount());
        assertEquals(new BigDecimal("20000.0000"), forSix.amount());
    }

    @Test
    void stillChargesPerPlaceOnASharedResource() {
        Resource openSpace = openSpace(8);
        BookingPricingCalculator calculator = calculatorPricedAt(openSpace, 2000);

        BookingPricingCalculator.Price forThree = calculator.calculate(openSpace, start(), start().plusHours(1), 3);

        assertEquals(new BigDecimal("6000.0000"), forThree.amount());
    }

    private BookingPricingCalculator calculatorPricedAt(Resource resource, int hourlyPrice) {
        resource.setId(1L);
        when(pricingRuleRepository.findAllActiveByResourceId(1L)).thenReturn(List.of(
                ResourcePricingRule.builder()
                        .resource(resource)
                        .resourceBookingUnit(ResourceBookingUnit.HOUR)
                        .price(hourlyPrice)
                        .priority(0)
                        .build()));
        return new BookingPricingCalculator(pricingRuleRepository);
    }

    // -------------------------------------------------------------------------------------
    // 3. Le créneau est pris en totalité
    // -------------------------------------------------------------------------------------

    /**
     * La décision appartient au module disponibilité, qui seul connaît la capacité du créneau.
     * Ce test fige la déclaration ; l'arithmétique est vérifiée dans
     * {@code ResourceAvailabilityServiceImplTest}.
     */
    @Test
    void declaresItselfAsAWholeResourceBooking() {
        assertTrue(meetingRoom(12).isWholeResourceBooking());
        assertFalse(openSpace(8).isWholeResourceBooking());
    }

    // -------------------------------------------------------------------------------------

    private LocalDateTime start() {
        return LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
    }

    private Resource meetingRoom(int seats) {
        ResourceType type = ResourceType.builder()
                .code("RTY-00001")
                .name("Salle de reunion")
                .bookableSlots(1)
                .wholeResourceBooking(Boolean.TRUE)
                .build();
        return bookable(type, seats);
    }

    private Resource openSpace(int seats) {
        ResourceType type = ResourceType.builder()
                .code("RTY-00003")
                .name("Poste Open Space")
                .wholeResourceBooking(Boolean.FALSE)
                .build();
        return bookable(type, seats);
    }

    private Resource bookable(ResourceType type, int seats) {
        return Resource.builder()
                .code("RES-0001")
                .name("Ressource")
                .resourceType(type)
                .capacity(seats)
                .bookingEnabled(Boolean.TRUE)
                .active(Boolean.TRUE)
                .status(ResourceStatus.ACTIVE)
                .resourcePolicy(ResourcePolicy.builder()
                        .minBookingDurationMinutes(30)
                        .maxBookingDurationMinutes(480)
                        .minBookingNoticeMinutes(60)
                        .cancellationNoticeMinutes(60)
                        .build())
                .build();
    }
}
