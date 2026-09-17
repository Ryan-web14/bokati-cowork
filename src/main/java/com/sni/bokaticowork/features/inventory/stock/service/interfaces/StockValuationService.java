package com.sni.bokaticowork.features.inventory.stock.service.interfaces;

import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.stock.enums.ValuationMethod;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import com.sni.bokaticowork.features.inventory.stock.model.StockLevel;

import java.math.BigDecimal;

/**
 * Valorise les mouvements de stock selon la methode de l'article.
 *
 * <p>Deux methodes coexistent. Le cout moyen pondere ne cree aucune couche de cout et reproduit
 * exactement le comportement anterieur au lot 2. Le FIFO s'appuie sur des couches, et n'est actif
 * que pour les articles qui le declarent explicitement.</p>
 *
 * <p>Dans les deux cas, {@code StockLevel.averageCost} reste alimente : le tableau de bord et la
 * valeur totale du stock continuent de fonctionner sans changement.</p>
 */
public interface StockValuationService {

    /**
     * Valorise une entree et, en FIFO, ouvre la couche de cout correspondante.
     *
     * @param previousQuantity quantite avant l'entree, utilisee pour le cout moyen pondere
     * @param unitCost         cout unitaire declare, ou null si inconnu
     * @return cout unitaire a porter sur le mouvement, ou null lorsque rien n'est valorisable
     */
    Long recordEntry(InventoryItem item,
                     InventoryLocation location,
                     StockLevel level,
                     BigDecimal previousQuantity,
                     BigDecimal quantity,
                     Long unitCost,
                     String movementCode,
                     String lotNumber);

    /**
     * Valorise une sortie et, en FIFO, consomme les couches les plus anciennes.
     *
     * @param allowShortfall autorise une sortie superieure aux couches disponibles, en repli sur le
     *                       cout moyen pour le solde non couvert
     * @return cout unitaire a porter sur le mouvement, ou null lorsque rien n'est valorisable
     */
    Long recordExit(InventoryItem item,
                    InventoryLocation location,
                    StockLevel level,
                    BigDecimal quantity,
                    boolean allowShortfall);

    /**
     * Deplace la valeur d'un emplacement vers un autre.
     *
     * <p>En FIFO, les couches sont transportees avec leur cout et leur date d'entree : l'anciennete
     * du stock survit au transfert, sans quoi un simple deplacement rajeunirait tout le stock.</p>
     */
    Long recordTransfer(InventoryItem item,
                        InventoryLocation from,
                        InventoryLocation to,
                        StockLevel fromLevel,
                        StockLevel toLevel,
                        BigDecimal quantity,
                        String movementCode,
                        boolean allowShortfall);

    /**
     * Bascule un article vers une methode de valorisation, en amorcant si besoin les couches depuis
     * le stock courant.
     *
     * <p>Le cout des couches d'amorcage est le cout moyen du moment, pas un cout d'achat reel :
     * elles sont marquees comme telles pour ne pas laisser croire a une precision qui n'existe pas.</p>
     *
     * @return nombre de couches amorcees
     */
    int switchValuationMethod(InventoryItem item, ValuationMethod target);
}
