package com.sni.bokaticowork.features.inventory.reference.service.implementation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Libelles francais des enumerations du module inventaire.
 *
 * <p>Cle de la forme {@code NomEnum.VALEUR}. Une valeur sans libelle declare retombe sur une
 * version lisible du nom technique, ce qui evite qu'un ajout d'enum casse une liste deroulante.</p>
 */
final class InventoryReferenceLabels {

    private static final Map<String, String> LABELS = new HashMap<>();

    private InventoryReferenceLabels() {
    }

    static String labelFor(Enum<?> value) {
        String key = value.getDeclaringClass().getSimpleName() + "." + value.name();
        String label = LABELS.get(key);
        return label != null ? label : humanize(value.name());
    }

    private static String humanize(String constant) {
        String lower = constant.replace('_', ' ').toLowerCase(Locale.ROOT);
        return lower.isEmpty() ? constant : Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static void put(String key, String label) {
        LABELS.put(key, label);
    }

    static {
        // Catalogue
        put("InventoryItemType.CONSUMABLE", "Consommable");
        put("InventoryItemType.ASSET", "Equipement");
        put("InventoryItemType.SERVICE", "Service");
        put("InventoryItemType.SPARE_PART", "Piece de rechange");

        put("InventoryTrackingType.NONE", "Aucun suivi");
        put("InventoryTrackingType.QUANTITY", "Suivi en quantite");
        put("InventoryTrackingType.SERIAL", "Suivi par numero de serie");
        put("InventoryTrackingType.LOT", "Suivi par lot");
        put("InventoryTrackingType.EXPIRY", "Suivi par date de peremption");

        put("InventoryUnitType.UNIT", "Unite");
        put("InventoryUnitType.KG", "Kilogramme");
        put("InventoryUnitType.LITER", "Litre");
        put("InventoryUnitType.BOX", "Carton");
        put("InventoryUnitType.PACK", "Paquet");
        put("InventoryUnitType.METER", "Metre");

        put("ItemLifecycleStatus.DRAFT", "Brouillon");
        put("ItemLifecycleStatus.NEW", "Nouveau");
        put("ItemLifecycleStatus.ACTIVE", "Actif");
        put("ItemLifecycleStatus.PHASE_OUT", "Fin de serie");
        put("ItemLifecycleStatus.OBSOLETE", "Obsolete");
        put("ItemLifecycleStatus.BLOCKED", "Bloque");

        put("InventoryBarcodeType.EAN13", "EAN 13");
        put("InventoryBarcodeType.EAN8", "EAN 8");
        put("InventoryBarcodeType.UPC", "UPC");
        put("InventoryBarcodeType.CODE128", "Code 128");
        put("InventoryBarcodeType.QR", "QR code");
        put("InventoryBarcodeType.INTERNAL", "Code interne");
        put("InventoryBarcodeType.SUPPLIER", "Code fournisseur");

        put("InventoryPackagingLevel.EACH", "Unite");
        put("InventoryPackagingLevel.INNER", "Sous-conditionnement");
        put("InventoryPackagingLevel.CASE", "Carton");
        put("InventoryPackagingLevel.PALLET", "Palette");

        put("InventoryPriceType.DEFAULT_COST", "Cout d'achat par defaut");
        put("InventoryPriceType.SALE_PRICE", "Prix de vente");

        // Emplacements et stock
        put("InventoryLocationType.SITE", "Site");
        put("InventoryLocationType.WAREHOUSE", "Entrepot");
        put("InventoryLocationType.ROOM", "Salle");
        put("InventoryLocationType.SHELF", "Etagere");
        put("InventoryLocationType.LOCKER", "Casier");
        put("InventoryLocationType.VIRTUAL", "Emplacement virtuel");

        put("StockMovementType.IN", "Entree");
        put("StockMovementType.OUT", "Sortie");
        put("StockMovementType.TRANSFER", "Transfert");
        put("StockMovementType.ADJUSTMENT_IN", "Ajustement positif");
        put("StockMovementType.ADJUSTMENT_OUT", "Ajustement negatif");

        put("StockOutReasonCode.CONSUMPTION", "Consommation");
        put("StockOutReasonCode.DAMAGE", "Casse");
        put("StockOutReasonCode.LOSS", "Perte");
        put("StockOutReasonCode.INTERNAL_USE", "Usage interne");
        put("StockOutReasonCode.SAMPLE", "Echantillon");
        put("StockOutReasonCode.DONATION", "Don");
        put("StockOutReasonCode.OTHER", "Autre");

        put("StockOwnershipType.COMPANY", "Propriete de l'entreprise");
        put("StockOwnershipType.CUSTOMER", "Propriete du client");
        put("StockOwnershipType.SUPPLIER", "Propriete du fournisseur");
        put("StockOwnershipType.CONSIGNMENT", "Depot en consignation");

        put("StockReferenceType.PURCHASE_ORDER", "Commande d'achat");
        put("StockReferenceType.GOODS_RECEIPT", "Reception");
        put("StockReferenceType.POS_SALE", "Vente en caisse");
        put("StockReferenceType.BOOKING", "Reservation");
        put("StockReferenceType.CONTRACT", "Contrat");
        put("StockReferenceType.MANUAL_ADJUSTMENT", "Ajustement manuel");
        put("StockReferenceType.INVENTORY_COUNT", "Inventaire physique");
        put("StockReferenceType.ASSET_ASSIGNMENT", "Affectation d'equipement");
        put("StockReferenceType.OTHER", "Autre");

        put("StockReservationStatus.ACTIVE", "Active");
        put("StockReservationStatus.CONSUMED", "Consommee");
        put("StockReservationStatus.RELEASED", "Liberee");
        put("StockReservationStatus.EXPIRED", "Expiree");
        put("StockReservationStatus.CANCELLED", "Annulee");

        put("StockTransferWorkflowStatus.REQUESTED", "Demande");
        put("StockTransferWorkflowStatus.APPROVED", "Approuve");
        put("StockTransferWorkflowStatus.SHIPPED", "Expedie");
        put("StockTransferWorkflowStatus.RECEIVED", "Receptionne");
        put("StockTransferWorkflowStatus.CANCELLED", "Annule");

        put("InventorySerialStatus.AVAILABLE", "Disponible");
        put("InventorySerialStatus.ISSUED", "Sorti");
        put("InventorySerialStatus.TRANSFERRED", "Transfere");
        put("InventorySerialStatus.LOST", "Perdu");
        put("InventorySerialStatus.DAMAGED", "Endommage");

        // Equipements
        put("AssetStatus.AVAILABLE", "Disponible");
        put("AssetStatus.RESERVED", "Reserve");
        put("AssetStatus.ASSIGNED", "Affecte");
        put("AssetStatus.IN_USE", "En service");
        put("AssetStatus.IN_MAINTENANCE", "En maintenance");
        put("AssetStatus.LOST", "Perdu");
        put("AssetStatus.RETIRED", "Reforme");
        put("AssetStatus.DAMAGED", "Endommage");

        put("AssetCondition.NEW", "Neuf");
        put("AssetCondition.GOOD", "Bon etat");
        put("AssetCondition.FAIR", "Etat moyen");
        put("AssetCondition.DAMAGED", "Endommage");
        put("AssetCondition.UNUSABLE", "Inutilisable");

        put("AssetAssigneeType.MEMBER", "Membre");
        put("AssetAssigneeType.CUSTOMER", "Client");
        put("AssetAssigneeType.BUSINESS", "Entreprise");
        put("AssetAssigneeType.USER", "Utilisateur");
        put("AssetAssigneeType.RESOURCE", "Ressource");

        put("AssetAssignmentStatus.RESERVED", "Reservee");
        put("AssetAssignmentStatus.ACTIVE", "En cours");
        put("AssetAssignmentStatus.RETURNED", "Restituee");
        put("AssetAssignmentStatus.CANCELLED", "Annulee");

        put("AssetMaintenanceType.PREVENTIVE", "Preventive");
        put("AssetMaintenanceType.CORRECTIVE", "Corrective");
        put("AssetMaintenanceType.WARRANTY", "Sous garantie");
        put("AssetMaintenanceType.INSPECTION", "Inspection");

        put("AssetMaintenanceStatus.PLANNED", "Planifiee");
        put("AssetMaintenanceStatus.IN_PROGRESS", "En cours");
        put("AssetMaintenanceStatus.COMPLETED", "Terminee");
        put("AssetMaintenanceStatus.CANCELLED", "Annulee");

        // Inventaires physiques
        put("InventoryCountStatus.DRAFT", "Brouillon");
        put("InventoryCountStatus.IN_PROGRESS", "En cours de comptage");
        put("InventoryCountStatus.REVIEW", "En revue");
        put("InventoryCountStatus.VALIDATED", "Valide");
        put("InventoryCountStatus.CANCELLED", "Annule");

        // Import
        put("InventoryImportType.ITEMS", "Articles");
        put("InventoryImportType.INITIAL_STOCK", "Stock initial");
        put("InventoryImportType.SUPPLIERS", "Fournisseurs");
        put("InventoryImportType.ASSETS", "Equipements");

        // Alertes et reapprovisionnement
        put("InventoryAlertType.LOW_STOCK", "Stock bas");
        put("InventoryAlertType.RECURRING_LOW_STOCK", "Stock bas recurrent");
        put("InventoryAlertType.OUT_OF_STOCK", "Rupture de stock");
        put("InventoryAlertType.NEGATIVE_STOCK", "Stock negatif");
        put("InventoryAlertType.OVERSTOCK", "Surstock");
        put("InventoryAlertType.SLOW_MOVING", "Rotation lente");
        put("InventoryAlertType.EXPIRY_SOON", "Peremption proche");
        put("InventoryAlertType.EXPIRY_IMMINENT", "Peremption imminente");
        put("InventoryAlertType.WARRANTY_SOON", "Fin de garantie proche");
        put("InventoryAlertType.MAINTENANCE_DUE", "Maintenance due");
        put("InventoryAlertType.ASSET_RETURN_OVERDUE", "Retour d'equipement en retard");
        put("InventoryAlertType.ASSET_RETURN_DUE_SOON", "Retour d'equipement proche");
        put("InventoryAlertType.SUSPICIOUS_ADJUSTMENT", "Ajustement suspect");
        put("InventoryAlertType.STOCK_LEVEL_DIVERGENCE", "Niveau de stock divergent du journal");

        put("InventoryAlertStatus.OPEN", "Ouverte");
        put("InventoryAlertStatus.ACKNOWLEDGED", "Prise en compte");
        put("InventoryAlertStatus.RESOLVED", "Resolue");
        put("InventoryAlertStatus.DISMISSED", "Ecartee");

        put("ConsumptionTrend.RISING", "En hausse");
        put("ConsumptionTrend.STABLE", "Stable");
        put("ConsumptionTrend.FALLING", "En baisse");

        put("ReorderSuggestionReason.NO_STOCK_LEVEL", "Aucun niveau de stock");
        put("ReorderSuggestionReason.OUT_OF_STOCK", "Rupture de stock");
        put("ReorderSuggestionReason.BELOW_MINIMUM", "Sous le minimum");
        put("ReorderSuggestionReason.AT_MINIMUM", "Au minimum");
        put("ReorderSuggestionReason.BELOW_TARGET_MAX", "Sous le maximum cible");

        put("ReorderSuggestionSeverity.CRITICAL", "Critique");
        put("ReorderSuggestionSeverity.HIGH", "Elevee");
        put("ReorderSuggestionSeverity.MEDIUM", "Moyenne");
        put("ReorderSuggestionSeverity.LOW", "Faible");

        // Achats
        put("SupplierStatus.ACTIVE", "Actif");
        put("SupplierStatus.INACTIVE", "Inactif");
        put("SupplierStatus.BLOCKED", "Bloque");

        put("PurchaseRequestStatus.DRAFT", "Brouillon");
        put("PurchaseRequestStatus.SUBMITTED", "Soumise");
        put("PurchaseRequestStatus.APPROVED", "Approuvee");
        put("PurchaseRequestStatus.REJECTED", "Rejetee");
        put("PurchaseRequestStatus.CONVERTED", "Convertie en commande");
        put("PurchaseRequestStatus.CANCELLED", "Annulee");

        put("PurchaseOrderStatus.DRAFT", "Brouillon");
        put("PurchaseOrderStatus.APPROVED", "Approuvee");
        put("PurchaseOrderStatus.ORDERED", "Commandee");
        put("PurchaseOrderStatus.PARTIALLY_RECEIVED", "Partiellement receptionnee");
        put("PurchaseOrderStatus.RECEIVED", "Receptionnee");
        put("PurchaseOrderStatus.CANCELLED", "Annulee");

        put("GoodsReceiptStatus.DRAFT", "Brouillon");
        put("GoodsReceiptStatus.POSTED", "Comptabilisee");
        put("GoodsReceiptStatus.CANCELLED", "Annulee");

        put("PurchaseApprovalLevel.NONE", "Aucune approbation");
        put("PurchaseApprovalLevel.MANAGER", "Responsable");
        put("PurchaseApprovalLevel.DIRECTOR", "Directeur");
        put("PurchaseApprovalLevel.EXECUTIVE", "Direction generale");
    }
}
