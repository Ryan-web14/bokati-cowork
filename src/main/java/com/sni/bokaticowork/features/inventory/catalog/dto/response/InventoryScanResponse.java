package com.sni.bokaticowork.features.inventory.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultat de la resolution d'un code scanne.
 *
 * <p>Le terminal envoie ce qu'il a lu, sans savoir de quoi il s'agit. Le backend identifie la nature
 * de l'objet et renvoie les actions possibles, ce qui evite au client de deviner.</p>
 */
@Data
@Builder
public class InventoryScanResponse {

    /** Code tel que scanne, avant normalisation. */
    private String scannedCode;

    /** Nature reconnue : ITEM, BARCODE, PACKAGING, LOCATION, ASSET, SERIAL, LOT ou UNKNOWN. */
    private String resolvedType;

    /** Vrai lorsque le code a ete reconnu. */
    private boolean resolved;

    /** Code metier de l'objet identifie. */
    private String code;

    /** Libelle lisible de l'objet identifie. */
    private String label;

    private String itemCode;

    private String itemName;

    private String locationCode;

    /** Nombre d'unites de base representees, lorsque le code designe un conditionnement. */
    private BigDecimal quantityPerScan;

    /** Nom d'unite associee, lorsque le code en porte une. */
    private String unitCode;

    /** Actions proposees au terminal pour cet objet. */
    private List<String> suggestedActions;

    /** Message a afficher lorsque le code n'est pas reconnu, ou porte un avertissement. */
    private String message;
}
