package com.sni.bokaticowork.features.inventory.stock.service.interfaces;

import com.sni.bokaticowork.features.inventory.stock.dto.response.StockJournalEntryResponse;
import com.sni.bokaticowork.features.inventory.stock.model.StockMovement;

import java.time.LocalDate;
import java.util.List;

/**
 * Traduit les mouvements de stock en ecritures comptables.
 *
 * <p>Une ecriture par mouvement valorise, jamais modifiee : une correction passe par la
 * contre-passation du mouvement, qui produit sa propre ecriture inverse.</p>
 */
public interface StockAccountingService {

    /**
     * Enregistre l'ecriture correspondant a un mouvement.
     *
     * <p>Sans effet lorsque le mouvement n'est pas valorise, ou lorsqu'il s'agit d'un transfert
     * interne : deplacer du stock d'un emplacement a un autre ne change pas la valeur du
     * patrimoine, donc ne produit aucune ecriture.</p>
     */
    void recordMovement(StockMovement movement);

    List<StockJournalEntryResponse> export(LocalDate fromDate, LocalDate toDate, String periodCode);

    /** Export au format CSV neutre, destine a un cabinet ou a un logiciel tiers. */
    String exportCsv(LocalDate fromDate, LocalDate toDate, String periodCode);
}
