package com.sni.bokaticowork.features.inventory.stock.service.interfaces;

import com.sni.bokaticowork.features.inventory.stock.dto.response.StockReconciliationReportResponse;

/**
 * Verifie que les niveaux de stock correspondent a la somme des mouvements enregistres.
 *
 * <p>Filet de securite du module : le journal de mouvements est la source de verite, le niveau de
 * stock n'en est qu'un agregat. Toute divergence signale un defaut a corriger, jamais un niveau a
 * ecraser silencieusement.</p>
 */
public interface StockReconciliationService {

    /**
     * Recalcule et compare, sur tout le stock ou sur un perimetre restreint.
     *
     * @param itemCode     limite au seul article, ou null pour tous
     * @param locationCode limite au seul emplacement, ou null pour tous
     */
    StockReconciliationReportResponse reconcile(String itemCode, String locationCode);

    /**
     * Rend le rapport au format CSV, pour transmission a la comptabilite ou a un auditeur.
     */
    String reconcileCsv(String itemCode, String locationCode);
}
