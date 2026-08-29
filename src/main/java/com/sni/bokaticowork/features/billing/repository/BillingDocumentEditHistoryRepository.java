package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentEditHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillingDocumentEditHistoryRepository extends JpaRepository<BillingDocumentEditHistory, Long> {

    List<BillingDocumentEditHistory> findAllByDocumentOrderByChangedAtDesc(BillingDocument document);

    List<BillingDocumentEditHistory> findAllByDocumentOrderByVersionNumberAsc(BillingDocument document);

    Optional<BillingDocumentEditHistory> findByDocumentAndVersionNumber(BillingDocument document, Integer versionNumber);

    /**
     * Numero de la derniere version d'un document, ou {@code null} s'il n'en a aucune.
     * Interroge le maximum plutot que de compter les lignes : l'historique anterieur a V211
     * a ete numerote retroactivement, et un comptage repartirait a cote de la serie existante.
     */
    @Query("SELECT MAX(h.versionNumber) FROM BillingDocumentEditHistory h WHERE h.document = :document")
    Integer findLastVersionNumber(@Param("document") BillingDocument document);

    /** Derniere version en date, celle que {@code send()} marque comme transmise. */
    Optional<BillingDocumentEditHistory> findFirstByDocumentOrderByVersionNumberDesc(BillingDocument document);
}
