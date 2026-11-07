package com.sni.bokaticowork.features.ressource.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceOpeningHours;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceOpeningHoursRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Les horaires restent globaux, et une ressource peut avoir les siens.
 */
class OpeningHoursTest {

    private final PlatformSettings settings = mock(PlatformSettings.class);
    private final ResourceOpeningHoursRepository repository = mock(ResourceOpeningHoursRepository.class);
    private final ResourceRepository resources = mock(ResourceRepository.class);
    private OpeningHoursResolver resolver;
    private ResourceOpeningHoursService service;
    private final Resource salle = Resource.builder().code("RES-1").build();

    @BeforeEach
    void setUp() {
        when(settings.text(anyString(), any())).thenAnswer(call -> call.getArgument(1));
        resolver = new OpeningHoursResolver(settings, repository, "08:00-18:00", "18:00-22:00", "08:00-12:00", "");
        service = new ResourceOpeningHoursService(resources, repository, resolver);
        when(resources.findByCode("RES-1")).thenReturn(Optional.of(salle));
    }

    @Test
    @DisplayName("Sans horaires propres, la ressource suit les horaires globaux, dimanche ferme")
    void followsGlobalHours() {
        var hours = resolver.forResource(salle);

        assertThat(hours.get(DayOfWeek.MONDAY)).hasSize(2);
        assertThat(hours.get(DayOfWeek.SATURDAY)).hasSize(1);
        assertThat(hours).doesNotContainKey(DayOfWeek.SUNDAY);
    }

    @Test
    @DisplayName("Un changement global s'applique aussitot, sans redemarrer")
    void globalChangeIsLive() {
        when(settings.text(OpeningHoursResolver.SATURDAY, "08:00-12:00")).thenReturn("09:00-13:00");

        assertThat(resolver.forResource(salle).get(DayOfWeek.SATURDAY).getFirst().open()).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    @DisplayName("Avec ses horaires, la ressource ne suit plus les globaux · un jour absent est ferme")
    void ownHoursReplaceGlobal() {
        when(repository.findAllByResourceOrderByDayOfWeekAscOpenTimeAsc(salle)).thenReturn(List.of(
                ResourceOpeningHours.builder().resource(salle).dayOfWeek(DayOfWeek.TUESDAY)
                        .openTime(LocalTime.of(10, 0)).closeTime(LocalTime.of(16, 0)).build()));

        var hours = resolver.forResource(salle);

        assertThat(hours).containsOnlyKeys(DayOfWeek.TUESDAY);
        assertThat(hours.get(DayOfWeek.TUESDAY).getFirst().close()).isEqualTo(LocalTime.of(16, 0));
        assertThat(service.get("RES-1").custom()).isTrue();
    }

    @Test
    @DisplayName("Sans horaires propres, la lecture rend les globaux et le dit")
    void viewOfGlobalHours() {
        ResourceOpeningHoursService.View view = service.get("RES-1");

        assertThat(view.custom()).isFalse();
        assertThat(view.hours()).hasSize(11);
    }

    @Test
    @DisplayName("Des plages inversees ou qui se chevauchent sont refusees")
    void incoherentRangesAreRefused() {
        assertThatThrownBy(() -> service.replace("RES-1", List.of(range(DayOfWeek.MONDAY, 18, 8)), "admin"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("avant de se fermer");
        assertThatThrownBy(() -> service.replace("RES-1", List.of(range(DayOfWeek.MONDAY, 8, 12), range(DayOfWeek.MONDAY, 11, 15)), "admin"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("chevauchent");
        assertThatThrownBy(() -> service.replace("RES-1", List.of(), "admin")).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Revenir aux horaires globaux retire ceux de la ressource")
    void clearReturnsToGlobal() {
        assertThat(service.clear("RES-1").custom()).isFalse();

        verify(repository).deleteAllByResource(salle);
    }

    private static ResourceOpeningHoursService.Range range(DayOfWeek day, int open, int close) {
        return new ResourceOpeningHoursService.Range(day, LocalTime.of(open, 0), LocalTime.of(close, 0));
    }
}
