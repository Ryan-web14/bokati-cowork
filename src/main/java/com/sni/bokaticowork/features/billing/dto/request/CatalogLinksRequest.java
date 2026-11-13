package com.sni.bokaticowork.features.billing.dto.request;

import jakarta.validation.constraints.Size;

/** Lien d'un article du catalogue vers l'inventaire et son compte de produit (lot B10) ; vide : retire. */
public record CatalogLinksRequest(@Size(max = 80) String inventoryItemCode, @Size(max = 20) String revenueAccount) {
}
