# Guide Frontend/API — Module Inventaire

## Vue d'ensemble

Ce document explique comment le frontend doit utiliser les endpoints du module `inventory`.

Il couvre l'ensemble des sous-modules :

- `catalog` — articles, categories, unites de mesure
- `stock` — niveaux de stock, mouvements, reservations, lots, transferts
- `procurement` — fournisseurs, demandes d'achat, bons de commande, receptions, regles d'approbation
- `asset` — gestion des actifs individuels, affectations, maintenances
- `control` — comptages physiques d'inventaire
- `intelligence` — alertes automatiques, regles de reappro
- `admin` — tableau de bord, etiquettes, rapports
- `importer` — import massif en CSV/Excel

Base URL :

- `/sni/api/v1`

Prefixe inventaire :

- `/sni/api/v1/inventory`

---

## Concepts principaux

- `InventoryItem`
  - definit un article, consommable, actif, service ou piece detachee
- `InventoryCategory`
  - regroupe les articles par famille
- `InventoryUnit`
  - definit l'unite de mesure d'un article (piece, litre, kg, etc.)
- `InventoryLocation`
  - definit un emplacement physique ou logique (entrepot, bureau, salle, zone)
- `StockLevel`
  - represente le niveau de stock actuel par article et par emplacement
- `StockMovement`
  - trace chaque entree, sortie, transfert ou ajustement
- `StockLot`
  - lot traçable d'un article (date d'expiration, numero de lot)
- `StockReservation`
  - reserve une quantite pour un usage futur
- `StockTransferWorkflow`
  - transfert de stock avec etapes d'approbation entre emplacements
- `Supplier`
  - fournisseur d'articles
- `PurchaseRequest`
  - demande d'achat interne avant bon de commande
- `PurchaseOrder`
  - bon de commande transmis au fournisseur
- `GoodsReceipt`
  - reception physique des marchandises
- `Asset`
  - actif individuel identifiable par numero de serie ou tag
- `AssetMaintenance`
  - intervention de maintenance sur un actif
- `InventoryCount`
  - session de comptage physique d'inventaire
- `InventoryAlert`
  - alerte automatique declenchee par des seuils ou anomalies
- `InventoryReorderRule`
  - regle de reappro automatique ou de suggestion

Types d'article (`InventoryItemType`) :

- `CONSUMABLE`
- `ASSET`
- `SERVICE`
- `SPARE_PART`

Types de suivi (`InventoryTrackingType`) :

- `NONE`
- `LOT`
- `SERIAL`

Types de mouvement de stock (`StockMovementType`) :

- `IN`
- `OUT`
- `TRANSFER`
- `ADJUSTMENT_IN`
- `ADJUSTMENT_OUT`

Statuts d'actif (`AssetStatus`) :

- `AVAILABLE`
- `RESERVED`
- `ASSIGNED`
- `IN_USE`
- `IN_MAINTENANCE`
- `LOST`
- `RETIRED`
- `DAMAGED`

---

## 1. Catalogue — Articles

### Creer un article

`POST /inventory/items`

Body :

```json
{
  "itemCode": "ART-001",
  "name": "Chaise de bureau",
  "description": "Chaise ergonomique reglable",
  "categoryCode": "MOBILIER",
  "unitCode": "PIECE",
  "itemType": "ASSET",
  "trackingType": "SERIAL",
  "defaultCost": 45000,
  "salePrice": 65000,
  "taxable": true,
  "allowNegativeStock": false,
  "requiresExpiryDate": false,
  "requiresLotNumber": false,
  "requiresSerialNumber": true,
  "active": true
}
```

Reponse `201` :

```json
{
  "itemCode": "ART-001",
  "name": "Chaise de bureau",
  "description": "Chaise ergonomique reglable",
  "categoryCode": "MOBILIER",
  "categoryName": "Mobilier",
  "unitCode": "PIECE",
  "unitName": "Piece",
  "itemType": "ASSET",
  "trackingType": "SERIAL",
  "defaultCost": 45000,
  "salePrice": 65000,
  "taxable": true,
  "allowNegativeStock": false,
  "requiresExpiryDate": false,
  "requiresLotNumber": false,
  "requiresSerialNumber": true,
  "active": true
}
```

### Modifier un article

`PUT /inventory/items/{itemCode}`

Body : identique a la creation (itemCode optionnel dans le body)

### Consulter un article

`GET /inventory/items/{itemCode}`

Reponse : `InventoryItemResponse`

### Rechercher les articles

`GET /inventory/items?query=chaise&categoryCode=MOBILIER&itemType=ASSET&active=true&page=0&size=20`

Parametres :

- `query` — recherche textuelle (nom, code, SKU)
- `categoryCode` — filtrer par categorie
- `itemType` — `CONSUMABLE`, `ASSET`, `SERVICE`, `SPARE_PART`
- `active` — `true` ou `false`
- `page`, `size` — pagination

Retour : `Page<InventoryItemResponse>`

### Activer / desactiver un article

`PATCH /inventory/items/{itemCode}/activate`

`PATCH /inventory/items/{itemCode}/deactivate`

Usage frontend :

- toggle sur la fiche article
- confirmer avant desactivation si du stock existe

---

## 2. Catalogue — Categories

### Creer une categorie

`POST /inventory/categories`

Body :

```json
{
  "categoryCode": "MOBILIER",
  "name": "Mobilier",
  "description": "Tables, chaises, rangements"
}
```

### Modifier une categorie

`PUT /inventory/categories/{categoryCode}`

### Lister les categories

`GET /inventory/categories`

Reponse :

```json
[
  {
    "categoryCode": "MOBILIER",
    "name": "Mobilier",
    "description": "Tables, chaises, rangements",
    "itemCount": 12
  }
]
```

Usage frontend :

- select/dropdown dans le formulaire article
- page d'administration des categories

### Consulter une categorie

`GET /inventory/categories/{categoryCode}`

---

## 3. Catalogue — Unites de mesure

### Creer une unite

`POST /inventory/units`

Body :

```json
{
  "unitCode": "PIECE",
  "name": "Piece",
  "unitType": "COUNT",
  "symbol": "pcs"
}
```

Types d'unite (`InventoryUnitType`) :

- `COUNT`
- `WEIGHT`
- `VOLUME`
- `LENGTH`

### Lister les unites

`GET /inventory/units`

Reponse :

```json
[
  {
    "unitCode": "PIECE",
    "name": "Piece",
    "unitType": "COUNT",
    "symbol": "pcs"
  },
  {
    "unitCode": "KG",
    "name": "Kilogramme",
    "unitType": "WEIGHT",
    "symbol": "kg"
  }
]
```

Usage frontend :

- select dans le formulaire article
- page d'administration des unites avec gestion des conversions

---

## 4. Emplacements

### Creer un emplacement

`POST /inventory/locations`

Body :

```json
{
  "locationCode": "ENTREPOT-A",
  "name": "Entrepot principal",
  "locationType": "WAREHOUSE",
  "description": "Zone de stockage principale",
  "active": true
}
```

Types d'emplacement (`InventoryLocationType`) :

- `WAREHOUSE`
- `OFFICE`
- `ROOM`
- `ZONE`
- `VIRTUAL`

### Modifier un emplacement

`PUT /inventory/locations/{locationCode}`

### Lister les emplacements

`GET /inventory/locations?active=true`

Reponse :

```json
[
  {
    "locationCode": "ENTREPOT-A",
    "name": "Entrepot principal",
    "locationType": "WAREHOUSE",
    "description": "Zone de stockage principale",
    "active": true
  }
]
```

### Activer / desactiver un emplacement

`PATCH /inventory/locations/{locationCode}/activate`

`PATCH /inventory/locations/{locationCode}/deactivate`

Usage frontend :

- select dans formulaires mouvements de stock et transferts
- page d'administration des emplacements

---

## 5. Stock — Niveaux

### Consulter les niveaux de stock

`GET /inventory/stock/levels?itemCode=ART-001&locationCode=ENTREPOT-A&page=0&size=20`

Parametres :

- `itemCode` — filtrer par article
- `locationCode` — filtrer par emplacement
- `belowMinimum` — uniquement les articles sous le seuil

Reponse :

```json
[
  {
    "itemCode": "ART-001",
    "itemName": "Chaise de bureau",
    "locationCode": "ENTREPOT-A",
    "locationName": "Entrepot principal",
    "quantityOnHand": 25,
    "quantityReserved": 3,
    "quantityAvailable": 22,
    "averageCost": 45000,
    "lastMovementAt": "2026-04-10T08:30:00Z"
  }
]
```

Usage frontend :

- tableau de bord des stocks
- alertes stock bas
- page de recherche d'articles disponibles

---

## 6. Stock — Mouvements

### Entree de stock

`POST /inventory/stock/in`

Body :

```json
{
  "itemCode": "ART-001",
  "locationCode": "ENTREPOT-A",
  "quantity": 10,
  "unitCost": 45000,
  "lotNumber": "LOT-2026-04",
  "expiryDate": null,
  "quarantined": false,
  "ownershipType": "COWORK",
  "referenceType": "GOODS_RECEIPT",
  "referenceCode": "REC-202604-0001",
  "reason": "Reception commande fournisseur",
  "performedBy": "USER-001"
}
```

Reponse `201` :

```json
{
  "movementCode": "MOV-202604-000001",
  "itemCode": "ART-001",
  "itemName": "Chaise de bureau",
  "locationFromCode": null,
  "locationToCode": "ENTREPOT-A",
  "movementType": "IN",
  "quantity": 10,
  "unitCost": 45000,
  "totalCost": 450000,
  "referenceType": "GOODS_RECEIPT",
  "referenceCode": "REC-202604-0001",
  "reason": "Reception commande fournisseur",
  "lots": [],
  "performedBy": "USER-001",
  "performedAt": "2026-04-10T09:00:00Z"
}
```

### Sortie de stock

`POST /inventory/stock/out`

Body :

```json
{
  "itemCode": "ART-001",
  "locationCode": "ENTREPOT-A",
  "quantity": 2,
  "referenceType": "RESERVATION",
  "referenceCode": "RSV-202604-0001",
  "reason": "Attribution bureau salle B",
  "allowNegativeOverride": false,
  "performedBy": "USER-001"
}
```

### Transfert entre emplacements

`POST /inventory/stock/transfer`

Body :

```json
{
  "itemCode": "ART-001",
  "locationFromCode": "ENTREPOT-A",
  "locationToCode": "BUREAU-B",
  "quantity": 3,
  "reason": "Redeploiement mobilier",
  "performedBy": "USER-001"
}
```

### Ajustement de stock

`POST /inventory/stock/adjust`

Body :

```json
{
  "itemCode": "ART-001",
  "locationCode": "ENTREPOT-A",
  "quantityDelta": -2,
  "reason": "Correction apres comptage physique",
  "referenceType": "INVENTORY_COUNT",
  "referenceCode": "CNT-202604-0001",
  "performedBy": "USER-001"
}
```

Note : `quantityDelta` peut etre positif (ajout) ou negatif (correction en baisse).

### Inverser un mouvement

`POST /inventory/stock/movements/{movementCode}/reverse`

Body :

```json
{
  "reversedBy": "USER-001",
  "reversalReason": "Erreur de saisie — article non recu"
}
```

Comportement :

- cree un mouvement inverse
- le mouvement original est marque comme annule
- les niveaux de stock sont corriges

Usage frontend :

- bouton `Annuler ce mouvement` sur la fiche mouvement
- exiger une confirmation et un motif obligatoire

### Rechercher les mouvements

`GET /inventory/stock/movements?itemCode=ART-001&locationCode=ENTREPOT-A&movementType=IN&from=2026-04-01&to=2026-04-30&page=0&size=20`

Parametres :

- `itemCode`
- `locationCode`
- `movementType` — `IN`, `OUT`, `TRANSFER`, `ADJUSTMENT_IN`, `ADJUSTMENT_OUT`
- `referenceType`
- `referenceCode`
- `from`, `to` — plage de dates
- `reversed` — filtrer les mouvements inverses
- `page`, `size`

Retour : `Page<StockMovementResponse>`

---

## 7. Stock — Reservations

### Creer une reservation

`POST /inventory/stock/reservations`

Body :

```json
{
  "itemCode": "ART-001",
  "locationCode": "ENTREPOT-A",
  "quantity": 3,
  "referenceType": "BOOKING",
  "referenceCode": "BOK-2026-001",
  "reason": "Reservation espace reunion",
  "reservedBy": "USER-001"
}
```

Reponse `201` :

```json
{
  "reservationCode": "RSV-202604-000001",
  "itemCode": "ART-001",
  "itemName": "Chaise de bureau",
  "locationCode": "ENTREPOT-A",
  "locationName": "Entrepot principal",
  "quantity": 3,
  "status": "ACTIVE",
  "referenceType": "BOOKING",
  "referenceCode": "BOK-2026-001",
  "reason": "Reservation espace reunion",
  "reservedBy": "USER-001",
  "createdAt": "2026-04-10T10:00:00Z"
}
```

Statuts de reservation (`StockReservationStatus`) :

- `ACTIVE`
- `CONSUMED`
- `RELEASED`

### Liberer une reservation

`PATCH /inventory/stock/reservations/{reservationCode}/release`

### Consommer une reservation

`PATCH /inventory/stock/reservations/{reservationCode}/consume`

Comportement :

- declenche une sortie de stock
- la reservation passe a `CONSUMED`

### Rechercher les reservations

`GET /inventory/stock/reservations?itemCode=ART-001&status=ACTIVE&page=0&size=20`

Usage frontend :

- visualisation des quantites reservees vs disponibles
- panneau de gestion de reservation lors d'une reservation d'espace

---

## 8. Stock — Lots

### Consulter les lots d'un article

`GET /inventory/stock/lots?itemCode=ART-CONSOM-001&locationCode=ENTREPOT-A`

Reponse :

```json
[
  {
    "lotCode": "LOT-202604-001",
    "itemCode": "ART-CONSOM-001",
    "itemName": "Capsules cafe",
    "locationCode": "ENTREPOT-A",
    "lotNumber": "LOT-2026-04",
    "expiryDate": "2026-10-01",
    "quantityOnHand": 150,
    "quantityReserved": 20,
    "quantityAvailable": 130,
    "averageCost": 500,
    "status": "ACTIVE"
  }
]
```

Usage frontend :

- affichage des lots sur la fiche article
- alerte lots proches de l'expiration
- selection du lot lors d'une sortie manuelle

---

## 9. Stock — Transferts avec workflow

### Creer une demande de transfert

`POST /inventory/stock/transfer-workflows`

Body :

```json
{
  "itemCode": "ART-001",
  "locationFromCode": "ENTREPOT-A",
  "locationToCode": "BUREAU-B",
  "quantity": 5,
  "reason": "Reamenagement bureau",
  "requestedBy": "USER-001"
}
```

Reponse `201` :

```json
{
  "transferCode": "TRF-202604-000001",
  "itemCode": "ART-001",
  "itemName": "Chaise de bureau",
  "locationFromCode": "ENTREPOT-A",
  "locationToCode": "BUREAU-B",
  "quantity": 5,
  "status": "PENDING",
  "reason": "Reamenagement bureau",
  "requestedBy": "USER-001",
  "createdAt": "2026-04-10T11:00:00Z"
}
```

Statuts (`StockTransferWorkflowStatus`) :

- `PENDING`
- `APPROVED`
- `REJECTED`
- `IN_TRANSIT`
- `COMPLETED`
- `CANCELLED`

### Approuver un transfert

`PATCH /inventory/stock/transfer-workflows/{transferCode}/approve`

Body :

```json
{
  "approvedBy": "MANAGER-001",
  "comment": "Transfert autorise"
}
```

### Rejeter un transfert

`PATCH /inventory/stock/transfer-workflows/{transferCode}/reject`

Body :

```json
{
  "rejectedBy": "MANAGER-001",
  "reason": "Stock insuffisant prevu pour les autres bureaux"
}
```

### Demarrer / finaliser un transfert

`PATCH /inventory/stock/transfer-workflows/{transferCode}/start`

`PATCH /inventory/stock/transfer-workflows/{transferCode}/complete`

### Rechercher les transferts

`GET /inventory/stock/transfer-workflows?status=PENDING&page=0&size=20`

---

## 10. Achats — Fournisseurs

### Creer un fournisseur

`POST /inventory/procurement/suppliers`

Body :

```json
{
  "code": "FOURNISSEUR-001",
  "name": "Bureau Plus SARL",
  "email": "contact@bureauplus.cm",
  "phone": "+237691000000",
  "taxId": "M12345678A",
  "address": "Rue de la Joie, Douala",
  "status": "ACTIVE"
}
```

Statuts fournisseur (`SupplierStatus`) :

- `ACTIVE`
- `INACTIVE`
- `BLACKLISTED`

### Modifier un fournisseur

`PUT /inventory/procurement/suppliers/{supplierCode}`

### Lister les fournisseurs

`GET /inventory/procurement/suppliers?status=ACTIVE&page=0&size=20`

Reponse :

```json
[
  {
    "code": "FOURNISSEUR-001",
    "name": "Bureau Plus SARL",
    "email": "contact@bureauplus.cm",
    "phone": "+237691000000",
    "taxId": "M12345678A",
    "address": "Rue de la Joie, Douala",
    "status": "ACTIVE"
  }
]
```

### Performance d'un fournisseur

`GET /inventory/procurement/suppliers/{supplierCode}/performance`

Reponse :

```json
{
  "supplierCode": "FOURNISSEUR-001",
  "supplierName": "Bureau Plus SARL",
  "totalOrders": 12,
  "completedOrders": 10,
  "cancelledOrders": 1,
  "pendingOrders": 1,
  "totalReceipts": 10,
  "onTimeDeliveryRate": 0.85,
  "qualityAcceptanceRate": 0.92,
  "averageDeliveryDelayDays": 2.3,
  "averageUnitCost": 47000
}
```

Usage frontend :

- onglet performance sur la fiche fournisseur
- comparaison de fournisseurs dans le formulaire bon de commande

---

## 11. Achats — Demandes d'achat

### Creer une demande d'achat

`POST /inventory/procurement/purchase-requests`

Body :

```json
{
  "locationCode": "ENTREPOT-A",
  "requestedBy": "USER-001",
  "lines": [
    {
      "itemCode": "ART-001",
      "quantity": 5,
      "unitCost": 45000
    },
    {
      "itemCode": "ART-002",
      "quantity": 10,
      "unitCost": 12000
    }
  ]
}
```

Reponse `201` :

```json
{
  "requestCode": "PR-202604-000001",
  "locationCode": "ENTREPOT-A",
  "locationName": "Entrepot principal",
  "status": "DRAFT",
  "requestedBy": "USER-001",
  "approvedBy": null,
  "rejectionReason": null,
  "createdAt": "2026-04-10T12:00:00Z",
  "lines": [
    {
      "itemCode": "ART-001",
      "itemName": "Chaise de bureau",
      "orderedQuantity": 5,
      "unitCost": 45000
    }
  ]
}
```

Statuts (`PurchaseRequestStatus`) :

- `DRAFT`
- `SUBMITTED`
- `APPROVED`
- `REJECTED`
- `CONVERTED`

### Creer depuis des suggestions de reappro

`POST /inventory/procurement/purchase-requests/from-reorder-suggestions`

Body :

```json
{
  "locationCode": "ENTREPOT-A",
  "requestedBy": "USER-001",
  "suggestionCodes": ["SUGG-001", "SUGG-002"]
}
```

Comportement :

- transforme automatiquement les suggestions en lignes de demande d'achat
- ideal pour le workflow de reappro automatis

### Soumettre une demande

`PATCH /inventory/procurement/purchase-requests/{requestCode}/submit`

### Approuver une demande

`PATCH /inventory/procurement/purchase-requests/{requestCode}/approve`

Body :

```json
{
  "approvedBy": "MANAGER-001"
}
```

### Rejeter une demande

`PATCH /inventory/procurement/purchase-requests/{requestCode}/reject`

Body :

```json
{
  "approvedBy": "MANAGER-001",
  "rejectionReason": "Budget insuffisant ce mois"
}
```

### Lister les demandes d'achat

`GET /inventory/procurement/purchase-requests?status=SUBMITTED&locationCode=ENTREPOT-A&page=0&size=20`

---

## 12. Achats — Bons de commande

### Creer un bon de commande

`POST /inventory/procurement/purchase-orders`

Body :

```json
{
  "supplierCode": "FOURNISSEUR-001",
  "locationCode": "ENTREPOT-A",
  "sourceRequestCode": "PR-202604-000001",
  "expectedDeliveryDate": "2026-04-20",
  "orderedBy": "USER-001",
  "lines": [
    {
      "itemCode": "ART-001",
      "quantity": 5,
      "unitCost": 45000
    }
  ]
}
```

Reponse `201` :

```json
{
  "orderCode": "PO-202604-000001",
  "supplierCode": "FOURNISSEUR-001",
  "supplierName": "Bureau Plus SARL",
  "locationCode": "ENTREPOT-A",
  "sourceRequestCode": "PR-202604-000001",
  "status": "DRAFT",
  "approvalLevel": "NONE",
  "approvedBy": null,
  "approvedAt": null,
  "totalAmount": 225000,
  "expectedDeliveryDate": "2026-04-20",
  "orderedBy": "USER-001",
  "createdAt": "2026-04-10T13:00:00Z",
  "lines": [],
  "approvalSteps": []
}
```

Statuts (`PurchaseOrderStatus`) :

- `DRAFT`
- `PENDING_APPROVAL`
- `APPROVED`
- `ORDERED`
- `PARTIALLY_RECEIVED`
- `RECEIVED`
- `CANCELLED`

Niveaux d'approbation (`PurchaseApprovalLevel`) :

- `NONE`
- `MANAGER`
- `DIRECTOR`
- `FINANCE`

### Creer depuis une demande approuvee

`POST /inventory/procurement/purchase-requests/{requestCode}/purchase-order`

Body :

```json
{
  "supplierCode": "FOURNISSEUR-001",
  "expectedDeliveryDate": "2026-04-20",
  "orderedBy": "USER-001"
}
```

Comportement :

- copie automatiquement les lignes de la demande approuvee

### Approuver un bon de commande

`PATCH /inventory/procurement/purchase-orders/{orderCode}/approve`

Body :

```json
{
  "approvedBy": "DIRECTOR-001"
}
```

### Marquer commande transmise

`PATCH /inventory/procurement/purchase-orders/{orderCode}/mark-ordered`

Body :

```json
{
  "orderedBy": "USER-001"
}
```

### Lister les bons de commande

`GET /inventory/procurement/purchase-orders?status=ORDERED&supplierCode=FOURNISSEUR-001&page=0&size=20`

---

## 13. Achats — Reception de marchandises

### Recevoir des marchandises

`POST /inventory/procurement/goods-receipts`

Body :

```json
{
  "orderCode": "PO-202604-000001",
  "locationCode": "ENTREPOT-A",
  "receivedBy": "USER-001",
  "invoiceDocumentCode": "DOC-202604-000010",
  "deliveryNoteDocumentCode": "DOC-202604-000011",
  "proofDocumentCode": null,
  "lines": [
    {
      "itemCode": "ART-001",
      "receivedQuantity": 5,
      "rejectedQuantity": 0,
      "qualityAccepted": true,
      "quarantineAcceptedStock": false,
      "unitCost": 45000,
      "lotNumber": null,
      "expiryDate": null
    }
  ]
}
```

Reponse `201` :

```json
{
  "receiptCode": "REC-202604-000001",
  "orderCode": "PO-202604-000001",
  "locationCode": "ENTREPOT-A",
  "locationName": "Entrepot principal",
  "status": "POSTED",
  "invoiceDocumentCode": "DOC-202604-000010",
  "deliveryNoteDocumentCode": "DOC-202604-000011",
  "proofDocumentCode": null,
  "receivedBy": "USER-001",
  "receivedAt": "2026-04-10T14:00:00Z",
  "postedAt": "2026-04-10T14:00:00Z",
  "lines": []
}
```

Statuts (`GoodsReceiptStatus`) :

- `DRAFT`
- `POSTED`
- `CANCELLED`

Comportement apres validation :

- cree automatiquement des entrees de stock (`StockMovement IN`) pour les articles recus
- les quantites rejetees ne sont pas integrees au stock
- le statut du bon de commande passe a `PARTIALLY_RECEIVED` ou `RECEIVED`

### Lister les receptions

`GET /inventory/procurement/goods-receipts?orderCode=PO-202604-000001&page=0&size=20`

---

## 14. Achats — Regles d'approbation

### Configurer les regles

`POST /inventory/procurement/approval-rules`

Body :

```json
[
  {
    "approvalLevel": "MANAGER",
    "minAmount": 0,
    "maxAmount": 500000,
    "requiredApprovals": 1,
    "active": true
  },
  {
    "approvalLevel": "DIRECTOR",
    "minAmount": 500001,
    "maxAmount": 2000000,
    "requiredApprovals": 1,
    "active": true
  },
  {
    "approvalLevel": "FINANCE",
    "minAmount": 2000001,
    "maxAmount": null,
    "requiredApprovals": 2,
    "active": true
  }
]
```

### Lister les regles

`GET /inventory/procurement/approval-rules`

Usage frontend :

- page de configuration des seuils d'approbation
- afficher le niveau requis en temps reel lors de la creation d'un bon de commande selon le montant saisi

---

## 15. Actifs

### Creer un actif

`POST /inventory/assets`

Body :

```json
{
  "assetCode": "AST-001",
  "itemCode": "ART-001",
  "serialNumber": "SN-20260410-001",
  "assetTag": "TAG-001",
  "status": "AVAILABLE",
  "condition": "NEW",
  "locationCode": "ENTREPOT-A",
  "purchaseDate": "2026-04-10",
  "purchaseCost": 45000,
  "warrantyEndDate": "2028-04-10",
  "usefulLifeMonths": 60,
  "residualValue": 5000,
  "notes": "Chaise ergonomique noire, bureau 3"
}
```

Reponse `201` :

```json
{
  "assetCode": "AST-001",
  "itemCode": "ART-001",
  "itemName": "Chaise de bureau",
  "serialNumber": "SN-20260410-001",
  "assetTag": "TAG-001",
  "status": "AVAILABLE",
  "condition": "NEW",
  "locationCode": "ENTREPOT-A",
  "locationName": "Entrepot principal",
  "assignedToType": null,
  "assignedToCode": null,
  "purchaseDate": "2026-04-10",
  "purchaseCost": 45000,
  "warrantyEndDate": "2028-04-10",
  "usefulLifeMonths": 60,
  "residualValue": 5000,
  "depreciatedValue": 45000,
  "notes": "Chaise ergonomique noire, bureau 3"
}
```

Etat de la condition (`AssetCondition`) :

- `NEW`
- `GOOD`
- `FAIR`
- `POOR`
- `DAMAGED`

### Modifier un actif

`PUT /inventory/assets/{assetCode}`

### Consulter un actif

`GET /inventory/assets/{assetCode}`

### Rechercher les actifs

`GET /inventory/assets?itemCode=ART-001&status=AVAILABLE&locationCode=ENTREPOT-A&page=0&size=20`

Parametres :

- `itemCode`
- `status`
- `locationCode`
- `assignedToCode`
- `condition`

### Affecter un actif

`PATCH /inventory/assets/{assetCode}/assign`

Body :

```json
{
  "assigneeType": "MEMBER",
  "assigneeCode": "MBR-000001",
  "assignedBy": "MANAGER-001",
  "notes": "Attribution poste fixe salle B"
}
```

Types d'affectataire (`AssetAssigneeType`) :

- `MEMBER`
- `CUSTOMER`
- `BUSINESS`
- `ROOM`
- `LOCATION`

### Reserver un actif

`PATCH /inventory/assets/{assetCode}/reserve`

Body :

```json
{
  "reservedFor": "MBR-000002",
  "reason": "Reservation pour arrivee prochaine"
}
```

### Annuler une reservation

`PATCH /inventory/assets/{assetCode}/cancel-reservation`

### Retourner un actif

`PATCH /inventory/assets/{assetCode}/return`

Body :

```json
{
  "returnedBy": "MANAGER-001",
  "returnedToLocationCode": "ENTREPOT-A",
  "condition": "GOOD",
  "notes": "Retour en bon etat"
}
```

### Marquer perdu / endommage / retire

`PATCH /inventory/assets/{assetCode}/mark-lost`

`PATCH /inventory/assets/{assetCode}/mark-damaged`

`PATCH /inventory/assets/{assetCode}/retire`

Body pour chaque :

```json
{
  "reason": "Cause detaillee",
  "reportedBy": "USER-001"
}
```

### Historique de localisation

`GET /inventory/assets/{assetCode}/location-history`

Reponse :

```json
[
  {
    "locationCode": "ENTREPOT-A",
    "locationName": "Entrepot principal",
    "movedAt": "2026-04-01T08:00:00Z",
    "movedBy": "USER-001"
  },
  {
    "locationCode": "BUREAU-B",
    "locationName": "Bureau coworking B",
    "movedAt": "2026-04-10T09:00:00Z",
    "movedBy": "USER-001"
  }
]
```

---

## 16. Actifs — Maintenances

### Creer une demande de maintenance

`POST /inventory/asset-maintenances`

Body :

```json
{
  "assetCode": "AST-001",
  "maintenanceType": "CORRECTIVE",
  "description": "Roulettes avant cassees",
  "scheduledDate": "2026-04-15",
  "requestedBy": "USER-001"
}
```

Types de maintenance (`AssetMaintenanceType`) :

- `PREVENTIVE`
- `CORRECTIVE`
- `INSPECTION`

### Finaliser une maintenance

`PATCH /inventory/asset-maintenances/{maintenanceCode}/complete`

Body :

```json
{
  "completedBy": "TECHNICIEN-001",
  "resolutionNotes": "Roulettes remplacees, fonctionnel",
  "maintenanceCost": 5000,
  "newCondition": "GOOD"
}
```

Statuts (`AssetMaintenanceStatus`) :

- `SCHEDULED`
- `IN_PROGRESS`
- `COMPLETED`
- `CANCELLED`

### Lister les maintenances

`GET /inventory/asset-maintenances?assetCode=AST-001&status=SCHEDULED&page=0&size=20`

Usage frontend :

- onglet maintenance sur la fiche actif
- tableau de bord technicien

---

## 17. Comptage physique

### Creer une session de comptage

`POST /inventory/counts`

Body :

```json
{
  "locationCode": "ENTREPOT-A",
  "createdBy": "MANAGER-001",
  "notes": "Comptage mensuel avril 2026"
}
```

Reponse `201` :

```json
{
  "countCode": "CNT-202604-000001",
  "locationCode": "ENTREPOT-A",
  "locationName": "Entrepot principal",
  "status": "DRAFT",
  "createdBy": "MANAGER-001",
  "notes": "Comptage mensuel avril 2026",
  "startedAt": null,
  "reviewedAt": null,
  "validatedAt": null,
  "createdAt": "2026-04-10T15:00:00Z",
  "items": []
}
```

Statuts (`InventoryCountStatus`) :

- `DRAFT`
- `IN_PROGRESS`
- `UNDER_REVIEW`
- `VALIDATED`
- `CANCELLED`

### Demarrer le comptage

`PATCH /inventory/counts/{countCode}/start`

Comportement :

- charge automatiquement toutes les references de stock de l'emplacement
- genere les lignes avec les quantites theoriques du systeme

### Saisir une ligne comptee

`PUT /inventory/counts/{countCode}/lines`

Body :

```json
{
  "itemCode": "ART-001",
  "countedQuantity": 23
}
```

Comportement :

- l'ecart entre `systemQuantity` et `countedQuantity` est calcule
- les lignes peuvent etre soumises en boucle pendant toute la session

### Envoyer en revue

`PATCH /inventory/counts/{countCode}/review`

### Valider le comptage

`PATCH /inventory/counts/{countCode}/validate`

Comportement :

- applique les ajustements de stock pour chaque ecart
- cree des `StockMovement` de type `ADJUSTMENT_IN` ou `ADJUSTMENT_OUT`
- met a jour les niveaux de stock

### Annuler le comptage

`PATCH /inventory/counts/{countCode}/cancel`

### Consulter un comptage

`GET /inventory/counts/{countCode}`

Reponse :

```json
{
  "countCode": "CNT-202604-000001",
  "locationCode": "ENTREPOT-A",
  "locationName": "Entrepot principal",
  "status": "VALIDATED",
  "createdBy": "MANAGER-001",
  "notes": "Comptage mensuel avril 2026",
  "startedAt": "2026-04-10T15:05:00Z",
  "reviewedAt": "2026-04-10T17:00:00Z",
  "validatedAt": "2026-04-11T09:00:00Z",
  "createdAt": "2026-04-10T15:00:00Z",
  "items": [
    {
      "itemCode": "ART-001",
      "itemName": "Chaise de bureau",
      "systemQuantity": 25,
      "countedQuantity": 23,
      "variance": -2,
      "adjustmentApplied": true
    }
  ]
}
```

### Lister les comptages

`GET /inventory/counts?status=IN_PROGRESS&locationCode=ENTREPOT-A&page=0&size=20`

---

## 18. Alertes intelligentes

### Lister les alertes

`GET /inventory/alerts?status=OPEN&alertType=LOW_STOCK&page=0&size=20`

Parametres :

- `status` — `OPEN`, `ACKNOWLEDGED`, `RESOLVED`, `DISMISSED`
- `alertType`
- `itemCode`
- `locationCode`

Reponse :

```json
[
  {
    "alertCode": "ALT-202604-000001",
    "alertType": "LOW_STOCK",
    "status": "OPEN",
    "itemCode": "ART-CONSOM-001",
    "itemName": "Capsules cafe",
    "locationCode": "OFFICE-A",
    "locationName": "Bureau A",
    "assetCode": null,
    "currentQuantity": 5,
    "thresholdQuantity": 20,
    "message": "Stock bas : 5 unites restantes, seuil minimum 20",
    "createdAt": "2026-04-10T06:00:00Z",
    "acknowledgedAt": null,
    "resolvedAt": null
  }
]
```

Types d'alerte (`InventoryAlertType`) :

- `LOW_STOCK`
- `OUT_OF_STOCK`
- `NEGATIVE_STOCK`
- `EXPIRY_SOON`
- `LOT_EXPIRED`
- `ASSET_WARRANTY_EXPIRING`
- `ASSET_MAINTENANCE_DUE`

Statuts (`InventoryAlertStatus`) :

- `OPEN`
- `ACKNOWLEDGED`
- `RESOLVED`
- `DISMISSED`

### Accuser reception

`PATCH /inventory/alerts/{alertCode}/acknowledge`

Body :

```json
{
  "acknowledgedBy": "USER-001"
}
```

### Resoudre une alerte

`PATCH /inventory/alerts/{alertCode}/resolve`

Body :

```json
{
  "resolvedBy": "USER-001",
  "resolutionNote": "Stock reapprovisionne"
}
```

### Ignorer une alerte

`PATCH /inventory/alerts/{alertCode}/dismiss`

Usage frontend :

- widget alertes dans le tableau de bord
- centre de notifications avec tri par type
- badge counter sur l'icone inventaire

---

## 19. Regles de reappro

### Creer une regle

`POST /inventory/reorder-rules`

Body :

```json
{
  "itemCode": "ART-CONSOM-001",
  "locationCode": "OFFICE-A",
  "minimumQuantity": 20,
  "reorderQuantity": 100,
  "active": true
}
```

### Modifier une regle

`PUT /inventory/reorder-rules/{ruleCode}`

### Lister les regles

`GET /inventory/reorder-rules?itemCode=ART-CONSOM-001&active=true`

Reponse :

```json
[
  {
    "ruleCode": "ROR-001",
    "itemCode": "ART-CONSOM-001",
    "itemName": "Capsules cafe",
    "locationCode": "OFFICE-A",
    "locationName": "Bureau A",
    "minimumQuantity": 20,
    "reorderQuantity": 100,
    "active": true
  }
]
```

### Obtenir les suggestions de reappro

`GET /inventory/reorder-rules/suggestions`

Reponse :

```json
[
  {
    "suggestionCode": "SUGG-001",
    "itemCode": "ART-CONSOM-001",
    "itemName": "Capsules cafe",
    "locationCode": "OFFICE-A",
    "currentQuantity": 5,
    "minimumQuantity": 20,
    "suggestedOrderQuantity": 100,
    "estimatedCost": 50000
  }
]
```

Usage frontend :

- bouton `Creer une demande d'achat depuis les suggestions`
- widget suggestions de reappro dans le tableau de bord achat

---

## 20. Administration — Tableau de bord

### Obtenir les metriques

`GET /inventory/admin/dashboard`

Reponse :

```json
{
  "openAlerts": 7,
  "lowStockAlerts": 3,
  "outOfStockAlerts": 1,
  "negativeStockAlerts": 0,
  "expirySoonAlerts": 3,
  "expiringLots30Days": 5,
  "outOfStockLevels": 2,
  "openPurchaseOrders": 4,
  "totalStockValue": 12500000
}
```

Usage frontend :

- page d'accueil du module inventaire
- widgets KPI en haut du tableau de bord

---

## 21. Administration — Etiquettes

### Obtenir une etiquette

`GET /inventory/admin/labels/{type}/{code}`

Types (`type`) :

- `item`
- `asset`
- `location`
- `lot`

Exemple :

`GET /inventory/admin/labels/asset/AST-001`

Reponse :

```json
{
  "type": "asset",
  "code": "AST-001",
  "name": "Chaise de bureau",
  "barcode": "AST-001",
  "qrContent": "AST-001",
  "metadata": {
    "serialNumber": "SN-20260410-001",
    "assetTag": "TAG-001",
    "locationName": "Entrepot principal"
  }
}
```

Usage frontend :

- generation d'etiquettes imprimables (code QR ou code barre)
- action `Imprimer etiquette` sur la fiche actif ou article

---

## 22. Administration — Rapports

### Rapport de mouvements (JSON)

`GET /inventory/admin/reports/movements?itemCode=ART-001&locationCode=ENTREPOT-A&from=2026-04-01&to=2026-04-30`

### Export CSV

`GET /inventory/admin/reports/movements.csv?from=2026-04-01&to=2026-04-30`

Comportement :

- retourne un fichier `.csv` en attachment

Usage frontend :

- bouton `Exporter CSV` dans la page rapports

### Export PDF

`GET /inventory/admin/reports/movements.pdf?from=2026-04-01&to=2026-04-30`

Comportement :

- retourne un fichier `.pdf` en attachment

### Rapport d'anomalies

`GET /inventory/admin/reports/anomalies`

Reponse :

```json
{
  "negativeStockItems": [],
  "missingLotItems": [],
  "expiredLots": [],
  "orphanAssets": [],
  "generatedAt": "2026-04-10T16:00:00Z"
}
```

---

## 23. Import massif

### Importer des articles

`POST /inventory/import/items`

Type de contenu : `multipart/form-data`

Champs :

- `file` — fichier CSV ou Excel

### Importer des actifs

`POST /inventory/import/assets`

### Importer des niveaux de stock initiaux

`POST /inventory/import/stock`

### Telecharger un template d'import

`GET /inventory/import/template/{type}`

Types (`type`) :

- `items`
- `assets`
- `stock`

Comportement : retourne un fichier CSV avec les colonnes attendues

Reponse d'import :

```json
{
  "importType": "ITEMS",
  "totalRows": 50,
  "successCount": 48,
  "errorCount": 2,
  "rows": [
    {
      "rowNumber": 3,
      "status": "ERROR",
      "message": "Code article deja existant : ART-001",
      "data": {}
    }
  ]
}
```

Usage frontend :

- page d'import avec upload de fichier
- affichage du rapport ligne par ligne
- export des erreurs en CSV

---

## Workflow frontend recommande

### Mise en stock initiale

1. Creer les categories manquantes.
2. Creer les unites manquantes.
3. Creer les articles.
4. Creer les emplacements.
5. Configurer les regles de reappro par article.
6. Effectuer des entrees de stock (`POST /inventory/stock/in`).
7. Verifier les niveaux de stock (`GET /inventory/stock/levels`).

### Cycle d'achat complet

1. Verifier les alertes et suggestions de reappro.
2. Creer une demande d'achat (manuelle ou depuis suggestions).
3. Soumettre la demande.
4. Le manager approuve ou rejette.
5. Creer le bon de commande depuis la demande approuvee.
6. Approuver le bon de commande si le montant necessite approbation.
7. Marquer le bon de commande comme commande transmise.
8. Recevoir les marchandises via `POST /inventory/procurement/goods-receipts`.
9. Verifier les niveaux mis a jour automatiquement.

### Cycle de vie d'un actif

1. Creer l'actif depuis une fiche article de type `ASSET`.
2. L'actif est `AVAILABLE` et rattache a un emplacement.
3. Affecter l'actif a un membre, espace ou emplacement.
4. Si probleme, creer une maintenance.
5. Apres maintenance, retourner l'actif et changer son statut.
6. En fin de vie, retirer l'actif (`retire`) ou le marquer `LOST`.

### Comptage physique

1. Creer une session de comptage pour un emplacement.
2. Demarrer la session.
3. Saisir les quantites comptees ligne par ligne.
4. Envoyer en revue si une validation supervisor est requise.
5. Valider — les ajustements de stock sont appliques automatiquement.

### Gestion des alertes

1. Charger les alertes ouvertes depuis le tableau de bord.
2. Accuser reception des alertes reconnues.
3. Pour les alertes `LOW_STOCK`, creer une demande d'achat.
4. Apres approvisionnement, l'alerte se resout automatiquement ou manuellement.

---

## Ecrans recommandes pour le frontend

### 1. Tableau de bord inventaire

Usage :

- vue d'ensemble de tout le module
- entree principale pour gestionnaires et operateurs

Contenu recommande :

- KPIs :
  - nombre d'alertes ouvertes
  - articles en rupture de stock
  - valeur totale du stock
  - bons de commande ouverts
  - actifs disponibles
- graphiques :
  - evolution des mouvements sur 30 jours
  - repartition du stock par categorie
- widgets :
  - alertes recentes
  - suggestions de reappro
  - receptions en attente

Presentation suggeree :

- layout en grille 3 colonnes desktop
- KPIs en haut en ligne
- graphiques au centre
- alertes et suggestions en colonne droite

---

### 2. Catalogue articles

Usage :

- lister, rechercher et gerer les articles

Contenu recommande :

- barre de recherche avec filtres (categorie, type, statut actif)
- tableau :
  - code article
  - nom
  - categorie
  - type
  - unite
  - statut actif
  - stock total (optionnel)
  - actions
- bouton `Nouvel article`
- import CSV

Formulaire article :

- section identite : code, nom, description, SKU
- section classification : categorie, type, unite, type de suivi
- section pricing : cout par defaut, prix de vente, taxable
- section comportement : autoriser stock negatif, requiert lot, requiert expiration, requiert numero de serie

---

### 3. Niveaux de stock

Usage :

- visualiser le stock disponible par article et par emplacement

Contenu recommande :

- filtres : emplacement, article, categorie, alertes seulement
- tableau :
  - article
  - emplacement
  - quantite en stock
  - quantite reservee
  - quantite disponible
  - cout moyen
  - dernier mouvement
  - indicateur visuel (vert/orange/rouge)

Statuts visuels recommandes :

- vert : stock normal
- orange : stock bas (proche du seuil minimum)
- rouge : rupture de stock ou stock negatif

---

### 4. Historique des mouvements

Usage :

- tracer tous les flux de stock

Contenu recommande :

- filtres : article, emplacement, type de mouvement, plage de dates, reference
- tableau :
  - code mouvement
  - date
  - type
  - article
  - emplacement
  - quantite
  - cout unitaire
  - reference
  - operateur
  - badge annule si inverse
  - action `Voir detail` et `Inverser`

---

### 5. Entree de stock manuelle

Usage :

- saisir une entree hors reception fournisseur

Contenu recommande :

- formulaire :
  - article (autocomplete)
  - emplacement
  - quantite
  - cout unitaire
  - lot et date d'expiration (si requis par l'article)
  - numeros de serie (multi-saisie si actif)
  - reference externe
  - motif
- afficher en temps reel : `Stock actuel : X → X + quantite saisie`

---

### 6. Sortie de stock manuelle

Usage :

- saisir une sortie hors workflow automatique

Contenu recommande :

- formulaire identique a l'entree mais en retrait
- afficher le stock disponible apres confirmation
- avertissement si stock insuffisant

---

### 7. Gestion des fournisseurs

Usage :

- repertoire fournisseurs

Contenu recommande :

- liste avec recherche et filtre par statut
- fiche fournisseur :
  - informations de contact
  - statut
  - onglet `Commandes`
  - onglet `Performance`
  - onglet `Receptions`

---

### 8. Demandes d'achat

Usage :

- creer et suivre les demandes internes d'achat

Contenu recommande :

- liste avec filtres : statut, emplacement, demandeur, periode
- tableau :
  - code demande
  - emplacement
  - statut (badge colore)
  - demandeur
  - date
  - montant total
  - actions
- formulaire creation :
  - emplacement
  - lignes d'articles (autocomplete + quantite + cout)
  - bouton `Ajouter une ligne`

Parcours actions :

- `Soumettre` — si statut `DRAFT`
- `Approuver` — si statut `SUBMITTED` et role manager
- `Rejeter` — si statut `SUBMITTED`
- `Creer un bon de commande` — si statut `APPROVED`

---

### 9. Bons de commande

Usage :

- gerer les bons envoyes aux fournisseurs

Contenu recommande :

- liste avec filtres : statut, fournisseur, periode
- detail bon de commande :
  - recap fournisseur, emplacement, date livraison prevue
  - lignes de commande
  - historique des etapes d'approbation
  - montant total avec indicateur du niveau d'approbation requis
- bouton `Creer une reception` si statut `ORDERED` ou `PARTIALLY_RECEIVED`

---

### 10. Reception de marchandises

Usage :

- enregistrer la reception physique d'une commande

Contenu recommande :

- formulaire :
  - selection du bon de commande (autocomplete avec filtre `ORDERED`)
  - emplacement de reception
  - documents joints (facture, bon de livraison)
  - lignes de reception avec :
    - article pre-rempli depuis le bon
    - quantite commandee (lecture seule)
    - quantite recue
    - quantite rejetee + motif de rejet
    - controle qualite (checkbox accepte)
    - quarantaine (optionnel)
    - lot + expiration si applicable

---

### 11. Gestion des actifs

Usage :

- registre des actifs identifiables

Contenu recommande :

- liste avec filtres : statut, categorie, emplacement, affectataire
- tableau :
  - code actif
  - article
  - numero de serie / tag
  - emplacement
  - statut (badge)
  - condition
  - date garantie
  - valeur depreciee
  - actions
- fiche actif :
  - en-tete avec statut et condition
  - informations financieres (cout achat, depreciation)
  - onglet `Affectation`
  - onglet `Maintenance`
  - onglet `Historique localisation`
  - boutons d'action selon le statut actuel

Actions disponibles par statut :

- `AVAILABLE` : `Affecter`, `Reserver`, `Envoyer en maintenance`
- `RESERVED` : `Annuler reservation`, `Affecter`
- `ASSIGNED` / `IN_USE` : `Retourner`
- `IN_MAINTENANCE` : voir les maintenances
- `GOOD/FAIR/POOR` : `Marquer endommage`, `Retirer`

---

### 12. Maintenances actifs

Usage :

- tableau de bord technicien + historique

Contenu recommande :

- liste filtrable par actif, statut, type, periode
- formulaire creation :
  - actif (autocomplete)
  - type de maintenance
  - description du probleme
  - date planifiee
- formulaire finalisation :
  - notes resolution
  - cout de maintenance
  - nouvelle condition

---

### 13. Comptage physique

Usage :

- saisie terrain + validation manager

Contenu recommande :

- liste des sessions avec filtre statut
- detail session :
  - en-tete : emplacement, statut, dates
  - tableau des lignes :
    - article
    - quantite theorique (systeme)
    - quantite comptee (saisie)
    - ecart (colore : vert = 0, orange = +/-, rouge = negatif important)
  - progression : `X / Y articles saisis`
- mode `saisie rapide` :
  - scan code barre ou QR code
  - champ quantite
  - validation ligne par ligne

Boutons actions par statut :

- `DRAFT` : `Demarrer`
- `IN_PROGRESS` : saisie lignes, `Envoyer en revue`
- `UNDER_REVIEW` : `Valider`, `Renvoyer en saisie`
- `VALIDATED` : lecture seule + `Voir les ajustements appliques`

---

### 14. Centre des alertes

Usage :

- gestion centralisee des alertes automatiques

Contenu recommande :

- filtres : type, statut, emplacement, article
- tableau :
  - type d'alerte (icone + couleur)
  - article + emplacement
  - message
  - quantite / seuil
  - date
  - statut
  - actions rapides : `Accuser reception`, `Resoudre`, `Ignorer`
- badge dans le menu lateral avec compteur d'alertes ouvertes

---

### 15. Regles de reappro

Usage :

- configurer les seuils de declenchement des alertes

Contenu recommande :

- tableau par article / emplacement
- pour chaque regle :
  - article
  - emplacement
  - quantite minimum (seuil alerte)
  - quantite de reappro suggere
  - actif / inactif
- bouton `Generer une demande d'achat` depuis les suggestions

---

### 16. Rapports

Usage :

- analyses et exports

Contenu recommande :

- filtre periode + emplacement + article + type mouvement
- tableau de mouvements
- bouton `Exporter CSV`
- bouton `Exporter PDF`
- section anomalies avec rapport detaille

---

### 17. Page d'administration catalogue

Usage :

- gerer categories et unites

Contenu recommande :

- onglet `Categories` :
  - liste, creation, modification
  - compteur d'articles par categorie
- onglet `Unites de mesure` :
  - liste, creation, modification
  - tableau des conversions

---

### 18. Page d'import massif

Usage :

- importer les donnees initiales ou mises a jour en masse

Contenu recommande :

- selection du type d'import
- lien `Telecharger le template`
- zone drag and drop ou selection de fichier
- bouton `Valider l'import`
- affichage du rapport :
  - nombre de lignes importees
  - nombre d'erreurs
  - detail des erreurs par numero de ligne
  - bouton `Telecharger les erreurs en CSV`

---

## Parcours UX recommandes

### Parcours gestionnaire stock quotidien

1. Ouvrir le tableau de bord inventaire.
2. Consulter les alertes ouvertes.
3. Accuser reception des alertes identifiees.
4. Verifier les suggestions de reappro.
5. Creer une demande d'achat si necessaire.
6. Consulter les niveaux de stock par emplacement.

### Parcours reception commande

1. Ouvrir la liste des bons de commande avec statut `ORDERED`.
2. Selectionner le bon a recevoir.
3. Creer la reception avec les lignes livrees.
4. Controler qualite et signaler les rejets eventuels.
5. Valider la reception — le stock est mis a jour automatiquement.
6. Verifier les nouveaux niveaux de stock.

### Parcours audit et compliance

1. Ouvrir la page de rapport.
2. Selectionner la periode.
3. Consulter le rapport de mouvements.
4. Verifier le rapport d'anomalies.
5. Exporter les deux en PDF ou CSV.

---

## Recommandations de presentation

### Codes couleur utiles

- gris : article inactif / emplacement desactive
- vert : stock normal / actif disponible / mouvement valide
- bleu : en cours / en attente d'approbation / en revue
- orange : stock bas / maintenance planifiee / alerte non acquittee
- rouge : rupture / actif endommage ou perdu / alerte critique / mouvement inverse
- violet : reserve

### Composants utiles

- `StockLevelIndicator`
- `MovementTypeBadge`
- `AlertBadge`
- `AssetStatusBadge`
- `PurchaseStatusTimeline`
- `CountProgressBar`
- `StockLevelTable`
- `SupplierPerformanceCard`
- `ReorderSuggestionWidget`
- `ImportResultReport`
- `BarcodeLabel`
- `MaintenanceCard`

### Bonnes pratiques UI

- afficher toujours le stock disponible en temps reel lors d'une saisie de mouvement
- distinguer clairement `quantite en stock`, `quantite reservee` et `quantite disponible`
- afficher les ecarts de comptage en couleur (rouge si significatif)
- toujours demander confirmation et motif avant une inversion de mouvement
- rendre visible le niveau d'approbation requis en cours de saisie du bon de commande selon le montant
- sur mobile, remplacer les tableaux par des cartes avec actions swipe
- afficher le numero de lot et la date d'expiration sur chaque sortie d'article traque par lot

---

## Proposition de structure d'ecrans

### Cote operateur / gestionnaire stock

- `InventoryDashboardPage`
- `CatalogItemListPage`
- `CatalogItemDetailPage`
- `CatalogItemFormPage`
- `StockLevelPage`
- `StockMovementListPage`
- `StockInFormPage`
- `StockOutFormPage`
- `StockAdjustmentFormPage`
- `StockReservationListPage`
- `StockTransferListPage`
- `LotListPage`

### Cote achats

- `SupplierListPage`
- `SupplierDetailPage`
- `SupplierFormPage`
- `PurchaseRequestListPage`
- `PurchaseRequestDetailPage`
- `PurchaseRequestFormPage`
- `PurchaseOrderListPage`
- `PurchaseOrderDetailPage`
- `PurchaseOrderFormPage`
- `GoodsReceiptFormPage`
- `GoodsReceiptDetailPage`

### Cote actifs

- `AssetListPage`
- `AssetDetailPage`
- `AssetFormPage`
- `AssetMaintenanceListPage`
- `AssetMaintenanceFormPage`

### Cote controle

- `InventoryCountListPage`
- `InventoryCountDetailPage`
- `InventoryCountEntryPage`

### Cote intelligence

- `AlertCenterPage`
- `ReorderRuleListPage`

### Cote administration

- `AdminDashboardPage`
- `AdminReportPage`
- `AdminCategoryPage`
- `AdminUnitPage`
- `AdminLocationPage`
- `AdminImportPage`
- `AdminApprovalRulePage`

---

## Priorite de construction frontend

Ordre recommande :

1. `AdminCategoryPage` + `AdminUnitPage` + `AdminLocationPage`
2. `CatalogItemListPage` + `CatalogItemFormPage`
3. `StockLevelPage`
4. `StockInFormPage` + `StockOutFormPage`
5. `StockMovementListPage`
6. `AlertCenterPage`
7. `ReorderRuleListPage`
8. `SupplierListPage` + `SupplierFormPage`
9. `PurchaseRequestListPage` + `PurchaseRequestFormPage`
10. `PurchaseOrderListPage` + `PurchaseOrderFormPage`
11. `GoodsReceiptFormPage`
12. `AssetListPage` + `AssetDetailPage` + `AssetFormPage`
13. `AssetMaintenanceListPage` + `AssetMaintenanceFormPage`
14. `InventoryCountListPage` + `InventoryCountEntryPage`
15. `AdminReportPage` + `AdminImportPage`
16. `StockTransferListPage` + `StockReservationListPage`
17. `AdminDashboardPage`

---

## Cas d'erreur a gerer

- stock insuffisant lors d'une sortie sans `allowNegativeOverride`
- article desactive lors d'un mouvement
- emplacement desactive lors d'une affectation
- bon de commande non dans l'etat `ORDERED` lors d'une reception
- demande d'achat non approuvee lors de la creation d'un bon de commande
- comptage deja valide lors d'une tentative de modification
- actif en maintenance lors d'une tentative d'affectation
- alerte deja resolue lors d'une action doublon
- fichier import incompatible (colonnes manquantes, format incorrect)
- montant bon de commande depassant le seuil sans approbateur disponible
