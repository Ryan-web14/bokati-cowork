package com.sni.bokaticowork.features.ressource.service.support;

import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceAvailability;
import com.sni.bokaticowork.features.ressource.model.ResourceType;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAvailabilityRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceClosureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Un creneau partage rejoint la capacite de sa ressource sans perdre ce qui est reserve.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SharedSlotCapacityAlignerTest {

    @Mock private ResourceAvailabilityRepository availabilityRepository;
    @Mock private ResourceClosureRepository closureRepository;
    @Mock private PlatformTransactionManager transactionManager;

    private SharedSlotCapacityAligner aligner;

    @BeforeEach
    void setUp() {
        aligner = new SharedSlotCapacityAligner(availabilityRepository, closureRepository, transactionManager);
        when(closureRepository.existsActiveOverlap(any(), any(), any())).thenReturn(false);
    }

    private static Resource openSpace(int capacity) {
        return Resource.builder().code("RES-OPEN").capacity(capacity)
                .resourceType(ResourceType.builder().code("RTY-00003").wholeResourceBooking(Boolean.FALSE).build())
                .build();
    }

    private static Resource meetingRoom(int seats) {
        return Resource.builder().code("RES-MEET").capacity(seats)
                .resourceType(ResourceType.builder().code("RTY-00001").wholeResourceBooking(Boolean.TRUE)
                        .bookableSlots(1).build())
                .build();
    }

    private static ResourceAvailability slot(Resource resource, int total, int remaining, boolean available) {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        return ResourceAvailability.builder().resource(resource).startedAt(start).endedAt(start.plusMinutes(30))
                .totalCapacity(total).remainingCapacity(remaining).available(available).active(true).build();
    }

    @Test
    @DisplayName("Un creneau libre a 1 sur un open space de 20 passe a 20")
    void aFreeSlotTakesTheRealCapacity() {
        ResourceAvailability slot = slot(openSpace(20), 1, 1, true);

        assertThat(aligner.align(slot)).isTrue();
        assertThat(slot.getTotalCapacity()).isEqualTo(20);
        assertThat(slot.getRemainingCapacity()).isEqualTo(20);
        assertThat(slot.getAvailable()).isTrue();
    }

    @Test
    @DisplayName("Les places deja reservees restent reservees")
    void bookedSeatsStayBooked() {
        ResourceAvailability slot = slot(openSpace(20), 5, 2, true);

        aligner.align(slot);

        assertThat(slot.getTotalCapacity()).isEqualTo(20);
        assertThat(slot.getRemainingCapacity()).isEqualTo(17);
    }

    @Test
    @DisplayName("Un creneau plein se rouvre quand la capacite augmente")
    void aFullSlotReopensWhenCapacityGrows() {
        ResourceAvailability slot = slot(openSpace(20), 1, 0, false);

        aligner.align(slot);

        assertThat(slot.getRemainingCapacity()).isEqualTo(19);
        assertThat(slot.getAvailable()).isTrue();
    }

    @Test
    @DisplayName("Un creneau plein sous une fermeture declaree reste ferme")
    void aFullSlotUnderAClosureStaysClosed() {
        ResourceAvailability slot = slot(openSpace(20), 1, 0, false);
        when(closureRepository.existsActiveOverlap(any(), any(), any())).thenReturn(true);

        aligner.align(slot);

        assertThat(slot.getTotalCapacity()).isEqualTo(20);
        assertThat(slot.getAvailable()).isFalse();
    }

    @Test
    @DisplayName("Un creneau ferme alors qu'il avait de la place n'est pas rouvert")
    void aClosedSlotIsNotReopened() {
        ResourceAvailability slot = slot(openSpace(20), 1, 1, false);

        aligner.align(slot);

        assertThat(slot.getTotalCapacity()).isEqualTo(20);
        assertThat(slot.getAvailable()).isFalse();
    }

    @Test
    @DisplayName("Une capacite abaissee ne descend pas sous ce qui est deja reserve")
    void aLoweredCapacityKeepsBookings() {
        ResourceAvailability slot = slot(openSpace(10), 20, 5, true);

        aligner.align(slot);

        assertThat(slot.getTotalCapacity()).isEqualTo(10);
        assertThat(slot.getRemainingCapacity()).isZero();
        assertThat(slot.getAvailable()).isFalse();
    }

    @Test
    @DisplayName("Une salle libre affiche ses places assises")
    void aFreeMeetingRoomShowsItsSeats() {
        ResourceAvailability slot = slot(meetingRoom(12), 1, 1, true);

        assertThat(aligner.align(slot)).isTrue();
        assertThat(slot.getTotalCapacity()).isEqualTo(12);
        assertThat(slot.getRemainingCapacity()).isEqualTo(12);
        assertThat(slot.getAvailable()).isTrue();
    }

    @Test
    @DisplayName("Une salle deja reservee le reste · son creneau passe a 12 mais avec 0 de libre")
    void aBookedMeetingRoomStaysBooked() {
        ResourceAvailability slot = slot(meetingRoom(12), 1, 0, false);

        aligner.align(slot);

        assertThat(slot.getTotalCapacity()).isEqualTo(12);
        assertThat(slot.getRemainingCapacity()).isZero();
        assertThat(slot.getAvailable()).isFalse();
    }

    @Test
    @DisplayName("Un creneau deja juste n'est pas reecrit")
    void anAlignedSlotIsNotRewritten() {
        assertThat(aligner.align(slot(openSpace(20), 20, 20, true))).isFalse();
    }
}
