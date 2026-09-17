package com.sni.bokaticowork.features.inventory.stock.service.interfaces;

import com.sni.bokaticowork.features.inventory.stock.dto.response.LotTraceabilityResponse;

/**
 * Remontee de tracabilite d un numero de lot.
 *
 * <p>Repond en un appel aux deux questions posees le jour d un rappel produit ou d un litige
 * fournisseur : d ou vient ce lot, et ou est-il parti.</p>
 */
public interface StockTraceabilityService {

    LotTraceabilityResponse trace(String lotNumber);
}
