package com.sni.bokaticowork.features.inventory.catalog.dto.request;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Demande de generation des variantes d'un modele.
 *
 * <p>Sans selection, toutes les combinaisons actives sont generees. Avec une selection, seules les
 * valeurs listees sont retenues, ce qui evite de creer des centaines d'articles inutiles.</p>
 */
@Data
public class InventoryVariantGenerationRequest {

    /** Valeurs retenues par axe. Un axe absent prend toutes ses valeurs actives. */
    private Map<String, List<String>> selectedValues;

    /** Renvoie le resultat sans rien creer, pour verifier le volume avant de valider. */
    private Boolean dryRun;
}
