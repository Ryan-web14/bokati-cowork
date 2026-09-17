package com.sni.bokaticowork.features.inventory.stock.service.interfaces;

import com.sni.bokaticowork.features.inventory.stock.dto.request.ProductRecallRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.ProductRecallResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * Rappels produit.
 *
 * <p>Lancer un rappel gele les lots concernes puis reconstitue, depuis la tracabilite descendante,
 * la liste de ceux qui detiennent encore de la marchandise.</p>
 */
public interface ProductRecallService {

    ProductRecallResponse create(ProductRecallRequest request);

    /** Gele les lots concernes et calcule ce qui est encore en stock et ce qui est deja sorti. */
    ProductRecallResponse launch(String recallCode, String launchedBy);

    ProductRecallResponse recordRecovery(String recallCode, BigDecimal quantity);

    ProductRecallResponse close(String recallCode, String closedBy);

    /** Annule le rappel et libere les lots qu il avait geles. */
    ProductRecallResponse cancel(String recallCode, String cancelledBy);

    ProductRecallResponse get(String recallCode);

    List<ProductRecallResponse> list();
}
