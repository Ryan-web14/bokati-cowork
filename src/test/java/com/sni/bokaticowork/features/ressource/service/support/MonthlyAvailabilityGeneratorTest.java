package com.sni.bokaticowork.features.ressource.service.support;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.enums.ResourceStatus;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAvailabilityRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceAvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le mois s'ouvre aux bons horaires, et seulement pour les ressources qui n'ont rien.
 *
 * <p>Le mercredi 28 octobre 2026 a 10:07 · il reste mercredi (des 10:30), jeudi, vendredi et
 * samedi 31. Dimanche n'existe pas dans ce reste de mois, ce qui laisse le samedi verifier ses
 * horaires courts.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MonthlyAvailabilityGeneratorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 28, 10, 7);

    @Mock private ResourceRepository resourceRepository;
    @Mock private ResourceAvailabilityRepository availabilityRepository;
    @Mock private ResourceAvailabilityService availabilityService;

    private MonthlyAvailabilityGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new MonthlyAvailabilityGenerator(resourceRepository, availabilityRepository,
                availabilityService, new ResourceSlotPolicy(), "08:00-18:00", "08:00-12:00", "");
    }

    private static Resource resource(String code) {
        return Resource.builder().code(code).capacity(10).bookingEnabled(true).active(true)
                .status(ResourceStatus.ACTIVE).build();
    }

    private List<CreateResourceAvailabilityRequest> created() {
        ArgumentCaptor<CreateResourceAvailabilityRequest> captor =
                ArgumentCaptor.forClass(CreateResourceAvailabilityRequest.class);
        verify(availabilityService, org.mockito.Mockito.atLeast(0)).createAvailability(captor.capture());
        return captor.getAllValues();
    }

    @Test
    @DisplayName("Lundi-vendredi 08:00-18:00, samedi 08:00-12:00, aujourd'hui a partir du prochain creneau")
    void opensTheRestOfTheMonthWithTheRightHours() {
        when(resourceRepository.findAll()).thenReturn(List.of(resource("RES-1")));

        MonthlyAvailabilityGenerator.Report report = generator.generate(NOW);

        List<CreateResourceAvailabilityRequest> requests = created();
        assertThat(requests).extracting(CreateResourceAvailabilityRequest::getStartedAt).containsExactly(
                LocalDateTime.of(2026, 10, 28, 10, 30),
                LocalDateTime.of(2026, 10, 29, 8, 0),
                LocalDateTime.of(2026, 10, 30, 8, 0),
                LocalDateTime.of(2026, 10, 31, 8, 0));
        assertThat(requests).extracting(CreateResourceAvailabilityRequest::getEndedAt).containsExactly(
                LocalDateTime.of(2026, 10, 28, 18, 0),
                LocalDateTime.of(2026, 10, 29, 18, 0),
                LocalDateTime.of(2026, 10, 30, 18, 0),
                LocalDateTime.of(2026, 10, 31, 12, 0));
        assertThat(requests).allMatch(r -> r.getCapacity() == null);
        assertThat(report.resourcesFilled()).isEqualTo(1);
        assertThat(report.daysCreated()).isEqualTo(4);
    }

    @Test
    @DisplayName("Une ressource qui a deja des creneaux dans le mois est sautee")
    void skipsResourcesThatAlreadyHaveSlots() {
        Resource already = resource("RES-1");
        when(resourceRepository.findAll()).thenReturn(List.of(already, resource("RES-2")));
        when(availabilityRepository.countByResourceAndStartedAtGreaterThanEqualAndEndedAtLessThanEqual(
                eq(already), any(), any())).thenReturn(12L);

        MonthlyAvailabilityGenerator.Report report = generator.generate(NOW);

        verify(availabilityService, never()).createAvailability(argThat(r -> "RES-1".equals(r.getResourceCode())));
        verify(availabilityService, times(4)).createAvailability(argThat(r -> "RES-2".equals(r.getResourceCode())));
        assertThat(report.resourcesSkipped()).isEqualTo(1);
    }

    @Test
    @DisplayName("Une ressource non reservable, inactive ou hors service n'est pas ouverte")
    void ignoresResourcesThatCannotBeBooked() {
        Resource disabled = resource("RES-OFF");
        disabled.setBookingEnabled(false);
        Resource maintenance = resource("RES-MAINT");
        maintenance.setStatus(ResourceStatus.MAINTENANCE);
        when(resourceRepository.findAll()).thenReturn(List.of(disabled, maintenance));

        generator.generate(NOW);

        verify(availabilityService, never()).createAvailability(any());
    }

    @Test
    @DisplayName("Une journee refusee (fermeture) n'empeche pas les suivantes")
    void aRefusedDayDoesNotStopTheOthers() {
        when(resourceRepository.findAll()).thenReturn(List.of(resource("RES-1")));
        doThrow(new ConflictException("resource availability", "the requested range overlaps an active closure"))
                .when(availabilityService).createAvailability(argThat(r ->
                        r.getStartedAt().toLocalDate().getDayOfMonth() == 29));

        MonthlyAvailabilityGenerator.Report report = generator.generate(NOW);

        assertThat(report.daysCreated()).isEqualTo(3);
        assertThat(report.daysRefused()).isEqualTo(1);
    }

    @Test
    @DisplayName("Apres la fermeture du jour, aujourd'hui n'est pas ouvert")
    void nothingTodayAfterClosingTime() {
        when(resourceRepository.findAll()).thenReturn(List.of(resource("RES-1")));

        generator.generate(LocalDateTime.of(2026, 10, 28, 18, 5));

        assertThat(created()).extracting(r -> r.getStartedAt().toLocalDate().getDayOfMonth())
                .containsExactly(29, 30, 31);
    }

    @Test
    @DisplayName("Le prochain debut de creneau est strictement apres maintenant")
    void nextBoundaryIsStrictlyInTheFuture() {
        assertThat(MonthlyAvailabilityGenerator.nextBoundary(LocalDateTime.of(2026, 10, 28, 10, 30), 30))
                .isEqualTo(LocalDateTime.of(2026, 10, 28, 11, 0));
        assertThat(MonthlyAvailabilityGenerator.nextBoundary(LocalDateTime.of(2026, 10, 28, 10, 7), 15))
                .isEqualTo(LocalDateTime.of(2026, 10, 28, 10, 15));
    }

    @Test
    @DisplayName("Une plage horaire mal ecrite arrete le demarrage plutot que d'ouvrir n'importe quoi")
    void rejectsMalformedHours() {
        assertThatThrownBy(() -> new MonthlyAvailabilityGenerator(resourceRepository, availabilityRepository,
                availabilityService, new ResourceSlotPolicy(), "18:00-08:00", "08:00-12:00", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
