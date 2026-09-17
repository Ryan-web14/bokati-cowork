package com.sni.bokaticowork.features.inventory.reference.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Une option de liste deroulante servie par le backend.
 *
 * <p>Le front ne compose jamais ses propres listes : il consomme celles-ci. Cela garantit que les
 * valeurs envoyees sont toujours valides, et qu'un ajout de valeur cote serveur apparait sans
 * livraison cote client.</p>
 */
@Data
@Builder
public class InventoryOptionResponse {

    /** Valeur technique a renvoyer au backend. */
    private String value;

    /** Libelle a afficher a l'utilisateur. */
    private String label;

    /** Precision facultative affichee en second plan. */
    private String description;

    /** Faux lorsque l'option existe encore mais ne doit plus etre proposee a la saisie. */
    @Builder.Default
    private boolean selectable = true;
}
