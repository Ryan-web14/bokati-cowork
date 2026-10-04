package com.sni.bokaticowork.core.generator.id;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L'unicite des cles primaires tient a ce calcul, pas a la base.
 *
 * <p>Elle repose sur une seule condition : deux instances qui tournent en meme temps portent des
 * identifiants de machine differents. Ce n'etait pas le cas · l'identifiant derivait de
 * {@code user.name}, identique sur tous les dynos Heroku, et la production affichait
 * {@code machine ID: 444} sur {@code web.1} comme sur {@code web.2}.</p>
 */
class GeneratorOfIdTest {

    @Test
    @DisplayName("Deux dynos differents ne portent pas le meme identifiant de machine")
    void twoDynosDoNotShareAMachineId() {
        // C'est la faille, dans sa forme la plus courte.
        long web1 = GeneratorOfId.resolveMachine(null, "web.1").machineId();
        long web2 = GeneratorOfId.resolveMachine(null, "web.2").machineId();

        assertThat(web1).isNotEqualTo(web2);
    }

    @Test
    @DisplayName("Un type de processus ne rejoint pas un autre portant le meme numero")
    void processTypesDoNotCollideOnTheSameNumber() {
        // « web.1 » et « worker.1 » auraient le meme numero · c'est le nom entier qui compte.
        assertThat(GeneratorOfId.resolveMachine(null, "web.1").machineId())
                .isNotEqualTo(GeneratorOfId.resolveMachine(null, "worker.1").machineId());
    }

    @Test
    @DisplayName("Une flotte realiste ne produit aucune collision")
    void aRealisticFleetHasNoCollision() {
        Set<Long> machineIds = new HashSet<>();
        for (String dyno : new String[]{
                "web.1", "web.2", "web.3", "web.4", "web.5",
                "worker.1", "worker.2", "scheduler.1", "release.1"}) {
            machineIds.add(GeneratorOfId.resolveMachine(null, dyno).machineId());
        }

        assertThat(machineIds).hasSize(9);
    }

    @Test
    @DisplayName("Le meme dyno garde son identifiant d'un redemarrage a l'autre")
    void theSameDynoKeepsItsMachineId() {
        assertThat(GeneratorOfId.resolveMachine(null, "web.1").machineId())
                .isEqualTo(GeneratorOfId.resolveMachine(null, "web.1").machineId());
    }

    @Test
    @DisplayName("Le reglage explicite a la priorite sur le nom du dyno")
    void theExplicitSettingWins() {
        GeneratorOfId.Resolved resolved = GeneratorOfId.resolveMachine("42", "web.1");

        assertThat(resolved.machineId()).isEqualTo(42L);
        assertThat(resolved.source()).isEqualTo(GeneratorOfId.MACHINE_ID_VARIABLE);
    }

    @ParameterizedTest
    @DisplayName("Un reglage explicite inexploitable est ignore, et le dyno reprend la main")
    @ValueSource(strings = {"pas-un-nombre", "-1", "1024", "99999", " "})
    void anUnusableSettingFallsBackToTheDyno(String configured) {
        GeneratorOfId.Resolved resolved = GeneratorOfId.resolveMachine(configured, "web.1");

        assertThat(resolved.source()).isEqualTo(GeneratorOfId.DYNO_VARIABLE);
        assertThat(resolved.machineId())
                .isEqualTo(GeneratorOfId.resolveMachine(null, "web.1").machineId());
    }

    @ParameterizedTest
    @DisplayName("Les bornes de la plage sont acceptees")
    @ValueSource(strings = {"0", "1023"})
    void boundariesAreAccepted(String configured) {
        assertThat(GeneratorOfId.resolveMachine(configured, "web.1").source())
                .isEqualTo(GeneratorOfId.MACHINE_ID_VARIABLE);
    }

    @Test
    @DisplayName("Sans rien, l'identifiant est tire au hasard dans la plage et la source le dit")
    void withoutAnythingItIsRandomButInRange() {
        GeneratorOfId.Resolved resolved = GeneratorOfId.resolveMachine(null, null);

        assertThat(resolved.machineId()).isBetween(0L, 1023L);
        assertThat(resolved.source()).isEqualTo("aleatoire");
    }

    @Test
    @DisplayName("L'identifiant de machine tient toujours dans ses dix bits")
    void everyMachineIdFitsInItsTenBits() {
        for (int i = 0; i < 500; i++) {
            assertThat(GeneratorOfId.resolveMachine(null, "web." + i).machineId())
                    .isBetween(0L, 1023L);
        }
    }

    @Test
    @DisplayName("Les identifiants produits sont uniques et croissants")
    void generatedIdsAreUniqueAndIncreasing() {
        GeneratorOfId generator = new GeneratorOfId();
        Set<Long> ids = new HashSet<>();
        long previous = 0;

        for (int i = 0; i < 20_000; i++) {
            long id = generator.generateId();
            assertThat(id).isGreaterThan(previous);
            previous = id;
            ids.add(id);
        }

        assertThat(ids).hasSize(20_000);
    }

    @Test
    @DisplayName("L'identifiant se relit · horodatage, machine et sequence")
    void anIdCanBeReadBack() {
        GeneratorOfId generator = new GeneratorOfId();
        long before = System.currentTimeMillis();

        long id = generator.generateId();

        assertThat(GeneratorOfId.extractTimestamp(id)).isGreaterThanOrEqualTo(before - 1);
        assertThat(GeneratorOfId.extractMachineId(id)).isBetween(0L, 1023L);
        assertThat(GeneratorOfId.extractSequence(id)).isBetween(0L, 4095L);
    }

    @Test
    @DisplayName("Deux instances de machines differentes ne produisent jamais la meme cle")
    void twoMachinesNeverProduceTheSameKey() {
        // Le scenario de production · deux dynos qui inserent dans la meme table au meme instant.
        // Avec le meme identifiant de machine, les deux repartaient de la sequence 0 et rendaient
        // la meme valeur. L'identifiant de machine est le seul bit qui les separe.
        long shiftedWeb1 = GeneratorOfId.resolveMachine(null, "web.1").machineId() << 12;
        long shiftedWeb2 = GeneratorOfId.resolveMachine(null, "web.2").machineId() << 12;
        long timestampPart = 1_234_567L << 22;

        assertThat(timestampPart | shiftedWeb1).isNotEqualTo(timestampPart | shiftedWeb2);
    }
}
