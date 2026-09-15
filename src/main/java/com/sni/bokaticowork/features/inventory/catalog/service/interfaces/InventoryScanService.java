package com.sni.bokaticowork.features.inventory.catalog.service.interfaces;

import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryScanResponse;

/**
 * Resolution d'un code lu par un terminal.
 *
 * <p>Le terminal envoie ce qu'il a scanne sans savoir de quoi il s'agit. C'est le backend qui
 * identifie la nature de l'objet et renvoie les actions possibles, ce qui evite de dupliquer la
 * logique de reconnaissance dans chaque client.</p>
 */
public interface InventoryScanService {

    InventoryScanResponse resolve(String code);
}
