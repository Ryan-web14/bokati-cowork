package com.sni.bokaticowork.features.ressource.repository;

import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAvailabilityRepository;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les requetes de l'alignement de capacite tiennent sur le schema reel.
 *
 * <p>Un test Mockito ne voit ni le JPQL ni le SQL qu'il produit · une jointure ou un nom de champ
 * faux ne casserait qu'au premier passage du worker en production. Ici, les migrations de
 * production sont appliquees et les deux requetes executees.</p>
 */
class SharedSlotCapacityQueriesIT extends PostgresIntegrationTestBase {

    @Autowired
    private ResourceAvailabilityRepository availabilityRepository;

    @Test
    @DisplayName("La recherche des creneaux hors capacite s'execute sur le schema de production")
    void misalignedSlotQueryRuns() {
        List<Long> ids = availabilityRepository.findMisalignedSharedSlotIds(LocalDateTime.now(), Limit.of(10));

        assertThat(ids).isNotNull();
    }

    @Test
    @DisplayName("Le verrouillage par identifiants s'execute sur le schema de production")
    void lockByIdsRuns() {
        assertThat(availabilityRepository.lockAllByIdIn(List.of(-1L))).isEmpty();
    }
}
