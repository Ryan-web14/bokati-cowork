package com.sni.bokaticowork.features.ressource.service.implementation;

import com.sni.bokaticowork.features.ressource.dto.request.ReserveResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.enums.ResourceStatus;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceAvailability;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAvailabilityRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceClosureRepository;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import com.sni.bokaticowork.features.ressource.service.support.ResourceSlotPolicy;
import com.sni.bokaticowork.features.ressource.service.support.StaffBookingOverride;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Limit;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Les creneaux du jour meme, et ceux que l'equipe peut prendre hors preavis.
 */
class SameDayAndStaffWindowsTest {

    private static final String CODE = "RES-1";

    private final ResourceAvailabilityRepository availabilities = mock(ResourceAvailabilityRepository.class);
    private final ResourceService resources = mock(ResourceService.class);
    private final ResourceAvailabilityServiceImpl service = new ResourceAvailabilityServiceImpl(availabilities,
            mock(ResourceClosureRepository.class), resources, new ResourceSlotPolicy(), new StaffBookingOverride(31), null);

    private final LocalDateTime hour = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
    private Resource resource;

    @BeforeEach
    void setUp() {
        resource = resource(true);
        when(resources.getResourceForService(CODE)).thenReturn(resource);
        when(availabilities.findExpiredSlots(any(LocalDateTime.class), any(Limit.class))).thenReturn(List.of());
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private Resource resource(boolean staffOverrideAllowed) {
        return Resource.builder().code(CODE).capacity(1).bookingEnabled(true).active(true).status(ResourceStatus.ACTIVE)
                .resourcePolicy(ResourcePolicy.builder().minBookingDurationMinutes(30).maxBookingDurationMinutes(480)
                        .minBookingNoticeMinutes(60).cancellationNoticeMinutes(60).staffOverrideAllowed(staffOverrideAllowed).build())
                .build();
    }

    private ResourceAvailability slot(LocalDateTime start, boolean open, Instant expiredAt) {
        return ResourceAvailability.builder().resource(resource).startedAt(start).endedAt(start.plusMinutes(30))
                .slotDurationMinutes(30).totalCapacity(1).remainingCapacity(1)
                .available(open).active(open).expiredAt(expiredAt).build();
    }

    private void asStaff() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("agent@sni-cg.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_OPERATIONS_AGENT"))));
    }

    private List<ResourceAvailability> daySlots() {
        List<ResourceAvailability> slots = new ArrayList<>();
        // Deux creneaux passes et restes libres, ferme par le temps ; un desactive a la main ; puis le futur.
        slots.add(slot(hour.minusHours(3), false, Instant.now()));
        slots.add(slot(hour.minusHours(2), false, null));
        slots.add(slot(hour.plusMinutes(30), true, null));
        slots.add(slot(hour.plusHours(3), true, null));
        return slots;
    }

    @Test
    @DisplayName("Jour meme : le debut de la recherche est passe, les creneaux plus tard dans la journee s'affichent")
    void sameDayWindowsShowUp() {
        LocalDateTime from = hour.minusHours(4);
        LocalDateTime to = hour.plusHours(5);
        when(availabilities.findCandidateSlots(resource, from, to)).thenReturn(daySlots());

        var windows = service.findRemainingWindows(CODE, from, to, 30, 1);

        // Le preavis d'une heure ecarte le creneau dans trente minutes, le passe n'est pas propose.
        assertThat(windows).extracting(w -> w.getStartedAt()).containsExactly(hour.plusHours(3));
    }

    @Test
    @DisplayName("L'equipe voit les creneaux passes restes libres et ceux dans le preavis, pas ceux desactives a la main")
    void staffSeesPastAndImminent() {
        asStaff();
        LocalDateTime from = hour.minusHours(4);
        LocalDateTime to = hour.plusHours(5);
        when(availabilities.findCandidateSlots(resource, from, to)).thenReturn(daySlots());

        var windows = service.findRemainingWindows(CODE, from, to, 30, 1);

        assertThat(windows).extracting(w -> w.getStartedAt())
                .containsExactly(hour.minusHours(3), hour.plusMinutes(30), hour.plusHours(3));
    }

    @Test
    @DisplayName("Politique stricte ou evenementielle : l'equipe garde le preavis")
    void strictPolicyKeepsNotice() {
        asStaff();
        resource = resource(false);
        when(resources.getResourceForService(CODE)).thenReturn(resource);
        LocalDateTime from = hour.minusHours(4);
        LocalDateTime to = hour.plusHours(5);
        when(availabilities.findCandidateSlots(resource, from, to)).thenReturn(daySlots());

        assertThat(service.findRemainingWindows(CODE, from, to, 30, 1)).extracting(w -> w.getStartedAt())
                .containsExactly(hour.plusHours(3));
    }

    @Test
    @DisplayName("Le portail ne beneficie jamais de la derogation, meme avec un compte d'equipe")
    void portalNeverOverrides() {
        asStaff();
        var request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/sni/api/v1/client/bookings/availability");
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                new org.springframework.web.context.request.ServletRequestAttributes(request));
        try {
            assertThat(new StaffBookingOverride(31).currentActor()).isEmpty();
        } finally {
            org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        }
        assertThat(new StaffBookingOverride(31).currentActor()).contains("agent@sni-cg.com");
    }

    @Test
    @DisplayName("Retenue de l'equipe : un creneau ferme par le temps se prend, pas un creneau desactive")
    void staffReservesExpiredSlot() {
        ResourceAvailability expired = slot(hour.minusHours(3), false, Instant.now());
        when(availabilities.lockAllSlotsInRange(eq(resource), eq(expired.getStartedAt()), eq(expired.getEndedAt())))
                .thenReturn(List.of(expired));

        service.reserve(ReserveResourceAvailabilityRequest.builder().resourceCode(CODE).startedAt(expired.getStartedAt())
                .endedAt(expired.getEndedAt()).quantity(1).staffOverride(true).build());

        assertThat(expired.getRemainingCapacity()).isZero();

        ResourceAvailability disabled = slot(hour.minusHours(2), false, null);
        when(availabilities.lockAllSlotsInRange(eq(resource), eq(disabled.getStartedAt()), eq(disabled.getEndedAt())))
                .thenReturn(List.of(disabled));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.reserve(ReserveResourceAvailabilityRequest.builder()
                .resourceCode(CODE).startedAt(disabled.getStartedAt()).endedAt(disabled.getEndedAt()).quantity(1)
                .staffOverride(true).build())).isInstanceOf(RuntimeException.class);
    }
}
