package com.sni.bokaticowork.features.inventory.procurement.suggestion.model;

/** Ce a quoi un fournisseur est rattache. */
public enum SupplierCategoryKind {
    /** Nature d'operation (cash_operation_nature) : la categorie d'une ligne de besoin. */
    NATURE,
    /** Categorie de stock (inventory_category) : les achats de stock. */
    INVENTORY_CATEGORY,
    /** Categorie du referentiel des fournisseurs (inventory_supplier_category_ref). */
    SUPPLIER_CATEGORY
}
