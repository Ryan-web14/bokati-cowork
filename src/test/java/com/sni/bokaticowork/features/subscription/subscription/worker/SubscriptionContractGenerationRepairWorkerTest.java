package com.sni.bokaticowork.features.subscription.subscription.worker;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.subscription.addon.repository.SubscriptionAddonRepository;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.service.support.ContractGenerationEvent;
import com.sni.bokaticowork.features.subscription.subscription.service.support.ContractGenerationProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un prerequis absent ne doit pas remplir les journaux, ni faire perdre les elements a rattraper.
 */
class SubscriptionContractGenerationRepairWorkerTest {

    private static final String MISSING_BUSINESS =
            "Aucune entite exploitante n'est enregistree. Creez la fiche de l'espace.";

    private ContractGenerationProcessor processor;
    private SubscriptionRepository subscriptionRepository;
    private SubscriptionContractGenerationRepairWorker worker;

    @BeforeEach
    void setUp() {
        processor = mock(ContractGenerationProcessor.class);
        subscriptionRepository = mock(SubscriptionRepository.class);
        PassRepository passRepository = mock(PassRepository.class);
        SubscriptionAddonRepository addonRepository = mock(SubscriptionAddonRepository.class);
        when(subscriptionRepository.findIdsMissingContract(any(Instant.class), anyInt())).thenReturn(List.of());
        when(passRepository.findIdsMissingContract(any(Instant.class), anyInt())).thenReturn(List.of());
        when(addonRepository.findIdsMissingContract(any(Instant.class), anyInt())).thenReturn(List.of());

        worker = new SubscriptionContractGenerationRepairWorker(
                subscriptionRepository, passRepository, addonRepository, processor);
        ReflectionTestUtils.setField(worker, "enabled", true);
        ReflectionTestUtils.setField(worker, "batchSize", 20);
        ReflectionTestUtils.setField(worker, "minAgeSeconds", 300L);
    }

    private void subscriptionsToRepair(Long... ids) {
        when(subscriptionRepository.findIdsMissingContract(any(Instant.class), anyInt()))
                .thenReturn(List.of(ids));
    }

    @Test
    @DisplayName("Un prerequis absent n'empeche pas de retenter · l'element reste a rattraper")
    void aMissingPrerequisiteStillLeavesTheItemToRepair() {
        subscriptionsToRepair(1L);
        doThrow(new BadRequestException(MISSING_BUSINESS)).when(processor).process(any());

        worker.repairMissingContracts();
        worker.repairMissingContracts();
        worker.repairMissingContracts();

        // Rien n'est marque comme traite ni abandonne · le rattrapage reste possible.
        verify(processor, times(3)).process(any());
    }

    @Test
    @DisplayName("Le prerequis leve, le contrat se genere")
    void onceThePrerequisiteExistsTheContractIsGenerated() {
        subscriptionsToRepair(1L);
        doThrow(new BadRequestException(MISSING_BUSINESS)).when(processor).process(any());
        worker.repairMissingContracts();

        doNothing().when(processor).process(any());
        worker.repairMissingContracts();

        verify(processor, times(2)).process(new ContractGenerationEvent("SUBSCRIPTION", 1L));
    }

    @Test
    @DisplayName("Chaque element de la fournee est tente, meme apres un echec metier")
    void everyItemInTheBatchIsAttempted() {
        subscriptionsToRepair(1L, 2L, 3L);
        doThrow(new BadRequestException(MISSING_BUSINESS)).when(processor).process(any());

        worker.repairMissingContracts();

        verify(processor, times(3)).process(any());
    }

    @Test
    @DisplayName("Un echec inattendu reste un echec par element, avec sa trace")
    void anUnexpectedFailureStaysPerItem() {
        subscriptionsToRepair(1L, 2L);
        doThrow(new IllegalStateException("base indisponible")).when(processor).process(any());

        worker.repairMissingContracts();

        // Les deux sont tentes · un incident technique sur l'un ne dit rien de l'autre.
        verify(processor, times(2)).process(any());
    }

    @Test
    @DisplayName("La cause metier est retenue par type de source, et oubliee quand elle est levee")
    void theCauseIsTrackedPerSourceTypeAndForgotten() {
        subscriptionsToRepair(1L);
        doThrow(new BadRequestException(MISSING_BUSINESS)).when(processor).process(any());
        worker.repairMissingContracts();
        assertThat(announcedCauses()).containsEntry("SUBSCRIPTION", MISSING_BUSINESS);

        doNothing().when(processor).process(any());
        worker.repairMissingContracts();

        assertThat(announcedCauses()).doesNotContainKey("SUBSCRIPTION");
    }

    @Test
    @DisplayName("Un message d'echec absent ne fait pas tomber le worker")
    void aMissingMessageDoesNotBreakTheWorker() {
        subscriptionsToRepair(1L);
        doThrow(new BadRequestException(null)).when(processor).process(any());

        worker.repairMissingContracts();

        assertThat(announcedCauses()).containsEntry("SUBSCRIPTION", "cause non precisee");
    }

    @Test
    @DisplayName("Desactive, le worker ne consulte meme pas la base")
    void disabledWorkerTouchesNothing() {
        ReflectionTestUtils.setField(worker, "enabled", false);

        worker.repairMissingContracts();

        verify(processor, times(0)).process(any());
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<String, String> announcedCauses() {
        return (java.util.Map<String, String>)
                ReflectionTestUtils.getField(worker, "lastAnnouncedCause");
    }
}
