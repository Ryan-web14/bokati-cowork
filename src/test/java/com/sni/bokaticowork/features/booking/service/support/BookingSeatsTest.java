package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * On affiche ce qui est reserve, pas la capacite de la ressource.
 */
class BookingSeatsTest {

    private static Booking booking(int quantity, int capacity, boolean whole) {
        Resource resource = Resource.builder().code("RES-1").capacity(capacity)
                .resourceType(ResourceType.builder().code(whole ? "RTY-00001" : "RTY-00003")
                        .wholeResourceBooking(whole).bookableSlots(whole ? 1 : null).build())
                .build();
        Booking booking = new Booking();
        booking.setResource(resource);
        booking.setQuantity(quantity);
        return booking;
    }

    @Test
    @DisplayName("2 places sur un bureau partage de 4 · et non « 4 personnes »")
    void sharedDeskShowsReservedSeats() {
        BookingSeats seats = BookingSeats.of(booking(2, 4, false));

        assertThat(seats.label()).isEqualTo("2 places réservées sur 4");
        assertThat(seats.reservedSeats()).isEqualTo(2);
        assertThat(seats.resourceCapacity()).isEqualTo(4);
        assertThat(seats.wholeResource()).isFalse();
    }

    @Test
    @DisplayName("Une seule place, au singulier ; toutes les places, sans « sur »")
    void singularAndFull() {
        assertThat(BookingSeats.of(booking(1, 4, false)).label()).isEqualTo("1 place réservée sur 4");
        assertThat(BookingSeats.of(booking(4, 4, false)).label()).isEqualTo("4 places réservées");
    }

    @Test
    @DisplayName("Une salle louee entiere dit les participants et la taille de la salle")
    void wholeRoomShowsParticipants() {
        assertThat(BookingSeats.of(booking(3, 12, true)).label()).isEqualTo("Espace entier · 3 participants (12 places)");
        assertThat(BookingSeats.of(booking(1, 12, true)).label()).isEqualTo("Espace entier · 1 participant (12 places)");
    }
}
