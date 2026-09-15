package com.sni.bokaticowork.features.inventory.catalog.service.interfaces;

import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemTemplateRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryVariantGenerationRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemTemplateResponse;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryVariantGenerationResponse;

import java.util.List;

/**
 * Modeles d'articles a variantes.
 *
 * <p>Un modele decrit des axes de variation et leurs valeurs. La generation produit un article
 * complet par combinaison, ce qui evite de saisir a la main des dizaines de fiches presque
 * identiques et garantit que leurs libelles et leurs codes suivent la meme regle.</p>
 */
public interface InventoryItemTemplateService {

    InventoryItemTemplateResponse create(InventoryItemTemplateRequest request);

    InventoryItemTemplateResponse update(String templateCode, InventoryItemTemplateRequest request);

    InventoryItemTemplateResponse get(String templateCode);

    List<InventoryItemTemplateResponse> list(Boolean active);

    /**
     * Genere les variantes manquantes. Les combinaisons deja creees sont laissees intactes, ce qui
     * rend l'operation rejouable apres l'ajout d'une valeur sur un axe.
     */
    InventoryVariantGenerationResponse generateVariants(String templateCode, InventoryVariantGenerationRequest request);
}
