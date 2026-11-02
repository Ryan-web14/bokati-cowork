package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.ressource.enums.ResourceStatus;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import com.sni.bokaticowork.features.ressource.model.ResourceType;
import com.sni.bokaticowork.features.ressource.service.support.ResourceSlotPolicy;
import com.sni.bokaticowork.features.ressource.service.support.StaffBookingOverride;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Le controle d'une reservation · le preavis pour tous, sauf l'equipe quand la politique l'admet.
 */
class StaffBookingGuardTest {

    private final BookingResourceGuard guard = new BookingResourceGuard(mock(BookingRepository.class), new ResourceSlotPolicy(),
            new StaffBookingOverride(31));
    private final LocalDateTime hour = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);

    private static Resource resource() {
        return Resource.builder().code("RES-1").capacity(4).bookingEnabled(true).active(true).status(ResourceStatus.ACTIVE)
                .resourceType(ResourceType.builder().code("RTY-1").name("Poste").bookableSlots(4).build())
                .resourcePolicy(ResourcePolicy.builder().minBookingDurationMinutes(30).maxBookingDurationMinutes(480)
                        .minBookingNoticeMinutes(60).cancellationNoticeMinutes(60).build())
                .build();
    }

    @Test
    @DisplayName("Client : un creneau passe ou dans le preavis est refuse")
    void clientKeepsNotice() {
        assertThatThrownBy(() -> guard.validateBookable(resource(), hour.minusHours(2), hour.minusHours(1), 1))
                .isInstanceOf(ConflictException.class).hasMessageContaining("past");
        assertThatThrownBy(() -> guard.validateBookable(resource(), hour.plusMinutes(30), hour.plusMinutes(60), 1))
                .isInstanceOf(ConflictException.class).hasMessageContaining("notice");
    }

    @Test
    @DisplayName("Equipe : passe, en cours ou dans une minute, admis ; au-dela de 31 jours en arriere, refuse")
    void staffOverridesNotice() {
        assertThatCode(() -> guard.validateBookable(resource(), hour.minusHours(2), hour.minusHours(1), 1, true)).doesNotThrowAnyException();
        assertThatCode(() -> guard.validateBookable(resource(), hour, hour.plusHours(2), 1, true)).doesNotThrowAnyException();
        assertThatCode(() -> guard.validateBookable(resource(), hour.plusHours(1), hour.plusHours(2), 1, true)).doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.validateBookable(resource(), hour.minusDays(40), hour.minusDays(40).plusHours(1), 1, true))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("31 jours");
    }

    @Test
    @DisplayName("Equipe : la duree de la politique s'applique toujours")
    void staffKeepsDuration() {
        assertThatThrownBy(() -> guard.validateBookable(resource(), hour.minusHours(10), hour, 1, true))
                .isInstanceOf(ConflictException.class).hasMessageContaining("maximum");
    }
}
