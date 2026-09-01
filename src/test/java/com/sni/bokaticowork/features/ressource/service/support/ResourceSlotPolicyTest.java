package com.sni.bokaticowork.features.ressource.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.ressource.model.Resource;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La duree de creneau etait une constante {@code SLOT_MINUTES = 30} redefinie a l'identique dans
 * deux classes. Ces tests fixent la seule source qui la remplace.
 */
class ResourceSlotPolicyTest {

    private final ResourceSlotPolicy policy = new ResourceSlotPolicy();

    @Test
    void shouldFallBackToThirtyWhenTheResourceCarriesNoDuration() {
        // Reprise indolore · une ressource anterieure a la migration se comporte comme avant.
        assertThat(policy.slotMinutes(resource(null))).isEqualTo(30);
        assertThat(policy.slotMinutes(null)).isEqualTo(30);
    }

    @Test
    void shouldUseTheDurationCarriedByTheResource() {
        assertThat(policy.slotMinutes(resource(15))).isEqualTo(15);
        assertThat(policy.slotMinutes(resource(60))).isEqualTo(60);
    }

    @Test
    void shouldAcceptOnlyDivisorsOfSixty() {
        // Une duree qui ne divise pas 60 casse l'alignement sur l'heure : la journee ne se
        // decoupe plus en tranches regulieres et une partie des heures devient inaccessible.
        for (int admis : new int[]{5, 10, 15, 20, 30, 60}) {
            assertThatCode(() -> policy.assertAcceptable(admis)).doesNotThrowAnyException();
        }
        for (int refuse : new int[]{7, 45, 90, 0, -30, 61}) {
            assertThatThrownBy(() -> policy.assertAcceptable(refuse))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("diviser 60");
        }
    }

    @Test
    void shouldTolerateAnAbsentDuration() {
        // Absente, la duree de la ressource s'applique · ce n'est pas une erreur de saisie.
        assertThatCode(() -> policy.assertAcceptable(null)).doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptARangeAlignedOnTheResourceSlots() {
        assertThatCode(() -> policy.assertAligned(resource(15),
                LocalDateTime.of(2026, 9, 2, 8, 15), LocalDateTime.of(2026, 9, 2, 9, 0)))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRefuseARangeThatDoesNotFallOnTheResourceSlots() {
        // 8h15 est valide en creneaux de 15 minutes, pas en creneaux de 30.
        assertThatThrownBy(() -> policy.assertAligned(resource(30),
                LocalDateTime.of(2026, 9, 2, 8, 15), LocalDateTime.of(2026, 9, 2, 9, 15)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("30 minutes");
    }

    @Test
    void shouldNameTheEffectiveDurationInTheRefusal() {
        // Un refus mentionnant 30 sur une ressource en creneaux de 15 enverrait chercher au
        // mauvais endroit.
        assertThatThrownBy(() -> policy.assertAligned(resource(15),
                LocalDateTime.of(2026, 9, 2, 8, 10), LocalDateTime.of(2026, 9, 2, 9, 0)))
                .hasMessageContaining("15 minutes");
    }

    @Test
    void shouldCountSlotsAgainstTheResourceDuration() {
        // Le decompte au coeur de la composition de fenetre · deux heures valent quatre creneaux
        // de 30, huit creneaux de 15.
        assertThat(policy.slotCount(resource(30), 120)).isEqualTo(4);
        assertThat(policy.slotCount(resource(15), 120)).isEqualTo(8);
    }

    private Resource resource(Integer slotDurationMinutes) {
        return Resource.builder()
                .code("RES-001")
                .name("Salle")
                .slotDurationMinutes(slotDurationMinutes)
                .build();
    }
}
