package com.sni.bokaticowork.features.ressource.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.ressource.dto.response.BulkResourceOperationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Le compte rendu est le coeur des appels groupes : sur vingt ressources il est normal que deux
 * echouent, et refuser l'ensemble obligerait a les retirer une par une puis a relancer.
 */
class ResourceBulkExecutorTest {

    // Une transaction par ressource est l'affaire du TransactionTemplate · ces cas portent sur le
    // compte rendu, pas sur la propagation, que seul un essai a l'execution valide vraiment.
    private final ResourceBulkExecutor executor =
            new ResourceBulkExecutor(org.mockito.Mockito.mock(PlatformTransactionManager.class));

    @Test
    void shouldKeepGoingAfterAFailureAndReportIt() {
        BulkResourceOperationResponse response = executor.run(
                List.of("RES-1", "RES-2", "RES-3"), 20,
                code -> {
                    if ("RES-2".equals(code)) {
                        throw new ConflictException("resource availability", "des creneaux existent deja");
                    }
                    return 384;
                });

        assertThat(response.processed()).isEqualTo(2);
        assertThat(response.skipped()).isEqualTo(1);
        // RES-3 est traitee malgre l'echec de RES-2 · c'est tout l'objet du compte rendu.
        assertThat(response.results()).extracting("resourceCode")
                .containsExactly("RES-1", "RES-2", "RES-3");
        assertThat(response.results().get(1).status()).isEqualTo("SKIPPED");
        assertThat(response.results().get(1).reason()).contains("creneaux existent deja");
        assertThat(response.results().get(2).detail()).isEqualTo(384);
    }

    @Test
    void shouldDeduplicateCodesWhilePreservingOrder() {
        // Un meme code deux fois creerait, au second passage, un chevauchement avec ce que le
        // premier vient d'ecrire · l'echec serait incomprehensible pour l'appelant.
        List<String> seen = new ArrayList<>();

        BulkResourceOperationResponse response = executor.run(
                List.of("RES-2", "RES-1", "RES-2", " RES-1 "), 20,
                code -> { seen.add(code); return 1; });

        assertThat(seen).containsExactly("RES-2", "RES-1");
        assertThat(response.processed()).isEqualTo(2);
    }

    @Test
    void shouldIgnoreBlankCodes() {
        BulkResourceOperationResponse response = executor.run(
                java.util.Arrays.asList("RES-1", "", null, "  "), 20, code -> 1);

        assertThat(response.processed()).isEqualTo(1);
    }

    @Test
    void shouldRefuseAnEmptySelection() {
        assertThatThrownBy(() -> executor.run(List.of(), 20, code -> 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Au moins un code ressource");

        assertThatThrownBy(() -> executor.run(null, 20, code -> 1))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldRefuseABatchLargerThanTheLimit() {
        // Une plage d'un mois produit deja ~384 creneaux par ressource · la borne evite qu'un
        // seul appel en ecrive des dizaines de milliers.
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            codes.add("RES-" + i);
        }

        assertThatThrownBy(() -> executor.run(codes, 20, code -> 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("maximum autorise : 20");
    }

    @Test
    void shouldFallBackToTheExceptionNameWhenThereIsNoMessage() {
        BulkResourceOperationResponse response = executor.run(
                List.of("RES-1"), 20,
                code -> { throw new IllegalStateException(); });

        assertThat(response.results().getFirst().reason()).isEqualTo("IllegalStateException");
    }
}
