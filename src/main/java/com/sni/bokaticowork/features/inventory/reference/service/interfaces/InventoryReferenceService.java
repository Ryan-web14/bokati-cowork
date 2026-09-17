package com.sni.bokaticowork.features.inventory.reference.service.interfaces;

import com.sni.bokaticowork.features.inventory.reference.dto.InventoryOptionResponse;

import java.util.List;
import java.util.Map;

/**
 * Sert au front toutes les listes de valeurs du module, enumerations figees comme donnees vivantes.
 *
 * <p>Objectif : supprimer la saisie libre partout ou une selection est possible. Un champ qui a un
 * domaine de valeurs connu doit etre un menu deroulant alimente ici, jamais un champ texte.</p>
 */
public interface InventoryReferenceService {

    /**
     * Toutes les enumerations du module, groupees par nom de liste.
     */
    Map<String, List<InventoryOptionResponse>> enums();

    /**
     * Une seule enumeration, par nom de groupe.
     *
     * @throws com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException si le groupe est inconnu
     */
    List<InventoryOptionResponse> enumGroup(String group);

    /**
     * Les referentiels vivants utilises comme listes deroulantes : unites, categories,
     * emplacements et fournisseurs actifs.
     */
    Map<String, List<InventoryOptionResponse>> data();

    /**
     * Noms des groupes d'enumerations disponibles, pour qu'un client se branche sans connaitre le
     * detail du module.
     */
    List<String> groupNames();
}
