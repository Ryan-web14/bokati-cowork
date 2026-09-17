# Module Inventaire · état des lieux et extension BTP

> Document de référence : ce qui existe aujourd'hui dans `features/inventory`, ce qui manque pour un
> logiciel d'inventaire généraliste, et la spécification complète des ajouts nécessaires pour couvrir
> les besoins d'une entreprise de BTP (sortie de matériel tracée, caisses à outils, dépôts multiples,
> véhicules, engins, matériaux, location et leasing d'équipement).
>
> Le module doit couvrir sans rupture toute la gradation du parc, du foret à 800 FCFA à la grue à
> 400 millions. C'est l'objet du profil de gestion à six paliers décrit en 3.2 : il évite d'appliquer
> la même lourdeur administrative à un consommable et à un engin, tout en gardant un modèle unique.

- **Périmètre** : `com.sni.bokaticowork.features.inventory`
- **Base HTTP** : `/sni/api/v1/inventory`
- **Migrations existantes** : V39 à V50, V70 à V72 (dossier `db/migration`), équivalents dans `db/migration-prod`
- **Permission RBAC** : ressource `INVENTORY`, actions dérivées de la méthode HTTP, plus `APPROVE` sur les chemins `/approve`

---

# Partie 1 · État des lieux du module existant

## 1.1 Découpage réel

Le module est découpé en huit sous-modules, chacun respectant la convention projet
(`controller` / `dto/request` / `dto/response` / `mapper/interfaces` / `mapper/decorator` / `model` /
`repository` / `service/interfaces` / `service/implementation` / `enums`).

```text
inventory/
  catalog/        referentiel articles, categories, unites, conversions
  stock/          emplacements, niveaux, lots, series, mouvements, reservations, transferts
  asset/          equipements individualises, affectations, maintenance, historique de localisation
  procurement/    fournisseurs, demandes d'achat, commandes, receptions, regles d'approbation
  control/        inventaires physiques et ecarts
  intelligence/   alertes, regles de reapprovisionnement, suggestions, worker planifie
  importer/       import tabulaire (articles, stock initial, fournisseurs, assets)
  admin/          tableau de bord, etiquettes, rapports de mouvements, rapport d'anomalies
```

Deux paquets prévus dans la conception initiale ne sont **pas** implémentés : `integration/` et
`search/`. L'entité `InventoryBarcode` prévue au design n'existe pas non plus.

## 1.2 Catalog · référentiel

### Entités

| Entité | Table | Points notables |
|---|---|---|
| `InventoryCategory` | `inventory_category` | arborescence via `parentCategory`, `code` unique, `active` |
| `InventoryItem` | `inventory_item` | voir ci-dessous |
| `InventoryUnit` | `inventory_unit` | `code`, `name`, `unitType`, `active` |
| `InventoryUnitConversion` | `inventory_unit_conversion` | `fromUnitCode`, `toUnitCode`, `factor` (BigDecimal), par article |

`InventoryItem` porte : `itemCode`, `name`, `description`, `psku`, `shortCode`, `displayCode`,
`identificationCode`, `specification`, `searchText`, `category`, `unit`, `itemType`, `trackingType`,
`defaultCost`, `salePrice`, `taxable`, `allowNegativeStock`, `requiresExpiryDate`,
`requiresLotNumber`, `requiresSerialNumber`, `active`.

Les codes dynamiques (`psku`, `shortCode`, `displayCode`, `identificationCode`, `searchText`) ont été
introduits par V70 et resserrés par V71 pour la recherche.

### Enums

```text
InventoryItemType     CONSUMABLE, ASSET, SERVICE, SPARE_PART
InventoryTrackingType NONE, QUANTITY, SERIAL, LOT, EXPIRY
InventoryUnitType     UNIT, KG, LITER, BOX, PACK, METER
```

### Endpoints

```text
POST   /inventory/categories                 PUT /inventory/categories/{code}
GET    /inventory/categories/{code}          GET /inventory/categories
PATCH  /inventory/categories/{code}/activate | /deactivate

POST   /inventory/items                      PUT /inventory/items/{itemCode}
GET    /inventory/items/{itemCode}           GET /inventory/items
PATCH  /inventory/items/{itemCode}/activate | /deactivate
DELETE /inventory/items/{itemCode}

POST   /inventory/units                      PUT /inventory/units/{code}
GET    /inventory/units/{code}               GET /inventory/units
PATCH  /inventory/units/{code}/activate | /deactivate
```

La recherche d'articles passe par `InventoryItemSpecification` (JPA Specification), pas par
Elasticsearch.

## 1.3 Stock · emplacements et mouvements

### Entités

| Entité | Table | Rôle |
|---|---|---|
| `InventoryLocation` | `inventory_location` | arborescence `parentLocation`, rattachement `businessCode` |
| `StockLevel` | `stock_level` | unique (item, location), `quantityOnHand` / `quantityReserved` / `quantityAvailable`, `averageCost` |
| `StockLot` | `stock_lot` | `lotNumber`, `expiryDate`, `initialQuantity`, `remainingQuantity`, `quarantined` + `quarantineReason`, `ownershipType`, `ownerCode` |
| `InventoryItemSerial` | `inventory_item_serial` | numéro de série, statut, lot d'origine, dates de réception et de sortie |
| `StockMovement` | `stock_movement` | journal immuable, contre-passation native |
| `StockMovementLot` | `stock_movement_lot` | ventilation par lot d'un mouvement |
| `StockReservation` | `stock_reservation` | réservation avec expiration |
| `StockTransferWorkflow` | `stock_transfer_workflow` | transfert en quatre temps |

`StockMovement` porte la traçabilité complète : `movementCode`, `item`, `locationFrom`, `locationTo`,
`movementType`, `quantity`, `unitCost`, `totalCost`, `referenceType`, `referenceCode`, `reasonCode`,
`reason`, `allowNegativeOverride`, `reversed`, `reversalOfMovement`, `reversedAt`, `reversedBy`,
`reversalReason`, `performedBy`, `performedAt`.

### Enums

```text
InventoryLocationType        SITE, WAREHOUSE, ROOM, SHELF, LOCKER, VIRTUAL
StockMovementType            IN, OUT, TRANSFER, ADJUSTMENT_IN, ADJUSTMENT_OUT
StockOutReasonCode           CONSUMPTION, DAMAGE, LOSS, INTERNAL_USE, SAMPLE, DONATION, OTHER
StockReferenceType           PURCHASE_ORDER, GOODS_RECEIPT, POS_SALE, BOOKING, CONTRACT,
                             MANUAL_ADJUSTMENT, INVENTORY_COUNT, ASSET_ASSIGNMENT, OTHER
StockOwnershipType           COMPANY, CUSTOMER, SUPPLIER, CONSIGNMENT
StockReservationStatus       ACTIVE, CONSUMED, RELEASED, EXPIRED, CANCELLED
StockTransferWorkflowStatus  REQUESTED, APPROVED, SHIPPED, RECEIVED, CANCELLED
InventorySerialStatus        AVAILABLE, ISSUED, TRANSFERRED, LOST, DAMAGED
```

### Valorisation

Le coût moyen pondéré (CMUP) est calculé dans `StockServiceImpl` :
- à l'entrée : recalcul à partir de la quantité précédente, du coût moyen précédent, de la quantité
  et du coût unitaire entrants ;
- au transfert : le coût moyen de l'emplacement source est recopié sur l'emplacement destination ;
- à l'ajustement : recalcul sur la même base.

Les montants sont stockés en `BIGINT` (entiers XAF) depuis V41.

### Endpoints

```text
POST  /inventory/stock/in
POST  /inventory/stock/out
POST  /inventory/stock/transfer
POST  /inventory/stock/adjust
POST  /inventory/stock/movements/{movementCode}/reverse
GET   /inventory/stock/movements
GET   /inventory/stock/levels

POST  /inventory/stock/reservations
PATCH /inventory/stock/reservations/{code}/release | /consume
GET   /inventory/stock/reservations

GET   /inventory/stock/lots
GET   /inventory/items/{itemCode}/lots        POST /inventory/items/{itemCode}/lots
GET   /inventory/items/{itemCode}/serials

GET   /inventory/stock/planning/picking
POST  /inventory/stock/planning/unit-conversions
GET   /inventory/stock/planning/unit-conversions/{itemCode}
GET   /inventory/stock/planning/unit-conversions/{itemCode}/convert

POST  /inventory/stock/transfers
PATCH /inventory/stock/transfers/{code}/approve | /ship | /receive | /cancel
GET   /inventory/stock/transfers

POST  /inventory/locations                    PUT /inventory/locations/{locationCode}
GET   /inventory/locations/{locationCode}     GET /inventory/locations
PATCH /inventory/locations/{locationCode}/activate | /deactivate
```

`InventoryConsumptionService` expose trois opérations destinées aux autres modules métier :
`reserveForBusinessEvent`, `releaseBusinessReservation`, `consumeBusinessReservation`.

## 1.4 Asset · équipements individualisés

### Entités

| Entité | Table | Contenu |
|---|---|---|
| `Asset` | `asset` | `assetCode`, `item`, `serialNumber`, `assetTag`, `status`, `condition`, `location`, `assignedToType`, `assignedToCode`, `purchaseDate`, `purchaseCost`, `warrantyEndDate`, `usefulLifeMonths`, `residualValue`, `notes` |
| `AssetAssignment` | `asset_assignment` | `assigneeType`, `assigneeCode`, `status`, `startAt`, `expectedReturnAt`, `endAt`, `assignedBy`, `returnedBy`, `checkoutCondition`, `returnCondition`, `checkoutPhotoUrl`, `returnPhotoUrl`, `receiverSignatureUrl`, `purpose`, `notes` |
| `AssetLocationHistory` | `asset_location_history` | `fromLocation`, `toLocation`, `changedBy`, `reason`, `changedAt` |
| `AssetMaintenance` | `asset_maintenance` | `maintenanceCode`, `maintenanceType`, `status`, `scheduledAt`, `startedAt`, `completedAt`, `providerName`, `cost`, `description`, `resolution` |

### Enums

```text
AssetStatus            AVAILABLE, RESERVED, ASSIGNED, IN_USE, IN_MAINTENANCE, LOST, RETIRED, DAMAGED
AssetCondition         NEW, GOOD, FAIR, DAMAGED, UNUSABLE
AssetAssigneeType      MEMBER, CUSTOMER, BUSINESS, USER, RESOURCE
AssetAssignmentStatus  RESERVED, ACTIVE, RETURNED, CANCELLED
AssetMaintenanceType   PREVENTIVE, CORRECTIVE, WARRANTY, INSPECTION
AssetMaintenanceStatus PLANNED, IN_PROGRESS, COMPLETED, CANCELLED
```

### Endpoints

```text
POST  /inventory/assets                       PUT /inventory/assets/{assetCode}
GET   /inventory/assets/{assetCode}           GET /inventory/assets
PATCH /inventory/assets/{assetCode}/assign | /reserve | /cancel-reservation | /return
PATCH /inventory/assets/{assetCode}/mark-lost | /mark-damaged | /retire
GET   /inventory/assets/{assetCode}/location-history
GET   /inventory/assets/{assetCode}/assignments/{assignmentId}/loan-sheet.pdf

POST  /inventory/assets/{assetCode}/maintenances
GET   /inventory/assets/{assetCode}/maintenances
PATCH /inventory/asset-maintenances/{maintenanceCode}/start | /complete | /cancel
```

Le rendu `AssetLoanSheetPdfRenderer` produit déjà une **fiche de prêt PDF** par affectation. C'est la
brique la plus proche du bon de sortie BTP attendu, mais elle est mono-asset.

## 1.5 Procurement · achats

Chaîne complète : `PurchaseRequest` → `PurchaseOrder` → `GoodsReceipt`, avec règles d'approbation par
tranche de montant (`PurchaseApprovalRule`, `PurchaseApprovalStep`) et référentiel fournisseur
(`Supplier`, `SupplierItem` avec prix, délai, fournisseur préféré).

`GoodsReceiptLine` gère déjà `receivedQuantity`, `rejectedQuantity`, `rejectionReason`,
`qualityAccepted`, `backorderQuantity`, `unitCost`, `lotNumber`, `expiryDate`. `GoodsReceipt` porte
`invoiceDocumentCode`, `deliveryNoteDocumentCode`, `proofDocumentCode`.

```text
SupplierStatus        ACTIVE, INACTIVE, BLOCKED
PurchaseRequestStatus DRAFT, SUBMITTED, APPROVED, REJECTED, CONVERTED, CANCELLED
PurchaseOrderStatus   DRAFT, APPROVED, ORDERED, PARTIALLY_RECEIVED, RECEIVED, CANCELLED
GoodsReceiptStatus    DRAFT, POSTED, CANCELLED
PurchaseApprovalLevel NONE, MANAGER, DIRECTOR, EXECUTIVE
```

```text
POST  /inventory/procurement/suppliers            PUT .../suppliers/{supplierCode}
GET   /inventory/procurement/suppliers            GET .../suppliers/{supplierCode}/performance
POST  /inventory/procurement/purchase-requests
POST  /inventory/procurement/purchase-requests/from-reorder-suggestions
PATCH /inventory/procurement/purchase-requests/{code}/submit | /approve | /reject
POST  /inventory/procurement/purchase-orders
POST  /inventory/procurement/purchase-requests/{code}/purchase-order
PATCH /inventory/procurement/purchase-orders/{code}/approve | /mark-ordered
POST  /inventory/procurement/goods-receipts       GET .../goods-receipts
POST  /inventory/procurement/approval-rules       GET .../approval-rules
```

## 1.6 Control · inventaires physiques

`InventoryCount` et `InventoryCountItem` (`expectedQuantity`, `countedQuantity`, `varianceQuantity`).
Cycle : `DRAFT → IN_PROGRESS → REVIEW → VALIDATED` (ou `CANCELLED`), la validation générant les
mouvements d'ajustement.

```text
POST  /inventory/counts
PATCH /inventory/counts/{countCode}/start | /review | /validate | /cancel
PUT   /inventory/counts/{countCode}/lines
GET   /inventory/counts/{countCode}           GET /inventory/counts
```

## 1.7 Intelligence · alertes et réapprovisionnement

`InventoryAlert` (13 types) et `InventoryReorderRule` (min / max / quantité de réapprovisionnement /
fournisseur préféré, par article et emplacement).

```text
InventoryAlertType   LOW_STOCK, RECURRING_LOW_STOCK, OUT_OF_STOCK, NEGATIVE_STOCK, OVERSTOCK,
                     SLOW_MOVING, EXPIRY_SOON, EXPIRY_IMMINENT, WARRANTY_SOON, MAINTENANCE_DUE,
                     ASSET_RETURN_OVERDUE, ASSET_RETURN_DUE_SOON, SUSPICIOUS_ADJUSTMENT
InventoryAlertStatus OPEN, ACKNOWLEDGED, RESOLVED, DISMISSED
ReorderSuggestionReason   NO_STOCK_LEVEL, OUT_OF_STOCK, BELOW_MINIMUM, AT_MINIMUM, BELOW_TARGET_MAX
ReorderSuggestionSeverity CRITICAL, HIGH, MEDIUM, LOW
ConsumptionTrend          RISING, STABLE, FALLING
```

`InventoryDailyWorker` (cadences configurables dans `application.yml`) :

| Tâche | Cron par défaut | Détections |
|---|---|---|
| Expiration des réservations | toutes les 30 min | réservations périmées |
| Péremption | toutes les 2 h | `EXPIRY_SOON` (30 j), `EXPIRY_IMMINENT` (7 j) |
| Rotation lente | toutes les 6 h | `SLOW_MOVING` (30 j sans mouvement) |
| Assets | toutes les 3 h | garantie (30 j), maintenance due, retours en retard, retours proches |

Le worker publie dans l'**outbox** et pousse des notifications **WebSocket** via
`SimpMessagingTemplate`.

## 1.8 Importer et Admin

- Import tabulaire : `POST /inventory/import/{type}` avec `type ∈ {ITEMS, INITIAL_STOCK, SUPPLIERS, ASSETS}`,
  résultat ligne par ligne (`InventoryImportRowResult`).
- Admin : tableau de bord (alertes ouvertes, ruptures, lots expirants, commandes ouvertes,
  valeur totale du stock), génération d'**étiquettes** (`/labels/{type}/{code}` et lot via `POST /labels`),
  rapports de mouvements en JSON / CSV / PDF, rapport d'anomalies (stock négatif, surcharges de
  négatif, ajustements suspects, écarts d'inventaire importants).

## 1.9 Intégration transverse

- **Séquences** : `SequenceGeneratorFacade` pour tous les codes métier (V42 et V72 sur les séquences inventaire).
- **Outbox** : événements publiés de façon transactionnelle, traités toutes les 10 s par lots de 25.
- **Audit** : journalisation AOP centrale.
- **Idempotence** : mécanisme `core/idempotency` disponible.
- **RBAC** : `AdminApiAuthorizationManager` mappe `/inventory` sur la ressource `INVENTORY`,
  avec `APPROVE` pour les chemins contenant `/approve`.
- **Temps réel** : WebSocket pour les alertes.

---

# Partie 2 · Manques pour un inventaire généraliste

Ces manques concernent tout logiciel d'inventaire sérieux, indépendamment du BTP.

## 2.1 Valorisation et comptabilité matière

| Manque | Conséquence | Correctif proposé |
|---|---|---|
| Une seule méthode de valorisation (CMUP), non paramétrable | impossible de valoriser en FIFO / LIFO / coût standard | `valuationMethod` sur `InventoryItem` ou `InventoryCategory` |
| Pas de couches de coût (`cost layers`) | FIFO impossible, coût de sortie approximatif | entité `StockCostLayer` (quantité, coût unitaire, date, lot) consommée en FIFO / FEFO |
| Pas d'écriture comptable matière | aucun rapprochement stock / comptabilité | `StockJournalEntry` (compte de stock, compte de contrepartie, chantier, centre de coût) |
| Pas de clôture de période | le stock passé est réinscriptible indéfiniment | `InventoryPeriod` avec statut `OPEN / CLOSED`, blocage des mouvements antérieurs |
| Pas de photo de valorisation historique | impossible de reconstituer la valeur du stock au 31/12 | `StockValuationSnapshot` quotidien ou mensuel |
| Pas de dépréciation / provision | stock obsolète valorisé au plein | `provisionRate` par tranche d'ancienneté |

## 2.2 Traçabilité

- **Généalogie des lots** absente : on sait qu'un lot est sorti, pas ce qu'il est devenu en aval
  (transformation, incorporation). À couvrir par `StockLotGenealogy` (lot parent → lot enfant).
- **Traçabilité amont / aval sur un seul écran** (rappel produit, litige fournisseur) : endpoint
  `GET /inventory/traceability/{lotNumber}` remontant la chaîne complète.
- **Numéros de série** : gérés en entité mais sans historique de possession propre (le suivi passe
  par l'asset, ce qui exclut les consommables sérialisés).

## 2.3 Codes-barres, QR et terrain

- `InventoryBarcode` n'existe pas : un article a plusieurs codes (EAN fournisseur, code interne,
  code carton). À créer avec `barcodeType`, `barcodeValue`, `unit`, `primary`.
- Pas d'endpoint de résolution par scan : `GET /inventory/scan/{code}` qui identifie article,
  asset, lot, série, emplacement, caisse ou chantier et renvoie l'action contextuelle.
- Pas de mode hors ligne ni de file de synchronisation pour les terminaux de terrain.

## 2.4 Approvisionnement et retours

- **Retours fournisseur** (`SupplierReturn`) et avoirs sur réception : absents.
- **Litiges réception** : `rejectedQuantity` existe mais sans workflow de réclamation ni suivi.
- **Multi-devise** : montants en `BIGINT` sans code devise ni taux, bloquant pour l'import.
- **Taxes d'achat** : pas de ventilation TVA / droits de douane sur les lignes de commande, alors que
  le module `billing` gère déjà une ventilation fiscale.
- **Frais accessoires** (`landed cost`) : transport, dédouanement, manutention non incorporés au coût
  d'achat, ce qui fausse le CMUP sur les matériaux importés.
- **Contrats-cadres et tarifs négociés** avec paliers de quantité : absents.

## 2.5 Inventaires physiques

- Pas de **comptage tournant** (`cycle counting`) avec classification ABC et fréquence par classe.
- Pas d'**inventaire aveugle** (le compteur voit la quantité théorique).
- Pas de **double comptage** ni d'arbitrage sur écart.
- Pas de **gel des mouvements** sur la zone pendant l'inventaire.
- Pas de comptage par emplacement de rangement fin ni par contenant.

## 2.6 Structure et opérations d'entrepôt

- Pas de **nomenclature / kit** (`BOM`) : impossible d'assembler ou de décomposer un article.
- Pas d'ordre de **préparation** (`picking list`) multi-lignes ; `planning/picking` ne fait que
  suggérer sur un article.
- Pas de **stratégies d'emplacement** (rangement dirigé, zones de picking / réserve).
- Pas de **règles d'allocation** paramétrables (FEFO, FIFO, lot le plus proche).
- Pas de gestion des **colis / unités logistiques** (palette, carton).

## 2.7 Recherche et analytique

- `search/` non implémenté : la recherche article est en JPA Specification. Elasticsearch 8.18 est
  bien déclaré dans `compose.yaml` et tourne sur le port 9200, mais **aucun code Java ne s'y
  connecte** : pas de dépendance dans `pom.xml`, pas de configuration dans `application.yml`, aucune
  référence dans les sources. Le module `ressource` fonctionne lui aussi en JPA Specification. Passer
  la recherche sur Elasticsearch est donc un chantier complet, pas un simple branchement, et la
  mention d'Elasticsearch dans `CLAUDE.md` décrit une intention et non l'état du code.
- Indicateurs absents : rotation du stock, couverture en jours, taux de service, ancienneté du stock
  (`stock aging`), valeur immobilisée par famille, taux de disponibilité des équipements.
- Pas d'export analytique vers le module `analytics` / `reporting`.

## 2.8 Divers

- Pas de **réservation d'asset par plage de dates** exploitable en planning (le statut `RESERVED`
  existe mais sans calendrier).
- Pas de **pièces jointes** normalisées sur les entités stock (le module `document` existe et n'est
  câblé que sur `GoodsReceipt`).
- Pas de **gestion multi-société** stricte : `businessCode` existe sur `InventoryLocation` mais n'est
  pas propagé comme dimension de cloisonnement sur les articles et les assets.

---

## 2.9 Référentiel article avancé

| Manque | Conséquence | Correctif proposé |
|---|---|---|
| Cycle de vie réduit à un booléen `active` | impossible de piloter une fin de série | `lifecycleStatus` : `DRAFT, NEW, ACTIVE, PHASE_OUT, OBSOLETE, BLOCKED` |
| Pas d'articles à variantes | une vis M8 en trois longueurs fait trois fiches sans lien | `InventoryItemTemplate` plus `InventoryItemVariant` (axes taille, couleur, longueur, grade) |
| Pas de substituts ni d'équivalences | rupture évitable alors qu'un équivalent est en stock | `InventoryItemSubstitute` (article, substitut, bidirectionnel, priorité, taux de conversion) |
| Pas de versions ni de révisions | impossible de savoir quelle révision est en stock | `revision` sur l'article plus `InventoryItemRevisionHistory` |
| Pas de multi-langue | libellés figés | `InventoryItemTranslation` (article, langue, nom, description) |
| Pas d'historisation des prix et coûts | aucune explication des variations de marge | `InventoryItemPriceHistory` (type de prix, valeur, période de validité, auteur) |
| Un seul code-barres implicite | codes fournisseur, carton et palette non gérés | `InventoryBarcode` (type GS1 ou interne, valeur, unité, quantité par code, principal) |
| Pas de conditionnements | on ne sait pas qu'un carton contient 24 unités | `InventoryPackaging` (article, niveau, unité, quantité contenue, dimensions, poids, code-barres) |
| Pas de fiche technique ni de pièces jointes normalisées | documents épars | rattachement générique à `features/document` via `entityType` et `entityCode` |
| Pas de poids ni de dimensions | calcul de chargement et de transport impossible | `weightKg`, `volumeM3`, `lengthMm`, `widthMm`, `heightMm`, `stackable` |
| Unité de mesure unique par ligne | l'acier se compte au mètre et se facture au kilo | double unité (`catch weight`) : `quantityInStockUnit` plus `quantityInBillingUnit` sur les lignes |

## 2.10 Qualité, quarantaine et conformité

Le champ `StockLot.quarantined` existe mais aucun processus ne le pilote.

```text
QualityControlPlan
  item | categoryCode, controlStage   ON_RECEIPT, IN_STORAGE, BEFORE_ISSUE, PERIODIC
  samplingMode                        FULL, AQL, FIXED_QUANTITY, PERCENTAGE
  samplingParameter
  criteria : QualityCriterion (nom, type, valeur attendue, tolérance, unité, bloquant)
  requiredDocuments
  defaultDecisionOnFail               REJECT, QUARANTINE, DEROGATION_REQUIRED

QualityInspection
  plan, lotNumber, itemCode, locationCode, sourceType, sourceCode
  inspectedAt, inspectedByCode
  sampledQuantity, conformQuantity, nonConformQuantity
  results : QualityInspectionResult (critère, valeur mesurée, conforme)
  decision                            ACCEPTED, REJECTED, QUARANTINED, ACCEPTED_BY_DEROGATION
  decisionByCode, decisionReason
  nonConformanceCode

NonConformance
  code, sourceType, sourceCode, itemCode, lotNumber, quantity
  severity                            MINOR, MAJOR, CRITICAL
  detectedAt, detectedByCode, description, photoUrls
  disposition                         USE_AS_IS, REWORK, RETURN_TO_SUPPLIER, SCRAP, DOWNGRADE
  correctiveAction, responsibleCode, dueDate, closedAt
  supplierClaimCode, costImpact
```

À ajouter également : la **libération de quarantaine** avec double validation, le **blocage de lot**
manuel (`StockLot.blocked` plus motif), et la procédure de **rappel** (`ProductRecall`) qui gèle tous
les lots d'une plage et notifie les détenteurs à partir de la traçabilité descendante.

## 2.11 Planification, prévision et réapprovisionnement intelligent

Le module sait déclencher sur un seuil saisi à la main. Un logiciel complet calcule ce seuil.

| Manque | Correctif proposé |
|---|---|
| Seuils min et max saisis manuellement | calcul du **stock de sécurité** à partir de la variabilité de la demande et du délai fournisseur, avec niveau de service cible |
| Pas de prévision | moyenne mobile, lissage exponentiel simple et double, détection de saisonnalité, sur `ConsumptionForecast` (article, emplacement, période, quantité prévue, intervalle de confiance, méthode) |
| Pas de quantité économique | calcul de la quantité de commande économique arbitrant coût de passation contre coût de possession, et respect des multiples de conditionnement fournisseur |
| Pas de classification | `AbcXyzClassification` automatique (A, B, C sur la valeur, X, Y, Z sur la régularité) pilotant fréquence d'inventaire, niveau de service et mode de réappro |
| Pas de calcul de besoins nets | `NetRequirementRun` intégrant stock disponible, réservé, commandes attendues, besoins planifiés et délais, avec proposition d'ordres |
| Pas de simulation | mode `dry-run` sur les propositions de réappro, avec impact chiffré sur la trésorerie et le taux de service |
| Pas de gestion des ruptures | `StockOutEvent` (article, emplacement, début, fin, demande non servie) pour mesurer le taux de service réel |

## 2.12 Achats avancés

| Manque | Correctif proposé |
|---|---|
| Pas d'appel d'offres | `RequestForQuotation` plus `SupplierQuotation` avec grille comparative multi-critères et attribution tracée |
| Pas de contrat-cadre | `SupplierAgreement` (période, engagement de volume, tarifs par palier, remises, révision de prix, pénalités) |
| Pas de conditions tarifaires | `SupplierPriceTier` (quantité minimale, prix, validité) et remises de fin de période |
| Pas d'avis d'expédition | `AdvanceShipmentNotice` pour connaître le contenu et la date avant l'arrivée du camion |
| Pas de rapprochement à trois voies | contrôle commande contre réception contre facture, avec tolérances de prix et de quantité, et blocage du paiement en cas d'écart |
| Pas de retour fournisseur | `SupplierReturn` plus `SupplierClaim` (litige qualité, quantité, prix, retard) et suivi de l'avoir attendu |
| Évaluation fournisseur rudimentaire | scoring pondéré : ponctualité, conformité quantité, conformité qualité, respect du prix, réactivité, litiges, avec historique et plan de progrès |
| Pas de frais accessoires | `LandedCostAllocation` répartissant transport, dédouanement, assurance et manutention sur les lignes réceptionnées, au poids, au volume ou à la valeur |
| Mono-devise | `currencyCode` et `exchangeRate` sur les documents d'achat, écart de change constaté à la facturation |
| Pas de taxes d'achat | ventilation TVA et droits sur les lignes, récupérable ou non, en cohérence avec `features/billing` |
| Pas de portail fournisseur | accusé de réception de commande, confirmation de date, dépôt de documents |
| Pas de budget achat | enveloppes par période et par catégorie, avec contrôle d'engagement au moment de la commande |

## 2.13 Opérations d'entrepôt

| Manque | Correctif proposé |
|---|---|
| Pas de rangement dirigé | `PutAwayRule` (article ou catégorie, zone cible, critère de choix, capacité) |
| Pas de règles d'allocation | `AllocationStrategy` paramétrable par article : FEFO, FIFO, LIFO, lot le plus proche, emplacement le plus proche, quantité exacte |
| Pas de réappro interne | `InternalReplenishmentTask` de la zone réserve vers la zone de prélèvement, sur seuil de picking |
| Pas d'ordres de mission | `WarehouseTask` (type, priorité, opérateur, emplacement source et cible, statut, temps passé) pour piloter et mesurer l'activité |
| Pas de préparation multi-lignes | `PickingList` et `PickingListLine`, regroupement par tournée, contrôle par scan, colisage |
| Pas d'unités logistiques | `HandlingUnit` (palette ou colis, identifiant SSCC, contenu, poids, emplacement) permettant de déplacer un contenu complet |
| Pas de contrôle de capacité | dimensions, poids et volume maximum par emplacement, avec refus ou avertissement au rangement |
| Pas de cross-docking ni de livraison directe | flux réception vers expédition sans mise en stock, et commande fournisseur livrée directement au client ou au chantier |
| Pas de gestion des zones | zonage (réception, réserve, picking, expédition, quarantaine, rebut) avec droits et règles propres |

## 2.14 Inventaires physiques, suite

En complément du point 2.5 :

- **motifs d'ajustement paramétrables** (`AdjustmentReason`) avec compte comptable et niveau
  d'approbation associés, au lieu d'un texte libre ;
- **seuils d'approbation d'écart** : au-delà d'un montant ou d'un pourcentage, la validation d'un
  inventaire exige un visa supérieur ;
- **gel de zone** pendant le comptage, empêchant tout mouvement sur les emplacements concernés ;
- **comptage par contenant et par emplacement fin**, et non seulement par article et par emplacement ;
- **inventaire aveugle** et **double comptage** avec arbitrage obligatoire en cas d'écart entre les
  deux passages ;
- **comptage tournant** piloté par la classification ABC, avec calendrier généré automatiquement et
  taux de couverture annuel mesuré ;
- **inventaire de fin de période** rattaché à `InventoryPeriod` et bloquant la clôture tant qu'il
  n'est pas validé.

## 2.15 Propriété du stock et flux tiers

`StockOwnershipType` distingue déjà `COMPANY`, `CUSTOMER`, `SUPPLIER` et `CONSIGNMENT`, mais aucun
processus ne l'exploite.

- **Consignation entrante** : stock chez nous, propriété du fournisseur, facturé à la consommation.
  Exige un relevé de consommation périodique, un rapprochement avec le fournisseur et une
  auto-facturation. Hors bilan tant qu'il n'est pas consommé.
- **Consignation sortante** : notre stock chez un client ou un dépositaire, à inventorier à distance.
- **Stock confié par le client** (fourniture du maître d'ouvrage) : ni valorisé, ni facturé, mais
  suivi et restituable, avec responsabilité en cas de perte.
- **Dépôt-vente** et **prêt commercial** : mêmes mécaniques, motifs différents.
- **Séparation comptable stricte** : les états de valorisation ne doivent jamais additionner du stock
  propre et du stock tiers. Filtre obligatoire sur `ownershipType` dans tous les rapports de valeur.

## 2.16 Intégration, API et interopérabilité

| Manque | Correctif proposé |
|---|---|
| Pas de webhooks inventaire | événements sortants sur mouvement, alerte, réception, sortie, en réutilisant l'infrastructure de `features/notification` |
| Pas d'export comptable | génération des écritures de stock et export au format attendu par le cabinet |
| Import limité à quatre types | ajouter mouvements, comptages, prix fournisseur, emplacements, assets détaillés, avec mode simulation, rapport d'erreurs téléchargeable et annulation d'import |
| Pas d'export généralisé | export CSV et Excel de toute liste filtrée, plus export programmé récurrent |
| Recherche non unifiée | index Elasticsearch couvrant articles, assets, lots, séries, emplacements et mouvements, avec recherche par code-barres, code partiel et tolérance aux fautes |
| Pas d'API publique documentée | exposition versionnée et authentifiée par clé pour les intégrations tierces, distincte de l'API interne |
| Pas de GraphQL sur l'inventaire | le projet utilise DGS ; exposer au minimum le catalogue et les niveaux de stock |

## 2.17 Robustesse et exploitation

Ces points ne se voient pas dans une démonstration, mais décident de la fiabilité en production.

- **Concurrence** : déjà traitée. `StockLevel` porte `@Version` et
  `StockLevelRepository.findByItemAndLocationForUpdate` verrouille en `PESSIMISTIC_WRITE`. Même
  traitement sur `StockLot`, `StockReservation`, `Asset`, `InventoryCount`, `PurchaseOrder`,
  `PurchaseRequest` et `StockTransferWorkflow`. Reste à faire : des tests de charge sur les sorties
  simultanées d'un même article, et la vérification que tous les chemins d'écriture passent bien par
  la variante verrouillée plutôt que par `findByItemAndLocation`.
- **Idempotence** : déjà câblée. L'aspect `@Idempotent` couvre les treize contrôleurs du module, dont
  les cinq endpoints de mouvement (`/in`, `/out`, `/transfer`, `/adjust`, `/movements/{code}/reverse`).
  Reste un point ouvert : tous sont déclarés `required = false`, donc un client qui n'envoie pas de
  clé d'idempotence n'est pas protégé. Pour des terminaux de terrain qui rejouent après coupure
  réseau, les endpoints de mouvement devraient passer en `required = true`, ce qui est une rupture
  de contrat à planifier avec les clients de l'API.
- **Immuabilité du journal** : `StockMovement` ne doit jamais être modifié ni supprimé. La
  contre-passation existe déjà ; il faut la contrainte en base et l'interdiction applicative.
- **Cohérence recalculable** : une commande de vérification qui recalcule `StockLevel` à partir de la
  somme des mouvements et signale les divergences, plus un rapport de réconciliation périodique.
- **Archivage et purge** : politique de rétention des mouvements anciens avec archivage avant purge,
  et suppression logique plutôt que physique sur les entités référencées.
- **Données personnelles** : les signatures, photos et identifiants de destinataires sont des données
  personnelles. Durée de conservation, base légale et procédure d'effacement à définir.
- **Traçabilité des dérogations** : toute surcharge de règle (stock négatif, sortie malgré une
  inspection expirée, ouverture de scellé) doit être journalisée avec auteur, motif et approbateur,
  puis restituée dans un rapport dédié.
- **Reprise de données** : outillage d'initialisation du stock avec rapprochement contradictoire et
  procès-verbal d'entrée en service.

## 2.18 Restitution et pilotage

Indicateurs absents à ajouter, alimentés vers `features/analytics` et `features/reporting` :

- rotation du stock et couverture en jours, par article, famille et emplacement ;
- ancienneté du stock et valeur dormante, avec seuils de provision ;
- taux de service et ruptures, mesurés sur les demandes et non sur les sorties ;
- exactitude d'inventaire (nombre d'emplacements justes rapporté au nombre compté) ;
- valeur immobilisée par famille, par emplacement et par propriétaire ;
- taux de disponibilité et d'utilisation des équipements ;
- coût des ajustements et des pertes, par motif et par responsable ;
- performance fournisseur consolidée ;
- tableaux de bord par rôle (magasinier, responsable matériel, contrôleur de gestion, direction) avec
  widgets configurables, plutôt qu'un tableau de bord unique.

---

# Partie 3 · Spécificités BTP

C'est le cœur de la demande. Un magasin de BTP se distingue d'un magasin classique sur cinq points :
le **chantier** est la dimension analytique principale, le stock est **mobile** (camions, caisses,
conteneurs), le matériel est **confié à des personnes** avec responsabilité nominative, les
équipements sont **loués** en entrée comme en sortie, et une partie du parc est soumise à des
**contrôles réglementaires**.

## 3.1 Nouvelles dimensions du référentiel

### Chantier (`Worksite`)

Première classe, nouvelle entité. C'est la dimension d'imputation de tout mouvement sortant.

```text
Worksite
  id, worksiteCode (séquence), name, description
  customer (client final, lien vers features/client)
  businessCode
  status            : PROSPECT, PREPARATION, ACTIVE, SUSPENDED, RECEPTION, CLOSED, CANCELLED
  startDate, plannedEndDate, actualEndDate
  addressLine, city, country, latitude, longitude, geofenceRadiusMeters
  siteManagerCode   (chef de chantier, utilisateur)
  storeKeeperCode   (magasinier de chantier)
  budgetMaterialAmount, budgetEquipmentAmount
  defaultLocation   (le magasin de chantier créé automatiquement)
  contractCode      (lien vers features/contract)
  createdAt, updatedAt
```

Chaque chantier crée automatiquement un `InventoryLocation` de type `WORKSITE`, rattaché en enfant du
dépôt de rattachement. Un chantier clos verrouille son emplacement : plus aucune sortie, et un
inventaire de clôture obligatoire avec retour au dépôt du reliquat.

### Extension de `InventoryLocationType`

```text
InventoryLocationType
  existants : SITE, WAREHOUSE, ROOM, SHELF, LOCKER, VIRTUAL
  ajouts    : DEPOT            dépôt central ou régional
              WORKSITE         magasin de chantier
              VEHICLE          véhicule ou engin porteur de stock
              CONTAINER        conteneur maritime, bungalow, benne
              TOOLBOX          caisse à outils, malle, servante
              WORKSHOP         atelier de réparation
              QUARANTINE       zone de mise en quarantaine
              SCRAP            zone de rebut
              TRANSIT          en cours de transfert entre deux emplacements
              THIRD_PARTY      chez un sous-traitant ou un client
```

Ajouts de champs sur `InventoryLocation` :

```text
  mobile              boolean, vrai pour VEHICLE, CONTAINER, TOOLBOX
  linkedAssetCode     l'asset qui matérialise l'emplacement mobile (le camion, la caisse)
  worksiteCode        rattachement chantier courant d'un emplacement mobile
  custodianCode       responsable nominatif de l'emplacement
  sealNumber          numéro de scellé pour une caisse plombée
  capacityWeightKg, capacityVolumeM3
  geofence            latitude, longitude, rayon
```

**Point de conception central** : une caisse à outils, un camion et un conteneur sont à la fois un
**asset** (on les achète, on les entretient, on les perd) et un **emplacement** (ils contiennent du
stock). Le lien `InventoryLocation.linkedAssetCode` ↔ `Asset.linkedLocationCode` matérialise cette
dualité sans dupliquer les données. Déplacer l'asset déplace l'emplacement et donc, en cascade, tout
son contenu.

### Catégorisation BTP de `InventoryItemType`

```text
InventoryItemType
  existants : CONSUMABLE, ASSET, SERVICE, SPARE_PART
  ajouts    : MATERIAL         matériau de construction (ciment, fer, agrégats)
              BULK_MATERIAL    matériau en vrac vendu au poids ou au volume
              TOOL             outillage individualisé, retournable
              SMALL_TOOL       petit outillage non individualisé, suivi en quantité
              PPE              équipement de protection individuelle
              MACHINE          engin ou machine à compteur
              FORMWORK         coffrage, étaiement, échafaudage (loué à la journée)
              FUEL             carburant et lubrifiants
```

Champs supplémentaires sur `InventoryItem` pour le BTP :

```text
  returnable            boolean  (l'outil revient, le ciment non)
  consumableOnIssue     boolean  (consommé dès la sortie, pas de retour attendu)
  densityKgPerM3        BigDecimal, pour les conversions vrac volume ↔ poids
  wastageRatePercent    BigDecimal, freinte ou casse admise
  requiresCertification boolean  + certificationCode (CACES, habilitation électrique)
  ppeLifespanMonths     Integer  (casque 5 ans, harnais selon norme)
  standardIssueQuantity BigDecimal, dotation standard par ouvrier
  hireRatePerDay, hireRatePerWeek, hireRatePerMonth, hireRatePerHour (tarif de location interne ou externe)
```

### Matériaux en vrac

Les agrégats (sable, gravier, tout-venant) et le béton se commandent en m³, se livrent en tonnes et se
consomment en m³. `InventoryUnitConversion` existe déjà avec un facteur par article : il suffit
d'alimenter la conversion à partir de `densityKgPerM3` et d'ajouter les unités manquantes.

```text
InventoryUnitType
  existants : UNIT, KG, LITER, BOX, PACK, METER
  ajouts    : TON, CUBIC_METER, SQUARE_METER, LINEAR_METER, BAG, ROLL, BUNDLE,
              PALLET, TRUCKLOAD, HOUR, DAY
```

Prévoir aussi la **tolérance de réception** sur le vrac : une livraison de 30 t peut arriver à
29,4 t sans être un litige. Champ `receiptTolerancePercent` sur `InventoryItem` ou `SupplierItem`.

## 3.2 Gradation du parc : du plus petit outil au gros engin

Une entreprise de BTP gère dans le même magasin un foret à 800 FCFA et une grue à 400 millions.
Appliquer le même niveau de suivi aux deux est intenable : soit on noie les magasiniers sous la
paperasse pour des consommables, soit on perd la trace des engins. La réponse est un **profil de
gestion** déclaratif, porté par l'article et surchargeable par l'asset, qui pilote automatiquement
tout le comportement du module.

### Les six paliers

| Palier | Nature | Exemples BTP | Identification | Suivi | Retour | Maintenance | Imputation |
|---|---|---|---|---|---|---|---|
| **T0** | Consommable d'outillage | forets, disques à tronçonner, lames de scie, électrodes, mèches, embouts | aucune | quantité pure | non | aucune | consommé à la sortie |
| **T1** | Petit outillage banalisé | marteaux, truelles, pelles, niveaux, seaux, règles, serre-joints | marquage de lot ou de dépôt | quantité par détenteur ou par caisse | attendu mais non nominatif | remplacement | dotation, casse admise |
| **T2** | Outillage individualisé | perceuses, meuleuses, scies circulaires, postes à souder, vibreurs, dameuses | `assetTag` gravé plus QR | asset nominatif | obligatoire, daté | corrective, révision légère | location interne à la journée |
| **T3** | Machine de chantier | bétonnière, groupe électrogène, compresseur, pompe à béton, échafaudage, banches | plaque constructeur plus QR | asset avec compteur | obligatoire, état des lieux | préventive sur compteur | location interne, jours-machine |
| **T4** | Engin lourd automoteur | pelle hydraulique, chargeuse, bulldozer, grue, nacelle, chariot télescopique, compacteur | numéro de série constructeur, immatriculation éventuelle | asset, compteur, télématique | affectation longue avec pointage | préventive, VGP, contrôle réglementaire | taux horaire machine, carburant, TCO |
| **T5** | Véhicule routier | camion benne, toupie, porte-char, fourgon, véhicule léger | immatriculation, VIN | asset, flotte, documents réglementaires | affectation conducteur | entretien périodique, visite technique | coût au kilomètre, carburant, assurance |

### Le profil de gestion

Plutôt que de coder en dur les règles par type d'article, on déclare un profil réutilisable. Changer
la politique de suivi des meuleuses devient un paramétrage, pas un développement.

```text
ManagementProfile
  id, profileCode, name, tier          T0 | T1 | T2 | T3 | T4 | T5

  -- Identification
  identificationMode                   NONE | LOT | TAG | SERIAL | PLATE
  requiresPhysicalMarking              boolean
  markingType                          ENGRAVING | METAL_PLATE | QR_STICKER | RFID_TAG | PAINT_CODE
  labelTemplateCode

  -- Suivi
  trackingMode                         QUANTITY | POOL_BY_HOLDER | INDIVIDUAL_ASSET
  requiresCustodyChain                 boolean   journalise chaque changement de détenteur
  requiresDailyRollCall                boolean   pointage quotidien sur chantier
  requiresGeolocation                  boolean

  -- Retour
  returnPolicy                         NOT_EXPECTED | EXPECTED_BULK | MANDATORY_NOMINATIVE
  defaultLoanDurationDays
  toleratedLossRatePercent             taux de casse ou de perte admis sur la période
  chargeLossToHolder                   boolean

  -- Maintenance et conformité
  maintenanceMode                      NONE | ON_FAILURE | CALENDAR | METER | METER_AND_CALENDAR
  requiresInspection                   boolean
  inspectionTypes                      liste de AssetInspection.inspectionType
  requiresOperatorCertification        boolean
  certificationCodes

  -- Coût
  costingMode                          CONSUMED_ON_ISSUE | INTERNAL_HIRE_RATE | TCO
  internalHireRatePerDay, internalHireRatePerHour
  depreciationMethod
  capitalizationThreshold              seuil au-delà duquel l'achat devient une immobilisation

  -- Approvisionnement
  replenishmentMode                    MANUAL | MIN_MAX | PAR_LEVEL_BY_CONTAINER | ON_DEMAND
  autoReplenishContainers              boolean
```

`InventoryItem` reçoit `managementProfileCode`, et `Asset` peut le surcharger : une perceuse
d'exposition suivie en T2 peut passer en T3 si on lui ajoute un compteur.

### Conséquences mécaniques du profil

- **T0 et T1** ne créent pas d'`Asset`. Une sortie les consomme ou les affecte à un pool. Le suivi est
  statistique : on ne cherche pas le marteau numéro 7, on surveille le taux de casse par équipe et par
  mois. Dépassement du `toleratedLossRatePercent` : alerte `ABNORMAL_TOOL_LOSS_RATE`.
- **T2** déclenche systématiquement un `AssetAssignment` nominatif et un `CustodyEvent`. Le retour est
  bloquant pour la clôture du chantier.
- **T3 à T5** ajoutent compteur, plan de maintenance, inspections et, pour T4 et T5, blocage
  d'affectation si la certification du conducteur ou un contrôle réglementaire est expiré.
- **T4 et T5** entrent dans le pointage journalier et, si équipés, dans la remontée télématique.
- Le **seuil de capitalisation** (`capitalizationThreshold`) arbitre automatiquement entre charge et
  immobilisation à la réception, ce qui alimente le plan d'amortissement sans ressaisie comptable.

### Migration progressive

Les paliers permettent un déploiement par étapes sans bloquer l'entreprise : démarrer en T4 et T5
(les engins, là où l'argent est), puis T2 (l'outillage électroportatif, là où les pertes sont), puis
T1 et T3, et laisser T0 en simple gestion de quantité. Un profil par défaut par catégorie d'article
évite de qualifier des milliers de références une par une.

## 3.3 Le suivi de sortie de matériel

C'est la demande principale : savoir **quoi** est sorti, **à qui** il a été remis, **pourquoi**,
**quand**, **qui l'a remis**, **d'où** il venait, et **dans quelle caisse ou quel véhicule** il se
trouvait.

### Entité `MaterialIssue` (bon de sortie)

```text
MaterialIssue
  id, issueCode                séquence dédiée, par exemple BS-2026-000123
  issueType                    voir enum ci-dessous
  status                       DRAFT, PENDING_APPROVAL, APPROVED, ISSUED,
                               PARTIALLY_RETURNED, RETURNED, CLOSED, CANCELLED

  -- D'où
  fromLocation                 emplacement source (dépôt, magasin de chantier)
  sourceContainerCode          caisse ou conteneur d'origine de l'outil
  sourceVehicleAssetCode       véhicule d'origine

  -- Vers qui, vers où
  worksite                     chantier d'imputation
  toLocation                   emplacement de destination (magasin de chantier, véhicule, caisse)
  destinationContainerCode     caisse dans laquelle le matériel est rangé à la sortie
  vehicleAssetCode             véhicule de transport
  driverCode                   conducteur

  -- Qui
  issuedByCode                 magasinier qui a remis physiquement le matériel
  issuedByName                 dénormalisé pour le PDF
  receivedByType               EMPLOYEE, SUBCONTRACTOR, CUSTOMER, SITE_MANAGER, DRIVER, OTHER
  receivedByCode               identifiant du destinataire
  receivedByName               dénormalisé
  receivedByIdDocument         numéro de pièce présentée pour un tiers
  approvedByCode, approvedAt   visa hiérarchique si le seuil l'exige

  -- Pourquoi, quand
  purpose                      texte libre obligatoire
  purposeCode                  enum MaterialIssuePurpose
  taskCode                     lien vers features/task si la sortie est liée à une tâche
  issuedAt                     horodatage de la remise réelle
  expectedReturnAt             date de retour attendue (outillage, coffrage)
  actualReturnAt
  returnGraceDays

  -- Preuves
  receiverSignatureUrl         signature tactile du destinataire
  issuerSignatureUrl           signature du magasinier
  checkoutPhotoUrls            photos de l'état à la sortie
  gpsLatitude, gpsLongitude    position au moment de la remise
  deviceId                     terminal utilisé
  documentCode                 PDF signé archivé via features/document

  totalEstimatedValue          valeur du matériel sorti, pour le seuil d'approbation
  notes
  createdAt, updatedAt
```

```text
MaterialIssueType
  WORKSITE_ISSUE      sortie chantier
  INTERNAL_LOAN       prêt interne entre dépôts ou services
  SUBCONTRACTOR       matériel confié à un sous-traitant
  CUSTOMER_HIRE       location sortante vers un client
  REPAIR_OUT          départ en réparation chez un prestataire
  SCRAP               mise au rebut
  SALE                vente de surplus
  RETURN_TO_SUPPLIER  retour fournisseur

MaterialIssuePurpose
  PRODUCTION, INSTALLATION, MAINTENANCE, REPAIR, SAFETY_EQUIPMENT,
  REPLACEMENT, TRAINING, TESTING, EMERGENCY, OTHER
```

### Entité `MaterialIssueLine`

Une ligne peut porter **trois natures** de sortie, ce qui évite trois bons de sortie distincts pour un
même camion qui part au chantier :

```text
MaterialIssueLine
  id, materialIssue
  lineType             ITEM | ASSET | CONTAINER
  item                 renseigné si lineType = ITEM
  asset                renseigné si lineType = ASSET
  containerLocation    renseigné si lineType = CONTAINER (sortie d'une caisse complète)

  quantity, unitCode, quantityInBaseUnit
  lotNumber, serialNumbers
  unitCost, totalCost
  conditionOut         AssetCondition à la sortie
  conditionIn          AssetCondition au retour
  returnedQuantity
  lostQuantity, damagedQuantity, consumedQuantity
  discrepancyReason
  placedInContainerCode  la caisse dans laquelle cette ligne est rangée
  stockMovementCode      mouvement de stock généré
  assetAssignmentId      affectation d'asset générée
  notes
```

### Ce que la sortie déclenche

1. Pour chaque ligne `ITEM` : un `StockMovement` de type `TRANSFER` (dépôt → emplacement de
   destination) ou `OUT` avec `reasonCode = CONSUMPTION` si l'article est
   `consumableOnIssue`, avec `referenceType = MATERIAL_ISSUE` et `referenceCode = issueCode`.
2. Pour chaque ligne `ASSET` : un `AssetAssignment` en statut `ACTIVE`, avec `assigneeType` étendu à
   `EMPLOYEE` / `SUBCONTRACTOR` / `WORKSITE`, plus une entrée `AssetLocationHistory`.
3. Pour chaque ligne `CONTAINER` : un mouvement en cascade de **tout le contenu** de la caisse, avec
   un instantané de composition (`ContainerSnapshot`) pris à la sortie pour pouvoir constater l'écart
   au retour.
4. Un événement outbox `INVENTORY.MATERIAL_ISSUED` pour les notifications et l'analytique.
5. La génération du **PDF de bon de sortie** avec les deux signatures, archivé dans `features/document`.

### Retour : `MaterialReturn`

```text
MaterialReturn
  id, returnCode, materialIssue, status
  returnedAt, returnedByCode, receivedByCode (le magasinier qui réceptionne)
  toLocation, worksite
  receiverSignatureUrl, returnPhotoUrls
  lines : MaterialReturnLine
    materialIssueLine, item | asset | containerLocation
    returnedQuantity, conditionIn
    lostQuantity, damagedQuantity, consumedQuantity
    damageDescription, chargeToWorksite (boolean), chargeAmount
    requiresMaintenance (boolean) -> déclenche un AssetMaintenance CORRECTIVE
```

Un retour partiel laisse le bon en `PARTIALLY_RETURNED`. Le worker existant, étendu, produit une
alerte `MATERIAL_RETURN_OVERDUE` quand `expectedReturnAt + returnGraceDays` est dépassé, adressée au
destinataire **et** à son responsable hiérarchique.

### Responsabilité nominative et chaîne de détention

```text
CustodyEvent                   (journal unique de détention)
  id, subjectType              ASSET | CONTAINER | SERIAL
  subjectCode
  eventType                    ISSUED, TRANSFERRED, RETURNED, LOST, FOUND,
                               DAMAGED, SENT_TO_REPAIR, BACK_FROM_REPAIR, SCRAPPED
  fromHolderType, fromHolderCode
  toHolderType, toHolderCode
  locationCode, worksiteCode, containerCode, vehicleCode
  occurredAt, recordedAt, recordedByCode
  reason, referenceType, referenceCode
  signatureUrl, photoUrls, gpsLatitude, gpsLongitude
```

Ce journal répond directement à la question « où est le marteau-piqueur numéro 12 et qui l'a eu en
dernier ». Un seul endpoint le restitue :

```text
GET /inventory/custody/{subjectType}/{subjectCode}/timeline
```

### Endpoints de sortie de matériel

```text
POST  /inventory/issues                          créer un bon de sortie
PUT   /inventory/issues/{issueCode}              modifier tant qu'il est en DRAFT
POST  /inventory/issues/{issueCode}/lines        ajouter des lignes
PATCH /inventory/issues/{issueCode}/submit
PATCH /inventory/issues/{issueCode}/approve | /reject
PATCH /inventory/issues/{issueCode}/issue        remise physique, signatures, génère les mouvements
PATCH /inventory/issues/{issueCode}/cancel
GET   /inventory/issues                          filtres : chantier, destinataire, magasinier, statut,
                                                 période, retards, caisse, véhicule
GET   /inventory/issues/{issueCode}
GET   /inventory/issues/{issueCode}/voucher.pdf  bon de sortie signé
GET   /inventory/issues/overdue                  bons en retard de retour

POST  /inventory/issues/{issueCode}/returns      enregistrer un retour total ou partiel
GET   /inventory/returns/{returnCode}
GET   /inventory/returns/{returnCode}/voucher.pdf

GET   /inventory/holders/{holderType}/{holderCode}/holdings   ce qu'une personne détient aujourd'hui
GET   /inventory/worksites/{worksiteCode}/holdings            ce qu'un chantier détient
GET   /inventory/custody/{subjectType}/{subjectCode}/timeline
```

## 3.4 Caisses, conteneurs et contenu

### Composition théorique et réelle

```text
ContainerTemplate                (modèle de caisse : « caisse électricien niveau 1 »)
  id, templateCode, name, description, containerType
  lines : ContainerTemplateLine
    item | assetItemCode, expectedQuantity, mandatory (boolean), replacementCost

Container                        (instance physique, doublée d'un InventoryLocation TOOLBOX)
  id, containerCode, name, template
  linkedLocationCode             l'emplacement qui porte le stock
  linkedAssetCode                l'asset qui porte la valeur et la maintenance
  containerType                  TOOLBOX, TOOL_CHEST, CRATE, SHIPPING_CONTAINER, SITE_HUT,
                                 TRAILER, SKIP, CABINET, VAN_RACK
  currentHolderType, currentHolderCode
  currentWorksiteCode, currentVehicleAssetCode, currentLocationCode
  sealNumber, sealedAt, sealedByCode
  status                         IN_DEPOT, IN_TRANSIT, ON_SITE, IN_VEHICLE,
                                 WITH_HOLDER, UNDER_AUDIT, LOST, RETIRED
  lastAuditAt, lastAuditResult
```

### Opérations

- **Check-out complet** : une ligne `MaterialIssueLine` de type `CONTAINER` fait sortir la caisse et
  tout son contenu en un geste, avec `ContainerSnapshot` de la composition au départ.
- **Check-in complet** : au retour, comparaison automatique entre le snapshot de départ et le contenu
  constaté. Tout écart devient une ligne d'écart avec imputation possible au chantier ou au détenteur.
- **Audit de caisse** : `POST /inventory/containers/{containerCode}/audits`, comparaison avec le
  `ContainerTemplate`, production d'une liste de manquants et d'excédents, puis réapprovisionnement
  automatique depuis le dépôt (génération d'un bon de sortie de complètement).
- **Imbrication** : une caisse peut être dans un véhicule, lui-même sur un chantier. L'arborescence
  `InventoryLocation.parentLocation` supporte déjà cela ; il faut en revanche garantir la **cohérence
  transitive** (déplacer le véhicule déplace ses caisses et leur contenu) et interdire les cycles.
- **Scellés** : une caisse plombée ne peut être ouverte que par un rôle habilité, la rupture de scellé
  étant journalisée en `CustodyEvent`.

```text
POST  /inventory/container-templates              GET /inventory/container-templates
POST  /inventory/containers                       PUT /inventory/containers/{containerCode}
GET   /inventory/containers                       GET /inventory/containers/{containerCode}
GET   /inventory/containers/{containerCode}/contents
GET   /inventory/containers/{containerCode}/expected-vs-actual
POST  /inventory/containers/{containerCode}/audits
PATCH /inventory/containers/{containerCode}/seal | /break-seal
POST  /inventory/containers/{containerCode}/replenish
GET   /inventory/containers/{containerCode}/history
```

## 3.5 Dépôts multiples et logistique inter-sites

Le `StockTransferWorkflow` existant (`REQUESTED → APPROVED → SHIPPED → RECEIVED`) est mono-article.
Il faut le passer en **multi-lignes** et l'enrichir de la logistique :

```text
StockTransferWorkflow (étendu)
  lines : StockTransferLine (item, quantity, lot, série, conditionnement)
  vehicleAssetCode, driverCode, plateNumber
  departureAt, expectedArrivalAt, arrivalAt
  transportCost, transportSupplierCode
  waybillNumber, waybillDocumentCode
  transitLocationCode         emplacement TRANSIT qui porte le stock en route
  receivedQuantityByLine, shortageQuantity, damageQuantity, discrepancyNote
  status ajouts : IN_TRANSIT, PARTIALLY_RECEIVED, DISPUTED
```

Le stock expédié quitte le dépôt source vers un emplacement `TRANSIT` dédié, et n'entre au dépôt
destination qu'à la réception. Cela rend le stock en route visible et évite les disparitions
comptables pendant le transport, point sensible quand un dépôt est à plusieurs heures de route.

Ajouts nécessaires :
- **Hiérarchie dépôt / magasin de chantier** avec droits différenciés : un magasinier de chantier ne
  voit et ne mouvemente que son emplacement et ses enfants.
- **Réapprovisionnement de chantier** : règles `InventoryReorderRule` par chantier, générant une
  demande de transfert et non une demande d'achat.
- **Navette récurrente** : planification de tournées dépôt → chantiers.

## 3.6 Leasing et location d'équipement

Deux sens à couvrir, plus le crédit-bail.

### Location sortante (l'entreprise loue à des tiers)

```text
EquipmentLease
  id, leaseCode, leaseDirection   OUTBOUND | INBOUND
  leaseType                       OPERATING_HIRE, DRY_HIRE, WET_HIRE (avec opérateur),
                                  FINANCE_LEASE, HIRE_PURCHASE
  status                          QUOTE, RESERVED, CONFIRMED, ACTIVE, OFF_HIRE_REQUESTED,
                                  RETURNED, CLOSED, CANCELLED, DEFAULTED

  counterpartyType                CUSTOMER | SUPPLIER | SUBCONTRACTOR
  counterpartyCode
  contractCode                    lien vers features/contract (contrat signé)
  worksiteCode                    chantier d'utilisation
  deliveryAddress, siteContactName, siteContactPhone

  startAt, expectedEndAt, actualEndAt, minimumHireDays
  billingCycle                    HOURLY, DAILY, WEEKLY, MONTHLY, PER_USE
  rateAmount, currencyCode
  includedHoursPerPeriod          franchise horaire incluse
  excessHourRate                  tarif de l'heure au-delà de la franchise
  deliveryFee, collectionFee
  depositAmount, depositStatus    HELD, PARTIALLY_REFUNDED, REFUNDED, FORFEITED
  insuranceRequired, insurancePolicyNumber, insuranceExpiryDate
  operatorIncluded, operatorUserCode
  fuelPolicy                      FULL_TO_FULL, INCLUDED, BILLED_ON_USE
  lateReturnPenaltyPerDay
  damageWaiverAmount

  billingCustomerCode             client facturé (peut différer du détenteur)
  nextInvoiceDate, lastInvoicedUntil
  totalInvoicedAmount
  createdAt, updatedAt

EquipmentLeaseLine
  lease, asset | item, quantity
  rateAmount, meterStart, meterEnd, meterUnit
  conditionOut, conditionIn
  checkOutReportCode, checkInReportCode   états des lieux
  offHiredAt
```

Mécanique :
- **Disponibilité et planning** : une réservation de location pose un `AssetReservation` sur une plage
  de dates, avec détection de conflit et proposition d'équipement de substitution.
- **État des lieux** (`ConditionReport`) au départ et au retour : photos horodatées et géolocalisées,
  relevé de compteur, niveau de carburant, liste des accessoires, signature des deux parties, PDF
  archivé. C'est la pièce qui protège juridiquement en cas de litige.
- **Facturation récurrente** : à chaque échéance, le module produit une ligne à destination de
  `features/billing` (le module gère déjà séries, remises, ventilation fiscale, avoirs). Le calcul
  couvre le prorata de début et de fin, la franchise horaire, les heures excédentaires relevées, le
  carburant, les frais de livraison et de reprise, et les pénalités de retard.
- **Off-hire** : demande d'arrêt de location datée, qui fige la fin de facturation même si la reprise
  physique intervient plus tard.
- **Caution** : blocage à la signature, restitution ou retenue au retour en fonction de l'état des
  lieux, via `features/payment`.
- **Suspension** : un arrêt de chantier (intempéries) peut suspendre la facturation si le contrat le
  prévoit ; le statut `SUSPENDED` et une table `LeaseSuspension` (motif, période) le couvrent.

### Location entrante (l'entreprise loue à un loueur)

Même entité avec `leaseDirection = INBOUND`. Le matériel loué entre en stock en `StockOwnershipType`
existant `SUPPLIER`, donc **sans valorisation** au bilan, mais avec suivi de détention identique. La
charge de location est imputée au chantier au prorata de la durée d'utilisation, ce qui alimente le
coût de revient. Alertes sur matériel loué **inutilisé depuis N jours** : c'est le principal poste
d'économie sur un parc loué.

### Crédit-bail et financement

```text
LeaseFinancePlan
  lease, assetCode, financierName, contractNumber
  principalAmount, interestRatePercent, termMonths
  residualValue, purchaseOptionAmount, purchaseOptionDate
  firstInstalmentDate, instalmentAmount, instalmentCount
  schedule : LeaseInstalment (dueDate, principalPart, interestPart, status, paymentCode)
  accountingTreatment  OPERATING | FINANCE
```

Couplé à un **plan d'amortissement** de l'asset, à créer :

```text
AssetDepreciation
  asset, method      LINEAR | DECLINING | UNITS_OF_PRODUCTION
  startDate, usefulLifeMonths, residualValue, depreciationBase
  schedule : AssetDepreciationLine (period, amount, accumulated, netBookValue, posted)
```

`Asset` porte déjà `purchaseCost`, `usefulLifeMonths` et `residualValue` : le plan d'amortissement
est un ajout naturel, et il donne la valeur nette comptable nécessaire à l'arbitrage
« réparer ou remplacer ».

### Endpoints leasing

```text
POST  /inventory/leases                           créer un contrat de location
PATCH /inventory/leases/{leaseCode}/confirm | /activate | /off-hire | /close | /cancel
PATCH /inventory/leases/{leaseCode}/suspend | /resume
GET   /inventory/leases                           filtres direction, client, chantier, statut, échéance
GET   /inventory/leases/{leaseCode}
POST  /inventory/leases/{leaseCode}/condition-reports/{phase}   phase = out | in
GET   /inventory/leases/{leaseCode}/condition-reports/{id}.pdf
POST  /inventory/leases/{leaseCode}/meter-readings
POST  /inventory/leases/{leaseCode}/invoice-run   déclenche la facturation de la période
GET   /inventory/leases/{leaseCode}/billing-preview
GET   /inventory/leases/availability              calendrier de disponibilité du parc
POST  /inventory/leases/{leaseCode}/finance-plan
GET   /inventory/assets/{assetCode}/depreciation
```

## 3.7 Engins, compteurs et maintenance réglementaire

### Compteurs

```text
AssetMeter
  asset, meterType     ENGINE_HOURS, ODOMETER_KM, CYCLES, FUEL_LITERS
  currentValue, unit, lastReadingAt, rolloverValue

AssetMeterReading
  asset, meterType, value, readingAt, readByCode, source  MANUAL | TELEMATICS | LEASE_CHECKIN
  worksiteCode, photoUrl, delta (calculé)
```

La maintenance préventive devient déclenchable **sur compteur** et pas seulement sur date :

```text
MaintenancePlan
  asset | itemCode (modèle), planCode, name
  triggerType       CALENDAR | METER | BOTH
  intervalDays, intervalMeterValue, meterType
  toleranceDays, toleranceMeterValue
  lastPerformedAt, lastPerformedMeterValue
  nextDueAt, nextDueMeterValue
  taskChecklist     liste d'opérations
  estimatedCost, estimatedDowntimeHours
  requiredPartItemCodes   pièces de rechange à réserver
```

Le worker existant gagne une détection `MAINTENANCE_DUE_BY_METER`, et une réservation automatique des
pièces de rechange via `InventoryConsumptionService`.

### Contrôles réglementaires

Poste incontournable en BTP : les engins de levage, échafaudages, harnais et EPI sont soumis à des
vérifications générales périodiques.

```text
AssetInspection
  asset, inspectionType   VGP, LIFTING_TEST, ELECTRICAL, PRESSURE_VESSEL,
                          ROADWORTHINESS, CALIBRATION, PPE_CHECK
  regulationReference, inspectionBody, inspectorName
  performedAt, validUntil
  result                  PASS, PASS_WITH_RESERVE, FAIL
  reserves                observations à lever
  certificateDocumentCode
  cost
```

Règle métier forte : un asset dont un contrôle obligatoire est **expiré** ne peut pas être affecté ni
loué. Le service d'affectation doit refuser la sortie, avec un message explicite, et une possibilité
de dérogation tracée réservée à un rôle dédié. Nouvelles alertes : `INSPECTION_DUE_SOON`,
`INSPECTION_EXPIRED`, `PPE_EXPIRED`.

### Carburant et utilisation

```text
FuelLog
  asset, worksiteCode, fuelItemCode, quantityLiters, unitCost, totalCost
  meterValue, filledAt, filledByCode, supplierCode, vehicleTankCode
  consumptionPerHour (calculé)

AssetUtilization (agrégat quotidien)
  asset, date, worksiteCode
  hoursWorked, hoursIdle, hoursDown
  availabilityPercent, utilizationPercent
  internalHireAmount    coût interne refacturé au chantier
```

Les écarts de consommation par rapport à la moyenne de la machine constituent un signal fiable de
détournement de carburant : alerte `ABNORMAL_FUEL_CONSUMPTION`.

## 3.8 Personnes, habilitations et EPI

```text
PpeIssue
  employeeCode, item (PPE), size, quantity
  issuedAt, issuedByCode, signatureUrl
  expiresAt              calculé depuis ppeLifespanMonths
  replacementReason      ROUTINE, DAMAGED, LOST, SIZE_CHANGE, EXPIRED
  chargedToEmployee (boolean), chargeAmount
  worksiteCode

EmployeeCertification
  employeeCode, certificationCode   CACES_R482_A, CACES_R486, HABILITATION_B0, ...
  issuedAt, validUntil, certificateDocumentCode, issuingBody
```

Blocages à implémenter :
- refus d'affectation d'un engin à un employé sans la certification exigée par
  `InventoryItem.certificationCode`, ou dont la certification est expirée ;
- alerte `CERTIFICATION_EXPIRING` à 60 et 30 jours ;
- dotation EPI obligatoire par métier, avec tableau de conformité par chantier
  (`GET /inventory/worksites/{code}/ppe-compliance`).

## 3.9 Coût de revient chantier

Toute sortie imputée à un chantier alimente un coût. Les sources sont :

| Source | Montant imputé |
|---|---|
| `MaterialIssue` ligne `ITEM` consommée | quantité × coût valorisé (CMUP ou FIFO) |
| `MaterialIssue` ligne `ASSET` | coût de location interne × durée de détention |
| `EquipmentLease` INBOUND | loyer au prorata de la période d'affectation au chantier |
| `FuelLog` | coût du carburant |
| `AssetMaintenance` | coût des réparations imputables à un chantier |
| Casse et perte constatées au retour | valeur de remplacement |

```text
WorksiteCostEntry
  worksite, costType    MATERIAL, EQUIPMENT_HIRE, FUEL, MAINTENANCE, LOSS, TRANSPORT, LABOR
  amount, currencyCode, quantity, unitCode
  occurredAt, sourceType, sourceCode
  itemCode, assetCode, budgetLineCode
  reversed, reversalOfEntryCode
```

Restitutions attendues :

```text
GET /inventory/worksites/{code}/cost-summary       par nature, par période
GET /inventory/worksites/{code}/budget-vs-actual   consommé contre budget, avec alerte de dépassement
GET /inventory/worksites/{code}/material-usage     quantités consommées par article
GET /inventory/worksites/{code}/equipment-days     jours-machine par engin
GET /inventory/worksites/{code}/wastage            écart entre métré théorique et consommé réel
```

Le **métré théorique** (quantité prévue par article pour le chantier) est à saisir en début de
chantier (`WorksiteMaterialBudget`) pour rendre l'écart de freinte exploitable. C'est ce qui
transforme le module d'un outil de suivi en un outil de pilotage.

## 3.10 Terrain, mobilité et preuve

- **Étiquetage universel** : QR sur article, asset, caisse, emplacement, véhicule, chantier. Le module
  admin génère déjà des étiquettes ; il faut étendre les types et prévoir des étiquettes résistantes
  (gravure, plaque métal) pour les engins.
- **Endpoint de scan unique** : `GET /inventory/scan/{code}` renvoyant la nature de l'objet et les
  actions possibles selon le rôle et le contexte (au dépôt, sur chantier).
- **Parcours de sortie en mobilité** : scanner le destinataire (badge), scanner la caisse, scanner
  chaque article, faire signer, valider. Le bon est créé en une seule requête
  `POST /inventory/issues/quick` prenant une liste de codes scannés.
- **Mode hors ligne** : les chantiers ont une couverture réseau incertaine. File locale d'opérations
  avec identifiant d'idempotence (le module `core/idempotency` existe déjà) et synchronisation
  différée, avec résolution des conflits côté serveur.
- **Preuves** : signature tactile, photo, GPS et horodatage serveur sur chaque remise et chaque
  retour. Ces éléments doivent être scellés dans le PDF archivé, comme le module `billing` scelle
  déjà ses documents.
- **Géorepérage** : alerte si un asset géolocalisé quitte le périmètre de son chantier d'affectation
  (`ASSET_OUT_OF_GEOFENCE`), utile contre le vol d'engins.

## 3.11 Sous-traitants et matériel confié

```text
SubcontractorCustody
  subcontractorCode, worksiteCode
  materialIssueCode
  contractualResponsibility  FULL | SHARED | NONE
  insuranceCertificateCode, insuranceExpiryDate
  expectedReturnAt, actualReturnAt
  deductionInvoiceCode       retenue sur situation en cas de non-restitution
```

Le non-retour de matériel confié doit pouvoir générer automatiquement une **retenue** sur la
situation de travaux du sous-traitant, via `features/billing`.

---

## 3.12 Petit outillage : dotation, consigne et casse

Le petit outillage (paliers T1 et T2) représente rarement plus de 3 % de la valeur du parc, mais
concentre l'essentiel des disparitions. La réponse n'est pas de le suivre pièce par pièce, c'est de
le **responsabiliser statistiquement**.

```text
ToolAllowance                       (dotation standard par métier)
  id, allowanceCode, name
  tradeCode                         maçon, coffreur, ferrailleur, électricien, plombier, soudeur
  lines : ToolAllowanceLine
    item, quantity, tier, replacementValue
    renewalPeriodMonths             périodicité normale de renouvellement
    mandatory                       boolean

EmployeeToolHolding                 (ce qu'un ouvrier détient réellement)
  employeeCode, item, quantityHeld
  lastIssuedAt, lastVerifiedAt
  allowanceCode, varianceVsAllowance
  totalReplacementValue

ToolLossRecord
  employeeCode | crewCode, worksiteCode, item, quantity
  lossType                          BREAKAGE, WEAR, LOSS, THEFT, NOT_RETURNED
  declaredAt, declaredByCode, witnessedByCode
  withinToleratedRate               boolean
  replacementValue
  chargeDecision                    NO_CHARGE, CHARGED_TO_WORKSITE, CHARGED_TO_EMPLOYEE, PENDING
  chargeAmount, chargeReference     lien vers la retenue si applicable
  approvedByCode, approvedAt
```

Règles :
- une **consigne** peut être posée sur l'outillage remis à un ouvrier, restituée au départ de
  l'entreprise si le matériel est rendu. Toute retenue sur rémunération doit rester soumise à
  approbation explicite d'un rôle habilité, être tracée et être plafonnée par un paramètre, car elle
  est encadrée juridiquement ;
- le **taux de casse admis** par métier et par période neutralise l'usure normale : une truelle usée
  ne doit pas générer de procédure ;
- un **tableau de conformité de dotation** par équipe (`GET /inventory/crews/{code}/tool-compliance`)
  montre les écarts entre dotation théorique et détention réelle ;
- les **consommables d'outillage** (T0) se suivent par ratio d'usage : nombre de disques par
  meuleuse et par mois, forets par perceuse. Une dérive signale soit un usage abusif, soit un
  détournement, soit une qualité d'achat insuffisante. Alerte `ABNORMAL_TOOL_CONSUMPTION`.

```text
POST  /inventory/tool-allowances                  GET /inventory/tool-allowances
GET   /inventory/employees/{code}/tool-holdings
POST  /inventory/tool-losses                      PATCH /inventory/tool-losses/{code}/approve
GET   /inventory/crews/{crewCode}/tool-compliance
GET   /inventory/analytics/tool-loss-rate         par équipe, métier, chantier, période
```

## 3.13 Flotte de véhicules et engins lourds

Les paliers T4 et T5 demandent un traitement de flotte à part entière, absent du module actuel.

```text
FleetVehicle                        (extension d'Asset, relation 1 à 1)
  asset, plateNumber, vinNumber, engineNumber
  makeCode, modelCode, yearOfManufacture
  vehicleCategory                   TRUCK, TIPPER, MIXER, LOWLOADER, VAN, CAR, PICKUP,
                                    EXCAVATOR, LOADER, DOZER, CRANE, MEWP, TELEHANDLER,
                                    ROLLER, GRADER, DRILL_RIG, GENERATOR_TRAILER
  grossWeightKg, payloadKg, axleCount
  fuelType, tankCapacityLiters, averageConsumption
  selfPropelled                     boolean, distingue engin automoteur et machine tractée
  roadLegal                         boolean, autorise ou non la circulation routière
  requiresEscort                    boolean, convoi exceptionnel
  telematicsDeviceId, telematicsProvider
  homeDepotCode, currentDriverCode

VehicleDocument
  asset, documentType               REGISTRATION, INSURANCE, TECHNICAL_INSPECTION, ROAD_TAX,
                                    TRANSPORT_LICENCE, CRANE_CERTIFICATE, EXCEPTIONAL_CONVOY_PERMIT,
                                    POLLUTION_CERTIFICATE
  reference, issuedBy, issuedAt, validUntil
  documentCode                      archivage via features/document
  renewalCost, renewalLeadDays
  blocking                          boolean, empêche la sortie si expiré

TyreRecord
  asset, position                   FL, FR, RL, RR, SPARE, ou position d'engin
  tyreItemCode, serialNumber, brand, size
  fittedAt, fittedAtMeterValue, removedAt, removedAtMeterValue
  treadDepthMm, lastCheckedAt
  status                            NEW, IN_SERVICE, WORN, RETREADED, SCRAPPED
  costPerKm                         calculé

AssetIncident
  asset, incidentType               ACCIDENT, THEFT, VANDALISM, BREAKDOWN, FIRE, OVERTURN,
                                    THIRD_PARTY_DAMAGE
  occurredAt, reportedAt, reportedByCode
  worksiteCode, driverCode
  locationDescription, latitude, longitude
  description, photoUrls, policeReportNumber
  thirdPartyInvolved, thirdPartyDetails
  insuranceClaimNumber, claimStatus PENDING, ACCEPTED, REJECTED, SETTLED
  deductibleAmount, repairCost, indemnityAmount
  downtimeDays, replacementAssetCode

AssetDowntime
  asset, startAt, endAt
  cause                             MAINTENANCE, BREAKDOWN, INSPECTION, ACCIDENT,
                                    MISSING_OPERATOR, NO_WORK, WEATHER, ADMINISTRATIVE
  worksiteCode, estimatedLostRevenue, notes
```

Fonctions attendues :
- **Blocage documentaire** : un véhicule dont l'assurance ou la visite technique est expirée ne peut
  être ni affecté, ni loué, ni pointé comme actif. Même mécanique que les inspections, avec dérogation
  tracée.
- **Télématique** : ingestion des remontées GPS et CAN (heures moteur, kilométrage, ralenti,
  consommation, codes défaut) alimentant `AssetMeterReading` sans saisie humaine. Un endpoint
  d'ingestion générique `POST /inventory/fleet/telematics/events` avec mapping par fournisseur évite
  de coupler le module à un constructeur.
- **Rapprochement carte carburant** : import du relevé du pétrolier, rapprochement automatique avec
  les `FuelLog`, détection des écarts (plein supérieur à la capacité du réservoir, deux pleins
  rapprochés, plein hors zone du chantier).
- **Transport d'engins** : un engin non routier se déplace sur porte-char. `PlantMovementOrder`
  (engin transporté, porteur, chauffeur, itinéraire, autorisations, coût, dates) avec son propre
  bon et son imputation au chantier destinataire.
- **Coût total de possession** par engin : achat ou loyer, amortissement, carburant, lubrifiants,
  pneumatiques, entretien, réparations, assurance, documents, immobilisation. C'est l'indicateur qui
  arbitre entre garder, réparer, remplacer ou louer.

```text
POST  /inventory/fleet/vehicles                   PUT /inventory/fleet/vehicles/{assetCode}
GET   /inventory/fleet/vehicles                   filtres catégorie, dépôt, disponibilité, document expiré
POST  /inventory/fleet/vehicles/{assetCode}/documents
GET   /inventory/fleet/documents/expiring
POST  /inventory/fleet/vehicles/{assetCode}/tyres
POST  /inventory/fleet/incidents                  PATCH /inventory/fleet/incidents/{code}/settle
POST  /inventory/fleet/telematics/events
POST  /inventory/fleet/fuel-cards/import
GET   /inventory/fleet/fuel-cards/reconciliation
POST  /inventory/fleet/plant-movements
GET   /inventory/fleet/vehicles/{assetCode}/tco
GET   /inventory/fleet/availability               calendrier de la flotte
```

## 3.14 Atelier interne et réparations

Beaucoup d'entreprises de BTP réparent en interne. Sans ordre de travail, le coût de la main d'oeuvre
mécanicien et des pièces disparaît, et l'arbitrage réparer ou remplacer devient impossible.

```text
WorkOrder
  id, workOrderCode, asset, maintenanceCode
  workshopLocationCode
  type                     REPAIR, SERVICE, OVERHAUL, MODIFICATION, PREPARATION, INSPECTION_FIX
  priority                 LOW, NORMAL, HIGH, URGENT, IMMOBILIZING
  status                   REQUESTED, DIAGNOSED, AWAITING_PARTS, IN_PROGRESS, TESTED,
                           COMPLETED, CANCELLED, SENT_EXTERNAL
  requestedByCode, requestedAt, worksiteCode
  diagnosis, resolution
  assignedMechanicCodes
  plannedStartAt, actualStartAt, completedAt
  meterValueAtEntry
  externalProviderCode, externalQuoteAmount, externalInvoiceCode

WorkOrderPart                       (pièces consommées, réserve puis consomme le stock)
  workOrder, item, quantity, unitCost, stockMovementCode
  sourceType               STOCK | PURCHASED | WARRANTY | CANNIBALIZED

WorkOrderLabour
  workOrder, mechanicCode, startedAt, endedAt, hours, hourlyRate, amount, operationDescription
```

Points de conception :
- un `WorkOrder` **réserve** ses pièces via `InventoryConsumptionService` dès le diagnostic, ce qui
  évite qu'un autre chantier parte avec le joint attendu ;
- le passage en `AWAITING_PARTS` déclenche automatiquement une demande d'achat si la pièce manque ;
- la **cannibalisation** (prendre une pièce sur un engin en fin de vie) est un flux réel : elle doit
  générer un mouvement de stock depuis l'asset source et diminuer sa valeur ;
- le coût cumulé des réparations d'un engin, rapporté à sa valeur nette comptable, alimente une
  alerte `REPAIR_COST_EXCEEDS_VALUE` ;
- le temps d'immobilisation alimente `AssetDowntime` et donc le taux de disponibilité.

## 3.15 Pointage journalier du matériel sur chantier

C'est le mécanisme qui, en pratique, détecte les pertes en 24 heures au lieu de six mois. Chaque
matin, le chef de chantier confirme la présence du matériel qui lui est affecté.

```text
PlantRollCall
  id, rollCallCode, worksite, rollCallDate
  submittedByCode, submittedAt, status  PENDING, SUBMITTED, VALIDATED, DISPUTED
  weatherCondition, siteActive          boolean
  lines : PlantRollCallLine
    subjectType            ASSET | CONTAINER | ITEM_POOL
    subjectCode
    expectedPresent        boolean
    reportedStatus         PRESENT_WORKING, PRESENT_IDLE, PRESENT_BROKEN,
                           ABSENT_TRANSFERRED, ABSENT_UNKNOWN, ABSENT_STOLEN
    hoursWorked, meterValue
    operatorCode
    photoUrl, notes
```

Effets :
- la liste attendue est calculée automatiquement à partir des affectations en cours, donc le chef de
  chantier ne saisit que les écarts ;
- un `ABSENT_UNKNOWN` ouvre immédiatement une alerte et un `CustodyEvent` de type `LOST` en attente
  de confirmation ;
- les heures pointées alimentent `AssetUtilization` et le coût de location interne du chantier ;
- deux pointages manquants consécutifs génèrent une alerte au responsable matériel
  (`ROLL_CALL_MISSING`) ;
- un `PRESENT_IDLE` répété sur un engin loué déclenche `IDLE_HIRED_EQUIPMENT`, l'alerte qui rapporte
  le plus vite sur un parc en location.

```text
GET   /inventory/worksites/{code}/roll-calls/today     pré-rempli avec l'attendu
POST  /inventory/worksites/{code}/roll-calls
PATCH /inventory/roll-calls/{code}/validate | /dispute
GET   /inventory/roll-calls                            filtres chantier, date, écarts uniquement
GET   /inventory/analytics/idle-equipment
```

## 3.16 Demandes de matériel et planification des besoins

Aujourd'hui le module part de la sortie. En BTP, le flux commence en amont : le chantier **demande**,
le dépôt **prépare**, puis **remet**.

```text
MaterialRequisition
  id, requisitionCode, worksite, requestedByCode, requestedAt
  requiredByDate, priority           ROUTINE, URGENT, EMERGENCY
  status                             DRAFT, SUBMITTED, APPROVED, REJECTED, IN_PREPARATION,
                                     READY, ISSUED, PARTIALLY_FULFILLED, CLOSED, CANCELLED
  approvedByCode, approvedAt, rejectionReason
  targetLocationCode, phaseCode
  lines : MaterialRequisitionLine
    item | assetItemCode, quantity, unitCode
    requiredByDate, purpose
    availableQuantity                calculé à la soumission
    allocatedQuantity, issuedQuantity
    fulfilmentSource                 STOCK | TRANSFER | PURCHASE | HIRE
    substituteItemCode
    linkedIssueCode, linkedPurchaseRequestCode, linkedTransferCode, linkedLeaseCode
```

Le moteur d'arbitrage est le point de valeur : pour chaque ligne, il propose la source la moins
coûteuse et la plus rapide, à savoir le stock du dépôt, le transfert depuis un autre chantier qui a
du disponible, l'achat, ou la location. Un chantier qui dort avec dix étais dont un autre a besoin
est un gisement d'économie immédiat.

```text
WorksiteMaterialPlan                 (planning des besoins, dérivé du planning de travaux)
  worksite, phaseCode, item
  plannedQuantity, unitCode
  neededFromDate, neededToDate
  sourcedQuantity, consumedQuantity
  confidence                         FIRM, PROBABLE, ESTIMATED
```

Ce plan alimente le calcul des besoins nets : besoin planifié moins stock disponible moins commandes
attendues, avec décalage par le délai fournisseur. C'est ce qui évite l'arrêt de chantier pour une
rupture de fer à béton et le sur-stock de ciment qui prend l'humidité.

```text
POST  /inventory/requisitions                     PATCH /inventory/requisitions/{code}/submit
PATCH /inventory/requisitions/{code}/approve | /reject
GET   /inventory/requisitions/{code}/sourcing-options
POST  /inventory/requisitions/{code}/allocate
POST  /inventory/requisitions/{code}/prepare      génère une liste de préparation
POST  /inventory/requisitions/{code}/issue        convertit en MaterialIssue
GET   /inventory/worksites/{code}/material-plan
GET   /inventory/planning/net-requirements        calcul des besoins nets, horizon paramétrable
```

## 3.17 Coffrages, échafaudages et matériel comptable à l'unité

Les banches, étais, madriers, tubes et colliers d'échafaudage constituent une catégorie à part :
comptés à l'unité, loués au mètre carré ou à la journée, jamais sérialisés individuellement, et
systématiquement sources de litige au retour.

```text
BulkEquipmentSet                     (parc d'un article comptable)
  item, totalOwnedQuantity
  quantityInDepot, quantityOnHire, quantityOnSite, quantityLost, quantityScrapped
  unitReplacementValue, unitHireRatePerDay
  countUnit                          PIECE, SQUARE_METER, LINEAR_METER, TON

BulkEquipmentMovement
  item, fromLocation, toLocation, worksiteCode
  quantitySent, quantityReturned, quantityMissing, quantityDamaged
  sentAt, returnedAt, countedByCode, counterpartySignatureUrl
  missingChargeAmount, damageChargeAmount
  reconciliationStatus               OPEN, RECONCILED, DISPUTED, WRITTEN_OFF
```

Points spécifiques :
- **comptage contradictoire** au départ et au retour, signé par les deux parties, sans quoi le litige
  est perdu d'avance ;
- **facturation des manquants** au tarif de remplacement, et des pièces abîmées au tarif de remise en
  état, avec génération automatique d'une ligne vers `billing` pour un client, ou d'un
  `WorksiteCostEntry` en interne ;
- **tarification à la surface et à la durée** : les banches se louent au mètre carré et par mois, les
  étais à l'unité et par jour. Le moteur de location doit accepter les deux bases ;
- **nettoyage et remise en état** entre deux chantiers, à traiter comme un `WorkOrder` de type
  `PREPARATION` avec son coût ;
- **rapprochement de parc** périodique : total possédé contre somme des positions, pour détecter
  l'érosion silencieuse.

## 3.18 Carburant en cuve de chantier

Le gasoil est un poste majeur et la première cible de détournement. Une cuve de chantier est un
emplacement de stock à part entière.

```text
FuelTank
  id, tankCode, name, linkedLocationCode, linkedAssetCode
  tankType                 FIXED_DEPOT, MOBILE_BOWSER, SITE_TANK, VEHICLE_TANK
  capacityLiters, currentLevelLiters
  fuelItemCode, worksiteCode
  hasFlowMeter, meterReading
  custodianCode, lastDipAt, lastDipLiters

FuelTransaction
  tank, transactionType    DELIVERY, DISPENSE, TRANSFER, DIP_ADJUSTMENT, LOSS
  quantityLiters, unitCost
  targetAssetCode          engin ou véhicule servi
  operatorCode, dispensedByCode
  meterValueBefore, meterValueAfter
  assetMeterValue          heures moteur ou kilométrage du récepteur
  occurredAt, worksiteCode
  supplierCode, deliveryNoteNumber
  authorizationCode        badge ou code conducteur
```

Contrôles attendus :
- **jaugeage périodique** confronté au stock théorique, écart au-delà d'un seuil déclenchant une
  alerte `FUEL_VARIANCE` ;
- **consommation spécifique** par engin, en litres par heure moteur ou aux cent kilomètres, comparée
  à la référence constructeur et à l'historique de la machine ;
- distribution **impossible sans identifier l'engin servi** et son relevé de compteur, ce qui rend le
  contrôle mécanique plutôt que déclaratif ;
- rapprochement des livraisons du pétrolier avec les bons de livraison et les factures.

## 3.19 Qualité et traçabilité réglementaire des matériaux

Sur un ouvrage, l'origine et la conformité des matériaux doivent être démontrables des années après
la réception. Le module doit porter cette preuve.

```text
MaterialCertificate
  id, certificateCode, item, lotNumber, stockLotId
  certificateType          CE_MARKING, CONFORMITY, MILL_CERTIFICATE, CONCRETE_MIX_DESIGN,
                           TEST_REPORT, SAFETY_DATA_SHEET, ORIGIN, ENVIRONMENTAL
  heatNumber               numéro de coulée pour l'acier
  manufacturerName, standardReference
  issuedAt, validUntil
  documentCode
  supplierCode, purchaseOrderCode, goodsReceiptCode

MaterialTest                         (essais en laboratoire)
  item, lotNumber, worksiteCode, structuralElementCode
  testType                 CONCRETE_COMPRESSION, SLUMP, AGGREGATE_GRADING, SOIL_COMPACTION,
                           STEEL_TENSILE, WELD_INSPECTION, MOISTURE
  sampledAt, sampledByCode, sampleReference
  testedAt, laboratoryName
  expectedValue, measuredValue, unit
  result                   PASS, FAIL, PENDING
  reportDocumentCode
```

Conséquences :
- un lot de ciment ou d'acier **sans certificat** ne peut pas être libéré du statut quarantaine.
  `StockLot.quarantined` existe déjà : il faut le workflow qui va avec ;
- la **traçabilité descendante** doit répondre à « quel acier est parti dans le voile du niveau 2 »,
  donc lier le lot à l'élément d'ouvrage via `MaterialIssueLine.structuralElementCode` ;
- un essai en échec déclenche un blocage du lot restant et une alerte à la maîtrise d'oeuvre ;
- les **fiches de données de sécurité** conditionnent le stockage de produits dangereux, ce qui
  amène l'entité suivante.

```text
HazardousMaterialProfile
  item, hazardClass, unNumber, packingGroup
  storageRequirements      VENTILATED, FIREPROOF, SEPARATE_FROM, TEMPERATURE_RANGE
  incompatibleItemCodes
  maxStorageQuantity, requiresRetentionTank
  sdsDocumentCode, sdsRevisionDate
  requiredPpeItemCodes
  disposalRoute
```

Le module refuse le stockage d'incompatibles au même emplacement et impose les EPI associés à la
sortie du produit.

## 3.20 Déchets, rebuts et valorisation

Un chantier produit autant de sortant que d'entrant. Ignorer ce flux fausse l'inventaire et expose
réglementairement.

```text
WasteDisposal
  id, disposalCode, worksite
  wasteType                INERT, NON_HAZARDOUS, HAZARDOUS, GREEN, METAL, WOOD, CONCRETE, ASBESTOS
  itemCode                 si le déchet provient d'un article du catalogue
  quantity, unitCode, containerCode
  collectedAt, carrierCode, vehiclePlate
  destinationFacility, facilityLicenceNumber
  trackingSlipNumber       bordereau de suivi de déchets
  documentCode
  disposalCost, resaleValue
  status                   PENDING, COLLECTED, DELIVERED, CERTIFIED

ScrapRecovery                        (revente de ferraille, palettes, matériel réformé)
  sourceType               ASSET | STOCK | WASTE
  sourceCode, quantity, unitCode
  buyerCode, saleAmount, saleInvoiceCode
  approvedByCode, approvedAt
```

Le passage d'un asset en `RETIRED` doit obligatoirement choisir une issue : revente, cannibalisation,
don ou destruction, chacune tracée. C'est ce qui empêche un engin de disparaître des comptes sans
justification.

## 3.21 Consignes et emballages retournables

Palettes, tourets de câble, bouteilles de gaz industriel, big-bags, bennes fournisseur : tous
consignés, tous facturés si non retournés, et tous invisibles dans le module actuel.

```text
ReturnablePackaging
  id, packagingCode, item, packagingType  PALLET, DRUM, GAS_CYLINDER, CABLE_REEL, BIG_BAG,
                                          SKIP, CRATE, IBC
  supplierCode, depositAmount, dailyRentalAmount
  freeHoldingDays
  serialNumber, trackable             boolean

PackagingBalance                       (solde par tiers)
  counterpartyType    SUPPLIER | CUSTOMER
  counterpartyCode, packagingCode
  quantityHeld, quantityDue, depositExposure
  oldestHoldingDate, lastReconciledAt

PackagingMovement
  packagingCode, counterpartyCode, worksiteCode
  direction           RECEIVED | RETURNED | LOST | CHARGED
  quantity, occurredAt, referenceType, referenceCode, documentCode
```

Alertes `PACKAGING_OVERDUE` avant la fin de la période de franchise, et relevé de consignation
rapprochable avec celui du fournisseur. Sur un gros chantier, l'exposition en consignes de bouteilles
de gaz se chiffre en millions.

## 3.22 Vol, sécurité et immobilisation

Le vol d'engins et d'outillage est un risque structurel du secteur.

- **Déclaration de vol** structurée via `AssetIncident` de type `THEFT`, avec numéro de plainte,
  photos, dernier détenteur connu et dernière position, et diffusion automatique de la fiche
  signalétique de l'engin.
- **Géorepérage** par chantier et par dépôt, alerte `ASSET_OUT_OF_GEOFENCE` et alerte de mouvement en
  dehors des plages horaires de travail (`ASSET_MOVED_OUT_OF_HOURS`), qui est le signal le plus fiable.
- **Immobilisation à distance** pour les engins équipés, avec traçabilité stricte de qui a déclenché
  la commande et pourquoi.
- **Registre des clés et badges** : les clés d'engins sont un actif à part entière.

```text
KeyRegister
  keyCode, keyType         VEHICLE, PLANT, PADLOCK, CABINET, SITE_GATE, FUEL_CAP
  linkedAssetCode | linkedLocationCode
  copiesTotal, copiesIssued
  holders : KeyIssue (holderCode, issuedAt, returnedAt, signatureUrl)
  lostCount, lastRekeyedAt
```

- **Contrôle d'accès au dépôt** : toute entrée et sortie de matériel hors horaires ouvrés exige une
  autorisation nominative tracée.

## 3.23 Livraison directe, transferts entre chantiers et cession interne

Trois flux courants que le modèle actuel ne couvre pas.

**Livraison directe au chantier.** Le ciment et les agrégats ne passent jamais par le dépôt. La
réception se fait sur site, par le chef de chantier, souvent sans terminal. Il faut :
`GoodsReceipt` avec `location` de type `WORKSITE`, réception en mode dégradé (photo du bon de
livraison, quantité déclarée, régularisation ultérieure), pesée éventuelle au pont-bascule
(`weighbridgeTicketNumber`, poids brut, tare, net) et tolérance de réception sur le vrac.

**Transfert entre chantiers.** Un étai qui passe du chantier A au chantier B sans repasser par le
dépôt doit rester tracé et changer d'imputation analytique. Le `StockTransferWorkflow` étendu le
couvre, à condition d'autoriser `WORKSITE` en source et en destination, et de générer les deux
écritures de coût, sortie chez A, entrée chez B, à la valeur de transfert retenue.

**Cession interne et facturation inter-entités.** Si le groupe comporte plusieurs sociétés, un
transfert entre elles est une vente. Il faut un `InternalTransferPricing` (article ou catégorie,
entité source, entité destination, méthode de valorisation, marge appliquée) et la génération
automatique d'une facture inter-sociétés via `billing`.

```text
POST  /inventory/procurement/goods-receipts/site-delivery
POST  /inventory/stock/transfers/worksite-to-worksite
GET   /inventory/analytics/idle-stock-by-worksite      le disponible mobilisable ailleurs
POST  /inventory/settings/internal-transfer-pricing
```

---

# Partie 4 · Implications techniques

## 4.1 Nouvelles entités, récapitulatif

| Domaine | Entités à créer |
|---|---|
| Chantier | `Worksite`, `WorksiteMaterialBudget`, `WorksiteCostEntry` |
| Sortie de matériel | `MaterialIssue`, `MaterialIssueLine`, `MaterialReturn`, `MaterialReturnLine`, `CustodyEvent` |
| Contenants | `Container`, `ContainerTemplate`, `ContainerTemplateLine`, `ContainerSnapshot`, `ContainerAudit`, `ContainerAuditLine` |
| Location | `EquipmentLease`, `EquipmentLeaseLine`, `LeaseSuspension`, `ConditionReport`, `ConditionReportLine`, `LeaseFinancePlan`, `LeaseInstalment` |
| Engins | `AssetMeter`, `AssetMeterReading`, `MaintenancePlan`, `AssetInspection`, `FuelLog`, `AssetUtilization`, `AssetDepreciation`, `AssetDepreciationLine`, `AssetReservation` |
| Personnes | `PpeIssue`, `EmployeeCertification`, `ToolAllowance`, `ToolAllowanceLine`, `EmployeeToolHolding`, `ToolLossRecord` |
| Paliers de gestion | `ManagementProfile` |
| Flotte | `FleetVehicle`, `VehicleDocument`, `TyreRecord`, `AssetIncident`, `AssetDowntime`, `PlantMovementOrder` |
| Atelier | `WorkOrder`, `WorkOrderPart`, `WorkOrderLabour` |
| Terrain | `PlantRollCall`, `PlantRollCallLine`, `KeyRegister`, `KeyIssue` |
| Besoins | `MaterialRequisition`, `MaterialRequisitionLine`, `WorksiteMaterialPlan` |
| Parc comptable | `BulkEquipmentSet`, `BulkEquipmentMovement` |
| Carburant | `FuelTank`, `FuelTransaction` |
| Qualité matériaux | `MaterialCertificate`, `MaterialTest`, `HazardousMaterialProfile` |
| Déchets | `WasteDisposal`, `ScrapRecovery` |
| Consignes | `ReturnablePackaging`, `PackagingBalance`, `PackagingMovement` |
| Généraliste, référentiel | `InventoryBarcode`, `InventoryPackaging`, `InventoryItemTemplate`, `InventoryItemVariant`, `InventoryItemSubstitute`, `InventoryItemTranslation`, `InventoryItemPriceHistory`, `InventoryItemRevisionHistory` |
| Généraliste, valorisation | `StockCostLayer`, `StockValuationSnapshot`, `InventoryPeriod`, `StockJournalEntry`, `AdjustmentReason` |
| Généraliste, qualité | `QualityControlPlan`, `QualityCriterion`, `QualityInspection`, `QualityInspectionResult`, `NonConformance`, `ProductRecall`, `StockLotGenealogy` |
| Généraliste, planification | `ConsumptionForecast`, `AbcXyzClassification`, `NetRequirementRun`, `StockOutEvent` |
| Généraliste, achats | `RequestForQuotation`, `SupplierQuotation`, `SupplierAgreement`, `SupplierPriceTier`, `AdvanceShipmentNotice`, `SupplierReturn`, `SupplierClaim`, `LandedCostAllocation` |
| Généraliste, entrepôt | `PutAwayRule`, `AllocationStrategy`, `InternalReplenishmentTask`, `WarehouseTask`, `PickingList`, `PickingListLine`, `HandlingUnit`, `InventoryBom`, `InventoryBomLine` |
| Généraliste, tiers | `InternalTransferPricing` |

## 4.2 Enums à étendre

```text
InventoryLocationType   + DEPOT, WORKSITE, VEHICLE, CONTAINER, TOOLBOX, WORKSHOP,
                          QUARANTINE, SCRAP, TRANSIT, THIRD_PARTY
InventoryItemType       + MATERIAL, BULK_MATERIAL, TOOL, SMALL_TOOL, PPE, MACHINE, FORMWORK, FUEL
InventoryUnitType       + TON, CUBIC_METER, SQUARE_METER, LINEAR_METER, BAG, ROLL, BUNDLE,
                          PALLET, TRUCKLOAD, HOUR, DAY
AssetAssigneeType       + EMPLOYEE, SUBCONTRACTOR, WORKSITE, VEHICLE, CONTAINER
AssetStatus             + ON_HIRE, AWAITING_INSPECTION, IMMOBILIZED, IN_TRANSIT
StockReferenceType      + MATERIAL_ISSUE, MATERIAL_RETURN, EQUIPMENT_LEASE, CONTAINER_AUDIT,
                          WORKSITE_TRANSFER, SUPPLIER_RETURN
StockOutReasonCode      + WORKSITE_CONSUMPTION, WASTAGE, THEFT, SCRAP, RETURN_TO_SUPPLIER
StockTransferWorkflowStatus + IN_TRANSIT, PARTIALLY_RECEIVED, DISPUTED
InventoryAlertType      + MATERIAL_RETURN_OVERDUE, CONTAINER_INCOMPLETE, INSPECTION_DUE_SOON,
                          INSPECTION_EXPIRED, PPE_EXPIRED, CERTIFICATION_EXPIRING,
                          MAINTENANCE_DUE_BY_METER, ABNORMAL_FUEL_CONSUMPTION,
                          ASSET_OUT_OF_GEOFENCE, IDLE_HIRED_EQUIPMENT, LEASE_ENDING_SOON,
                          WORKSITE_BUDGET_EXCEEDED, DEPOSIT_PENDING_REFUND,
                          ABNORMAL_TOOL_LOSS_RATE, ABNORMAL_TOOL_CONSUMPTION,
                          VEHICLE_DOCUMENT_EXPIRED, VEHICLE_DOCUMENT_EXPIRING,
                          ASSET_MOVED_OUT_OF_HOURS, ROLL_CALL_MISSING, FUEL_VARIANCE,
                          REPAIR_COST_EXCEEDS_VALUE, PACKAGING_OVERDUE,
                          QUARANTINE_PENDING_RELEASE, CERTIFICATE_MISSING,
                          NON_CONFORMANCE_OPEN, SERVICE_LEVEL_BREACH, DORMANT_STOCK_VALUE
```

Nouveaux enums à créer :

```text
ManagementTier          T0, T1, T2, T3, T4, T5
TrackingMode            QUANTITY, POOL_BY_HOLDER, INDIVIDUAL_ASSET
IdentificationMode      NONE, LOT, TAG, SERIAL, PLATE
ReturnPolicy            NOT_EXPECTED, EXPECTED_BULK, MANDATORY_NOMINATIVE
MaintenanceMode         NONE, ON_FAILURE, CALENDAR, METER, METER_AND_CALENDAR
CostingMode             CONSUMED_ON_ISSUE, INTERNAL_HIRE_RATE, TCO
ValuationMethod         WEIGHTED_AVERAGE, FIFO, LIFO, STANDARD_COST, SPECIFIC_IDENTIFICATION
ItemLifecycleStatus     DRAFT, NEW, ACTIVE, PHASE_OUT, OBSOLETE, BLOCKED
MaterialIssueType       voir 3.3
MaterialIssuePurpose    voir 3.3
CustodyEventType        voir 3.3
ContainerType           voir 3.4
ContainerStatus         voir 3.4
LeaseDirection          OUTBOUND, INBOUND
LeaseType               OPERATING_HIRE, DRY_HIRE, WET_HIRE, FINANCE_LEASE, HIRE_PURCHASE
LeaseStatus             voir 3.6
BillingCycle            HOURLY, DAILY, WEEKLY, MONTHLY, PER_USE
MeterType               ENGINE_HOURS, ODOMETER_KM, CYCLES, FUEL_LITERS
InspectionType          voir 3.7
VehicleCategory         voir 3.13
VehicleDocumentType     voir 3.13
IncidentType            voir 3.13
DowntimeCause           voir 3.13
WorkOrderType           REPAIR, SERVICE, OVERHAUL, MODIFICATION, PREPARATION, INSPECTION_FIX
WorkOrderStatus         voir 3.14
RollCallStatus          PENDING, SUBMITTED, VALIDATED, DISPUTED
RollCallLineStatus      voir 3.15
RequisitionStatus       voir 3.16
FulfilmentSource        STOCK, TRANSFER, PURCHASE, HIRE
FuelTankType            FIXED_DEPOT, MOBILE_BOWSER, SITE_TANK, VEHICLE_TANK
FuelTransactionType     DELIVERY, DISPENSE, TRANSFER, DIP_ADJUSTMENT, LOSS
CertificateType         voir 3.19
MaterialTestType        voir 3.19
WasteType               voir 3.20
PackagingType           PALLET, DRUM, GAS_CYLINDER, CABLE_REEL, BIG_BAG, SKIP, CRATE, IBC
QualityDecision         ACCEPTED, REJECTED, QUARANTINED, ACCEPTED_BY_DEROGATION
NonConformanceDisposition  USE_AS_IS, REWORK, RETURN_TO_SUPPLIER, SCRAP, DOWNGRADE
```

Attention : l'ajout de valeurs à un enum persisté en `STRING` implique de mettre à jour les
contraintes `CHECK` correspondantes dans les migrations existantes.

## 4.3 Migrations

Le projet maintient **deux dossiers Flyway** avec des numérotations distinctes :
`src/main/resources/db/migration` (dev, actuellement à V215) et
`src/main/resources/db/migration-prod` (prod, actuellement à V211). Toute modification de schéma
nécessite un fichier dans **chacun** des deux dossiers, avec le numéro propre à chaque série.
`hibernate.ddl-auto` reste à `none`.

Découpage de migrations proposé :

| Migration | Contenu |
|---|---|
| `inventory_btp_worksite` | `worksite`, budgets, extension `inventory_location` (mobile, linkedAsset, custodian, geofence), nouveaux types d'emplacement |
| `inventory_btp_material_issue` | `material_issue`, lignes, retours, `custody_event`, séquences associées |
| `inventory_btp_container` | modèles de caisse, instances, snapshots, audits |
| `inventory_btp_asset_meters` | compteurs, relevés, plans de maintenance, inspections, carburant, utilisation |
| `inventory_btp_lease` | contrats de location, lignes, états des lieux, plans de financement, échéanciers |
| `inventory_btp_people` | EPI, certifications |
| `inventory_btp_costing` | `worksite_cost_entry`, agrégats, index analytiques |
| `inventory_management_profile` | `management_profile`, rattachement sur `inventory_item` et `asset` |
| `inventory_btp_tools` | dotations, détentions, pertes d'outillage |
| `inventory_btp_fleet` | `fleet_vehicle`, documents, pneumatiques, incidents, immobilisations, transports d'engins |
| `inventory_btp_workshop` | ordres de travail, pièces, main d'oeuvre |
| `inventory_btp_rollcall` | pointages journaliers, registre de clés |
| `inventory_btp_requisition` | demandes de matériel, plans de besoins |
| `inventory_btp_bulk_equipment` | parcs comptables, rapprochements |
| `inventory_btp_fuel` | cuves, transactions, jaugeages |
| `inventory_btp_material_quality` | certificats, essais, profils de produits dangereux |
| `inventory_btp_waste` | évacuations, valorisation des rebuts |
| `inventory_packaging_deposits` | emballages consignés, soldes, mouvements |
| `inventory_valuation_layers` | couches de coût, périodes, snapshots de valorisation, écritures de stock |
| `inventory_barcodes` | codes-barres multiples et conditionnements |
| `inventory_item_advanced` | cycle de vie, variantes, substituts, traductions, historique de prix |
| `inventory_quality_control` | plans de contrôle, inspections, non-conformités, rappels |
| `inventory_planning` | prévisions, classification ABC et XYZ, besoins nets, ruptures |
| `inventory_procurement_advanced` | appels d'offres, contrats-cadres, avis d'expédition, retours et litiges fournisseur, frais accessoires |
| `inventory_warehouse_ops` | règles de rangement et d'allocation, tâches, préparations, unités logistiques |

## 4.4 Séquences

Nouvelles séquences à déclarer dans le moteur `core/generator` : `MATERIAL_ISSUE` (BS),
`MATERIAL_RETURN` (BR), `WORKSITE` (CH), `CONTAINER` (CA), `EQUIPMENT_LEASE` (LOC),
`CONDITION_REPORT` (EDL), `ASSET_INSPECTION` (VGP), `CONTAINER_AUDIT` (AUD), `PPE_ISSUE` (EPI),
`MATERIAL_REQUISITION` (DM), `WORK_ORDER` (OT), `PLANT_ROLL_CALL` (PJ), `ASSET_INCIDENT` (SIN),
`PLANT_MOVEMENT_ORDER` (TRP), `WASTE_DISPOSAL` (BSD), `QUALITY_INSPECTION` (CQ),
`NON_CONFORMANCE` (NC), `REQUEST_FOR_QUOTATION` (AO), `SUPPLIER_RETURN` (RF),
`PICKING_LIST` (PREP), `TOOL_LOSS` (PERT).

## 4.5 RBAC

`AdminApiAuthorizationManager` mappe aujourd'hui tout `/inventory` sur la ressource `INVENTORY`.
Le BTP demande une granularité plus fine, car un magasinier de chantier, un chef de chantier, un
responsable matériel et un contrôleur de gestion n'ont pas les mêmes droits :

```text
Ressources proposées : INVENTORY (existante),
                       INVENTORY_ISSUE      créer et remettre du matériel
                       INVENTORY_CONTAINER  gérer les caisses et leurs audits
                       INVENTORY_LEASE      contrats de location
                       INVENTORY_FLEET      engins, véhicules, compteurs, inspections, sinistres
                       INVENTORY_WORKSHOP   ordres de travail et réparations
                       INVENTORY_QUALITY    contrôles, quarantaine, non-conformités
                       INVENTORY_COSTING    accès aux coûts et marges

Actions : READ, CREATE, UPDATE, DELETE, APPROVE,
          ISSUE           remise physique avec signature
          OVERRIDE        dérogation (sortie malgré inspection expirée, stock négatif)
          BREAK_SEAL      ouverture de caisse plombée
          WRITE_OFF       passage en perte
          RELEASE         libération de quarantaine
          CHARGE_HOLDER   imputation d'une perte à une personne
          IMMOBILIZE      immobilisation à distance d'un engin
```

Cloisonnement à ajouter : filtrage systématique par `businessCode` et par périmètre d'emplacements
autorisés (un magasinier de chantier ne voit que son arbre d'emplacements).

Profils métier attendus, à composer à partir de ces permissions : magasinier de dépôt, magasinier de
chantier, chef de chantier, conducteur de travaux, responsable matériel, mécanicien d'atelier,
responsable QSE, contrôleur de gestion, direction. Chacun avec un périmètre d'emplacements et de
chantiers, pas seulement un jeu de droits fonctionnels.

## 4.6 Intégrations avec les autres modules

| Module | Point d'intégration |
|---|---|
| `billing` | facturation des loyers, refacturation de matériel au client, retenues sous-traitant, avoirs sur casse |
| `contract` | contrat de location signé, conditions générales, avenants de prolongation |
| `document` | archivage des bons de sortie, états des lieux, certificats d'inspection, PV de réception |
| `payment` | cautions, encaissement des loyers, remboursement de dépôt de garantie |
| `notification` | relances de retour, alertes d'inspection, notifications de dépassement de budget |
| `analytics` / `reporting` | coût de revient chantier, taux d'utilisation du parc, rotation du stock |
| `client` | client du chantier, sous-traitants, employés destinataires |
| `task` | rattachement d'une sortie de matériel à une tâche planifiée |
| `ressource` | ne pas confondre : `ressource` gère les espaces réservables, `inventory` le matériel physique |

Attention à la **désambiguïsation du mot « caisse »** : le projet possède déjà un module de caisse
enregistreuse (`cash register`). Dans ce document, « caisse » désigne exclusivement un contenant à
outils, nommé `Container` dans le code pour éviter toute collision.

## 4.7 Performance

- Index composites sur `material_issue (worksite_code, status, issued_at)`,
  `custody_event (subject_type, subject_code, occurred_at DESC)`,
  `stock_movement (reference_type, reference_code)`.
- Les mouvements en cascade sur une caisse complète peuvent porter sur des dizaines de lignes : les
  traiter en lot, avec un seul verrou par emplacement, et pousser l'événement outbox une seule fois.
- Agrégats de coût chantier calculés en tâche de fond (`WorksiteCostEntry` en écriture,
  vues matérialisées ou tables d'agrégat en lecture) plutôt qu'à la volée.
- Recherche multi-critères (parc, disponibilité, localisation) à porter sur Elasticsearch, en
  réutilisant le paquet `search/` prévu mais jamais implémenté.

---

# Partie 5 · Feuille de route

L'ordre suit la dépendance technique et la valeur métier immédiate. Les lots 0 à 8 couvrent le BTP,
les lots 9 à 13 achèvent le socle généraliste. Les lots 0, 1 et 2 sont bloquants pour tout le reste.

### Lot 0 · Robustesse préalable

Contrainte d'immuabilité sur `stock_movement`, commande de recalcul et de réconciliation des niveaux,
tests de charge sur les sorties concurrentes, durcissement de la clé d'idempotence sur les endpoints
de mouvement. La concurrence et l'idempotence sont déjà en place : ce lot les vérifie et les durcit,
il ne les construit pas.

### Lot 1 · Socle BTP

`Worksite`, `ManagementProfile` et les six paliers, extension de `InventoryLocationType` et
d'`InventoryItemType`, emplacements mobiles, lien asset ↔ emplacement, nouvelles unités et
conversions vrac. Sans ce socle, rien d'autre ne tient.

### Lot 2 · Sortie de matériel et détention

`MaterialIssue`, `MaterialIssueLine`, `MaterialReturn`, `CustodyEvent`, PDF de bon de sortie avec
double signature, alertes de retard, endpoints de détention par personne et par chantier.
C'est le cœur de la demande, et le lot qui produit la valeur perçue la plus forte.

### Lot 3 · Caisses et contenants

`ContainerTemplate`, `Container`, snapshots, audits, réapprovisionnement automatique, scellés,
sortie et retour en cascade.

### Lot 4 · Dépôts et logistique

Transferts multi-lignes, emplacement `TRANSIT`, véhicule et chauffeur, bon de transport, écarts de
réception, réapprovisionnement de chantier par règle.

### Lot 5 · Parc, flotte et conformité

Compteurs, plans de maintenance sur compteur, inspections réglementaires avec blocage d'affectation,
`FleetVehicle` et documents réglementaires bloquants, pneumatiques, sinistres, immobilisations,
carburant et cuves de chantier, taux d'utilisation, EPI et certifications.

### Lot 6 · Terrain : pointage, demandes et petit outillage

`PlantRollCall` quotidien, `MaterialRequisition` avec arbitrage des sources, dotations d'outillage,
déclarations de perte et taux de casse. Ce lot est celui qui change réellement les habitudes sur le
chantier, et il doit venir après le socle de sortie mais avant les raffinements financiers.

### Lot 7 · Location et leasing

`EquipmentLease` dans les deux sens, états des lieux, facturation récurrente branchée sur `billing`,
cautions, off-hire, pénalités, parcs comptables de coffrage et d'échafaudage, emballages consignés,
puis crédit-bail et amortissement.

### Lot 8 · Atelier et coût de revient

`WorkOrder` avec pièces et main d'oeuvre, `WorksiteCostEntry`, budgets matière, écarts de freinte,
tableaux budget contre réalisé, refacturation du coût de location interne, coût total de possession
par engin.

### Lot 9 · Mobilité et terrain avancé

Codes-barres multiples et conditionnements, endpoint de scan unique, parcours de sortie rapide,
mode hors ligne avec idempotence, preuves scellées, géorepérage, registre de clés, télématique.

### Lot 10 · Valorisation et comptabilité matière

Couches de coût et FIFO, méthode de valorisation paramétrable, périodes comptables, snapshots de
valorisation, écritures de stock, motifs d'ajustement comptables, seuils d'approbation d'écart,
frais accessoires et multi-devise.

### Lot 11 · Qualité et traçabilité

Plans de contrôle, inspections, quarantaine avec libération, non-conformités, rappels, généalogie de
lots, certificats matière et essais, profils de produits dangereux, déchets et bordereaux de suivi.

### Lot 12 · Planification et achats avancés

Prévision de consommation, stock de sécurité calculé, classification ABC et XYZ, besoins nets,
quantité économique, appels d'offres, contrats-cadres, avis d'expédition, rapprochement à trois
voies, retours et litiges fournisseur, scoring fournisseur.

### Lot 13 · Entrepôt, référentiel et restitution

Rangement dirigé, stratégies d'allocation, tâches d'entrepôt, préparations multi-lignes, unités
logistiques, comptage tournant et inventaire aveugle, nomenclatures, variantes et substituts, cycle
de vie article, recherche Elasticsearch unifiée, tableaux de bord par rôle et exports.

---

# Annexe · Scénario de bout en bout

Pour valider la cohérence du modèle, voici un parcours complet.

1. Le chantier `CH-2026-0042` (immeuble R+4, client Bokati Promotion) est créé. Le système crée
   automatiquement l'emplacement `LOC-WS-0042` de type `WORKSITE`, enfant du dépôt `LOC-DEP-BZV`.
2. Le chef de chantier saisit le métré : 420 sacs de ciment, 12 t de fer à béton, 60 m³ de sable.
   Cela alimente `WorksiteMaterialBudget`.
3. Le chef de chantier émet la demande de matériel `DM-2026-000418`. Le moteur d'arbitrage propose :
   le ciment depuis le stock du dépôt, le marteau-piqueur en transfert depuis le chantier
   `CH-2026-0039` qui l'a en `PRESENT_IDLE` depuis six jours, et le coffrage en location externe
   faute de disponibilité interne.
4. Le magasinier du dépôt de Brazzaville prépare un bon de sortie `BS-2026-000731` :
   - ligne `ITEM` : 200 sacs de ciment, consommables à la sortie ;
   - ligne `CONTAINER` : la caisse `CA-ELEC-07` (caisse électricien, 23 outils selon son modèle) ;
   - ligne `ASSET` : le marteau-piqueur `AST-0154`, retour attendu au 30 septembre ;
   - véhicule `AST-CAM-03`, chauffeur Nkodia, destinataire le chef de chantier Mabiala.
5. Le montant estimé dépasse le seuil : le bon passe en `PENDING_APPROVAL`, le responsable matériel
   l'approuve.
6. À la remise, le magasinier scanne les articles, le chef de chantier signe sur tablette, une photo
   du chargement est prise. Le système génère :
   - un `StockMovement` `OUT` de 200 sacs imputé au chantier ;
   - un `StockMovement` `TRANSFER` en cascade pour les 23 outils de la caisse, du dépôt vers
     `LOC-WS-0042`, avec un `ContainerSnapshot` de la composition ;
   - un `AssetAssignment` sur `AST-0154` au profit de Mabiala ;
   - trois `CustodyEvent` ;
   - un PDF `BS-2026-000731.pdf` archivé et signé.
7. Le camion `AST-CAM-03` ne peut pas partir : sa visite technique a expiré la veille. Le blocage
   documentaire refuse l'affectation. Le responsable matériel accorde une dérogation tracée pour ce
   seul trajet et programme le passage au contrôle.
8. Le chantier manque de coffrage. Une location entrante `LOC-IN-2026-0088` est ouverte auprès du
   loueur Sotrabat : 200 m² de coffrage, 45 jours, facturé à la semaine. Le matériel entre en stock
   en `ownershipType = SUPPLIER`, sans valorisation, mais le loyer alimente `WorksiteCostEntry`.
9. Chaque matin, le chef de chantier valide le pointage `PJ-2026-…` : la liste attendue est
   pré-remplie, il ne saisit que les écarts et les heures de la pelle `AST-ENG-11`. Ces heures
   alimentent `AssetUtilization` et le coût de location interne du chantier.
10. Le gasoil de la cuve mobile `CUV-02` est distribué à la pelle avec relevé de compteur. Le
    jaugeage hebdomadaire révèle un écart de 180 litres : alerte `FUEL_VARIANCE`.
11. Le marteau-piqueur `AST-0154` atteint 500 heures moteur, relevé saisi au chantier. Le
    `MaintenancePlan` déclenche une maintenance préventive, ouvre un `WorkOrder` à l'atelier, réserve
    le kit de pièces au dépôt et passe l'asset en `IMMOBILIZED`.
12. Au 30 septembre, `AST-0154` n'est pas revenu : alerte `MATERIAL_RETURN_OVERDUE` au chef de
    chantier et au responsable matériel.
13. Retour de la caisse `CA-ELEC-07` en fin de chantier : l'audit constate 21 outils sur 23. Deux
    lignes d'écart sont créées. Le taux de casse de l'équipe restant sous la tolérance du profil T1,
    une seule des deux pertes est imputée, en `WorksiteCostEntry` de `costType = LOSS`.
14. Les gravats et la ferraille sont évacués : `WasteDisposal` avec bordereau de suivi pour les
    inertes, `ScrapRecovery` pour la ferraille revendue, dont le produit vient en déduction du coût
    du chantier.
15. Les palettes et les bouteilles de gaz consignées sont retournées à leurs fournisseurs, soldant
    `PackagingBalance` avant la fin de la période de franchise.
16. Clôture : inventaire de fin de chantier, retour du reliquat de ciment au dépôt, calcul de l'écart
    de freinte (consommé réel contre métré), restitution du coffrage loué avec comptage contradictoire
    et état des lieux de retour, arrêt de la facturation Sotrabat, et édition du coût de revient
    matière et matériel du chantier `CH-2026-0042`.

Ce scénario mobilise l'ensemble des entités décrites et sert de base aux tests d'intégration du
module.
