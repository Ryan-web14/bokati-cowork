package com.sni.bokaticowork.features.inventory.procurement.event;

/**
 * Une fiche fournisseur d'inventaire vient d'etre creee ou modifiee (dans la transaction en cours).
 * La comptabilite s'en sert pour tenir sa fiche fournisseur a jour.
 */
public record InventorySupplierSaved(Long supplierId) {
}
