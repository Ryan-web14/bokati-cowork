# Module Inventory

## Objectif

Le module `inventory` doit gerer les ressources materielles non bookables du systeme :
- les articles consommables : snacks, boissons, fournitures, consommables bureau
- les equipements individualises : ordinateurs, ecrans, routeurs, imprimantes, mobilier
- le stock par localisation : site, depot, salle, bureau, casier
- les achats et receptions
- les mouvements de stock
- les inventaires physiques et ajustements
- les alertes : stock bas, rupture, maintenance, garantie, valorisation

Ce module ne remplace pas le module `ressource`. Le module `ressource` gere les espaces ou ressources reservables, par exemple salle, bureau, poste, espace. Le module `inventory` gere ce qui est physiquement stocke, consomme, affecte, maintenu ou valorise.

## Architecture cible

Package racine propose :

```text
src/main/java/com/sni/bokaticowork/features/inventory
```

Decoupage interne :

```text
inventory/
  catalog/
  stock/
  asset/
  procurement/
  control/
  intelligence/
  integration/
  search/
```

Chaque sous-module doit suivre les conventions du projet :
- `controller`
- `dto/request`
- `dto/response`
- `mapper/interfaces`
- `mapper/decorator` si enrichissement necessaire
- `model`
- `repository/repo`
- `service/interfaces`
- `service/implementation`
- `enums`

Les endpoints doivent utiliser `ApiPath.V1`, donc la base HTTP sera :

```text
/sni/api/v1/inventory
```

## Domaines fonctionnels

### Catalog

Le catalog est le referentiel des articles.

Entites principales :
- `InventoryCategory`
- `InventoryItem`
- `InventoryUnit`
- `InventoryBarcode`
- `InventoryReorderRule`

`InventoryCategory` :
- `id`
- `code`
- `name`
- `description`
- `parentCategory`
- `active`
- audit fields standards

`InventoryItem` :
- `id`
- `itemCode`
- `name`
- `description`
- `psku`
- `shortCode`
- `displayCode`
- `identificationCode`
- `category`
- `itemType`
- `trackingType`
- `unit`
- `specification`
- `searchText`
- `defaultCost`
- `salePrice`
- `taxable`
- `active`
- `allowNegativeStock`
- `requiresExpiryDate`
- `requiresLotNumber`
- `requiresSerialNumber`
- `createdAt`
- `updatedAt`

Enums :
- `InventoryItemType`: `CONSUMABLE`, `ASSET`, `SERVICE`, `SPARE_PART`
- `InventoryTrackingType`: `NONE`, `QUANTITY`, `SERIAL`, `LOT`, `EXPIRY`
- `InventoryUnitType`: `UNIT`, `KG`, `LITER`, `BOX`, `PACK`, `METER`

Regles :
- un article `ASSET` doit produire ou etre lie a des assets individualises
- un article `CONSUMABLE` est gere par quantite
- un article avec `requiresSerialNumber = true` ne doit pas etre sorti sans serial
- un article desactive ne doit plus etre achetable ni consomme, mais reste visible dans l'historique
- `psku`, `shortCode`, `displayCode` et `identificationCode` doivent etre uniques quand ils sont renseignes
- `displayCode` est le code court affichable ou imprimable sur etiquette, desk, equipement ou article
- si `displayCode` est absent, le systeme le genere depuis `shortCode`, puis `psku`, puis `itemCode`
- `searchText` est reconstruit automatiquement depuis nom, description, codes, categorie et specification

Recherche catalog :
- rechercher par `name`
- rechercher par `itemCode`
- rechercher par `psku`
- rechercher par `shortCode`
- rechercher par `displayCode`
- rechercher par `identificationCode`
- rechercher dans `specification`
- filtrer par `categoryCode`
- filtrer par `itemType`
- filtrer par `active`

Automatisations catalog implementees en priorite :
- generation automatique de `itemCode` si absent
- normalisation des codes en majuscule
- generation de `shortCode` depuis le nom si absent
- generation de `displayCode` depuis `shortCode`, `psku` ou `itemCode`
- generation de `identificationCode` depuis `displayCode` si absent
- reconstruction de `searchText` a chaque creation ou mise a jour

### Stock

Le stock gere les quantites par emplacement.

Entites principales :
- `InventoryLocation`
- `StockLevel`
- `StockMovement`
- `StockReservation`
- `StockLot`

`InventoryLocation` :
- `id`
- `locationCode`
- `name`
- `description`
- `locationType`
- `parentLocation`
- `businessCode`
- `active`

`StockLevel` :
- `id`
- `item`
- `location`
- `lot`
- `quantityOnHand`
- `quantityReserved`
- `quantityAvailable`
- `averageCost`
- `lastMovementAt`
- `version`

`StockMovement` :
- `id`
- `movementCode`
- `item`
- `locationFrom`
- `locationTo`
- `lot`
- `movementType`
- `quantity`
- `unitCost`
- `totalCost`
- `referenceType`
- `referenceCode`
- `reason`
- `performedBy`
- `performedAt`

Enums :
- `InventoryLocationType`: `SITE`, `WAREHOUSE`, `ROOM`, `SHELF`, `LOCKER`, `VIRTUAL`
- `StockMovementType`: `IN`, `OUT`, `TRANSFER`, `ADJUSTMENT_IN`, `ADJUSTMENT_OUT`, `RESERVE`, `RELEASE`, `CONSUME`, `RETURN`
- `StockReferenceType`: `PURCHASE_ORDER`, `GOODS_RECEIPT`, `POS_SALE`, `BOOKING`, `CONTRACT`, `MANUAL_ADJUSTMENT`, `INVENTORY_COUNT`, `ASSET_ASSIGNMENT`

Regles :
- aucun stock negatif sauf si `InventoryItem.allowNegativeStock = true` ou override admin explicite
- tous les changements de stock doivent creer un `StockMovement`
- `quantityAvailable = quantityOnHand - quantityReserved`
- les mouvements concurrents doivent etre proteges par `@Version` sur `StockLevel` ou par lock pessimiste selon le cas
- les sorties FIFO/FEFO doivent etre possibles pour les lots avec expiration

### Asset

Le sous-module asset gere les biens individualises et suivis un par un.

Entites principales :
- `Asset`
- `AssetAssignment`
- `AssetMaintenance`
- `AssetWarranty`

`Asset` :
- `id`
- `assetCode`
- `item`
- `serialNumber`
- `assetTag`
- `status`
- `location`
- `assignedToType`
- `assignedToCode`
- `purchaseDate`
- `purchaseCost`
- `warrantyEndDate`
- `condition`
- `notes`

`AssetAssignment` :
- `id`
- `asset`
- `assigneeType`
- `assigneeCode`
- `startAt`
- `endAt`
- `status`
- `assignedBy`
- `returnCondition`

`AssetMaintenance` :
- `id`
- `asset`
- `maintenanceCode`
- `maintenanceType`
- `status`
- `scheduledAt`
- `startedAt`
- `completedAt`
- `providerName`
- `cost`
- `description`
- `resolution`

Enums :
- `AssetStatus`: `AVAILABLE`, `ASSIGNED`, `IN_USE`, `IN_MAINTENANCE`, `LOST`, `RETIRED`, `DAMAGED`
- `AssetCondition`: `NEW`, `GOOD`, `FAIR`, `DAMAGED`, `UNUSABLE`
- `AssetAssigneeType`: `MEMBER`, `CUSTOMER`, `BUSINESS`, `USER`, `RESOURCE`
- `AssetMaintenanceStatus`: `PLANNED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`

Regles :
- `serialNumber` doit etre unique si present
- un asset ne peut pas etre assigne deux fois en meme temps
- un asset `IN_MAINTENANCE`, `LOST`, `RETIRED` ou `DAMAGED` ne doit pas etre assignable sauf override admin
- l'historique d'affectation ne doit jamais etre supprime

### Procurement

Le procurement gere le cycle achat.

Workflow :

```text
Purchase Request -> Approval -> Purchase Order -> Goods Receipt -> Stock IN
```

Entites principales :
- `Supplier`
- `PurchaseRequest`
- `PurchaseRequestLine`
- `PurchaseOrder`
- `PurchaseOrderLine`
- `GoodsReceipt`
- `GoodsReceiptLine`

Enums :
- `PurchaseRequestStatus`: `DRAFT`, `SUBMITTED`, `APPROVED`, `REJECTED`, `CANCELLED`, `CONVERTED`
- `PurchaseOrderStatus`: `DRAFT`, `SENT`, `PARTIALLY_RECEIVED`, `RECEIVED`, `CANCELLED`, `CLOSED`
- `GoodsReceiptStatus`: `DRAFT`, `VALIDATED`, `CANCELLED`

Regles :
- une demande d'achat doit etre approuvee avant conversion en commande
- une reception validee doit creer les mouvements `StockMovementType.IN`
- une reception partielle doit mettre la commande en `PARTIALLY_RECEIVED`
- la validation d'une reception doit etre idempotente

### Control

Le control gere les inventaires physiques et ajustements.

Entites principales :
- `InventoryCount`
- `InventoryCountItem`
- `StockAdjustment`

Workflow inventaire :

```text
Create count -> Freeze expected stock -> Enter counted quantities -> Review variance -> Validate adjustment
```

Enums :
- `InventoryCountStatus`: `DRAFT`, `IN_PROGRESS`, `REVIEW`, `VALIDATED`, `CANCELLED`
- `StockAdjustmentStatus`: `DRAFT`, `APPROVED`, `APPLIED`, `CANCELLED`

Regles :
- un inventaire valide doit generer des mouvements d'ajustement
- les ecarts doivent etre justifies au-dessus d'un seuil configurable
- le stock attendu doit etre capture au moment du demarrage pour eviter les incoherences

### Intelligence

Le sous-module intelligence regroupe les calculs et alertes.

Fonctions :
- stock bas
- rupture
- quantite a recommander
- valorisation du stock
- expiration proche
- maintenance preventive
- assets non assignes
- assets en retard de retour

Entites possibles :
- `InventoryAlert`
- `InventoryValuationSnapshot`
- `InventoryForecastSnapshot`

Enums :
- `InventoryAlertType`: `LOW_STOCK`, `OUT_OF_STOCK`, `EXPIRY_SOON`, `ASSET_MAINTENANCE_DUE`, `ASSET_RETURN_OVERDUE`
- `InventoryAlertStatus`: `OPEN`, `ACKNOWLEDGED`, `RESOLVED`, `DISMISSED`

### Integration

Le module inventory doit pouvoir reagir aux autres modules sans rendre les creations bloquantes.

Integrations cible :
- POS/snack : vente cree mouvement `OUT`
- booking : consommation optionnelle de consommables ou affectation d'equipement
- contract : equipements et services inclus dans un contrat
- document : factures fournisseurs, garanties, bons de livraison
- finance : valorisation, cout d'achat, cout de consommation
- audit/outbox : publication des evenements de stock et achat

Evenements outbox proposes :
- `inventory.item.created`
- `inventory.stock.increased`
- `inventory.stock.decreased`
- `inventory.stock.transferred`
- `inventory.asset.assigned`
- `inventory.asset.returned`
- `inventory.purchase_order.created`
- `inventory.goods_receipt.validated`
- `inventory.count.validated`
- `inventory.alert.created`

## API back-office proposee

Base :

```text
/sni/api/v1/inventory
```

### Catalog

- `POST /items`
- `GET /items`
- `GET /items/{itemCode}`
- `PUT /items/{itemCode}`
- `PATCH /items/{itemCode}/activate`
- `PATCH /items/{itemCode}/deactivate`
- `DELETE /items/{itemCode}`
- `POST /categories`
- `GET /categories`
- `GET /categories/{code}`
- `PUT /categories/{code}`
- `PATCH /categories/{code}/activate`
- `PATCH /categories/{code}/deactivate`
- `GET /units`

Filtres `GET /items` :
- `q`
- `categoryCode`
- `itemType`
- `trackingType`
- `active`
- `lowStock`
- `page`
- `size`
- `sort`

### Stock

- `GET /stock-levels`
- `GET /stock-levels/{itemCode}`
- `POST /stock-movements/in`
- `POST /stock-movements/out`
- `POST /stock-movements/transfer`
- `POST /stock-movements/adjust`
- `GET /stock-movements`
- `GET /locations`
- `POST /locations`
- `GET /locations/{locationCode}`
- `PUT /locations/{locationCode}`
- `PATCH /locations/{locationCode}/activate`
- `PATCH /locations/{locationCode}/deactivate`

Filtres `GET /stock-levels` :
- `itemCode`
- `locationCode`
- `categoryCode`
- `lowStock`
- `outOfStock`
- `availableOnly`

### Assets

- `POST /assets`
- `GET /assets`
- `GET /assets/{assetCode}`
- `PUT /assets/{assetCode}`
- `PATCH /assets/{assetCode}/assign`
- `PATCH /assets/{assetCode}/return`
- `PATCH /assets/{assetCode}/retire`
- `PATCH /assets/{assetCode}/mark-lost`
- `PATCH /assets/{assetCode}/mark-damaged`
- `POST /assets/{assetCode}/maintenances`
- `GET /assets/{assetCode}/maintenances`
- `PATCH /asset-maintenances/{maintenanceCode}/start`
- `PATCH /asset-maintenances/{maintenanceCode}/complete`
- `PATCH /asset-maintenances/{maintenanceCode}/cancel`

Filtres `GET /assets` :
- `q`
- `itemCode`
- `status`
- `condition`
- `locationCode`
- `assignedToType`
- `assignedToCode`

### Procurement

- `POST /suppliers`
- `GET /suppliers`
- `GET /suppliers/{supplierCode}`
- `PUT /suppliers/{supplierCode}`
- `PATCH /suppliers/{supplierCode}/activate`
- `PATCH /suppliers/{supplierCode}/deactivate`
- `POST /purchase-requests`
- `GET /purchase-requests`
- `GET /purchase-requests/{requestCode}`
- `PUT /purchase-requests/{requestCode}`
- `PATCH /purchase-requests/{requestCode}/submit`
- `PATCH /purchase-requests/{requestCode}/approve`
- `PATCH /purchase-requests/{requestCode}/reject`
- `POST /purchase-requests/{requestCode}/convert-to-order`
- `POST /purchase-orders`
- `GET /purchase-orders`
- `GET /purchase-orders/{orderCode}`
- `PATCH /purchase-orders/{orderCode}/send`
- `PATCH /purchase-orders/{orderCode}/cancel`
- `POST /purchase-orders/{orderCode}/receipts`
- `GET /goods-receipts`
- `GET /goods-receipts/{receiptCode}`
- `PATCH /goods-receipts/{receiptCode}/validate`
- `PATCH /goods-receipts/{receiptCode}/cancel`

### Inventory Control

- `POST /counts`
- `GET /counts`
- `GET /counts/{countCode}`
- `PATCH /counts/{countCode}/start`
- `PUT /counts/{countCode}/items/{itemCode}`
- `PATCH /counts/{countCode}/review`
- `PATCH /counts/{countCode}/validate`
- `PATCH /counts/{countCode}/cancel`
- `GET /adjustments`
- `GET /adjustments/{adjustmentCode}`
- `PATCH /adjustments/{adjustmentCode}/approve`
- `PATCH /adjustments/{adjustmentCode}/apply`
- `PATCH /adjustments/{adjustmentCode}/cancel`

### Intelligence

- `GET /alerts`
- `PATCH /alerts/{alertCode}/acknowledge`
- `PATCH /alerts/{alertCode}/resolve`
- `PATCH /alerts/{alertCode}/dismiss`
- `GET /valuation`
- `POST /valuation/snapshot`
- `GET /reorder-suggestions`

## DTOs principaux

### Create item

```json
{
  "name": "Bouteille eau 50cl",
  "description": "Consommable vendu au snack",
  "psku": "SNK-EAU-50CL",
  "shortCode": "EAU50",
  "displayCode": "DESK-EAU50",
  "identificationCode": "INV-DESK-EAU50",
  "specification": "Bouteille plastique 50cl, pack de 24, stockage temperature ambiante",
  "categoryCode": "SNACK",
  "itemType": "CONSUMABLE",
  "trackingType": "QUANTITY",
  "unitCode": "UNIT",
  "defaultCost": 200,
  "salePrice": 500,
  "taxable": true,
  "allowNegativeStock": false,
  "requiresExpiryDate": true,
  "requiresLotNumber": true,
  "requiresSerialNumber": false
}
```

### Stock IN

```json
{
  "itemCode": "ITM-000001",
  "locationCode": "LOC-MAIN",
  "quantity": 100,
  "unitCost": 200,
  "referenceType": "GOODS_RECEIPT",
  "referenceCode": "GR-202604-0001",
  "lotNumber": "LOT-202604",
  "expiryDate": "2026-12-31",
  "reason": "Reception fournisseur"
}
```

### Transfer

```json
{
  "itemCode": "ITM-000001",
  "fromLocationCode": "LOC-MAIN",
  "toLocationCode": "LOC-SNACK",
  "quantity": 20,
  "reason": "Reapprovisionnement snack"
}
```

### Assign asset

```json
{
  "assigneeType": "MEMBER",
  "assigneeCode": "MEM-000001",
  "startAt": "2026-04-13T09:00:00Z",
  "expectedReturnAt": "2026-04-13T18:00:00Z",
  "notes": "Materiel remis au membre pour formation"
}
```

## Services metier attendus

Catalog :
- `InventoryItemService.create`
- `InventoryItemService.update`
- `InventoryItemService.get`
- `InventoryItemService.list`
- `InventoryItemService.activate`
- `InventoryItemService.deactivate`
- `InventoryCategoryService.create`
- `InventoryCategoryService.update`
- `InventoryCategoryService.list`

Stock :
- `StockService.receive`
- `StockService.issue`
- `StockService.transfer`
- `StockService.adjust`
- `StockService.reserve`
- `StockService.releaseReservation`
- `StockService.consumeReservation`
- `StockService.getLevel`
- `StockService.listLevels`

Asset :
- `AssetService.create`
- `AssetService.assign`
- `AssetService.returnAsset`
- `AssetService.retire`
- `AssetService.markLost`
- `AssetService.markDamaged`
- `AssetMaintenanceService.plan`
- `AssetMaintenanceService.start`
- `AssetMaintenanceService.complete`

Procurement :
- `PurchaseRequestService.create`
- `PurchaseRequestService.submit`
- `PurchaseRequestService.approve`
- `PurchaseRequestService.reject`
- `PurchaseRequestService.convertToOrder`
- `PurchaseOrderService.send`
- `GoodsReceiptService.create`
- `GoodsReceiptService.validate`

Control :
- `InventoryCountService.create`
- `InventoryCountService.start`
- `InventoryCountService.recordLine`
- `InventoryCountService.review`
- `InventoryCountService.validate`
- `StockAdjustmentService.approve`
- `StockAdjustmentService.apply`

Intelligence :
- `InventoryAlertService.detectLowStock`
- `InventoryAlertService.detectExpirySoon`
- `InventoryAlertService.detectMaintenanceDue`
- `InventoryValuationService.computeCurrentValuation`
- `InventoryValuationService.createSnapshot`

## Automatisations

Le module doit etre automatisable sans rendre l'usage back-office lourd.

Automatisations proposees :
- creer une alerte quand le stock disponible passe sous le seuil
- fermer une alerte stock bas quand le stock repasse au-dessus du seuil
- creer les assets automatiquement lors d'une reception d'article `ASSET`
- creer un mouvement stock `OUT` automatiquement quand une vente POS est confirmee
- reserver du stock quand une operation metier le demande
- consommer la reservation apres confirmation finale
- liberer la reservation si l'operation est annulee
- detecter les lots proches de l'expiration
- detecter les assets avec maintenance due
- calculer une valorisation quotidienne ou a la demande

Les automatisations doivent utiliser outbox si elles declenchent des effets hors transaction locale.

## Idempotency, audit et concurrence

Idempotency :
- obligatoire sur les operations sensibles si le header est present
- permissif si le header manque, comme le comportement attendu dans le projet
- fortement recommande pour `receive`, `issue`, `transfer`, `validate goods receipt`, `apply adjustment`, `assign asset`

Audit :
- tracer creation, modification, validation, annulation
- tracer chaque mouvement de stock
- tracer chaque override admin
- tracer les transitions achat, inventaire, asset et maintenance

Concurrence :
- proteger `StockLevel` par `@Version`
- utiliser une transaction stricte sur chaque mouvement
- recalculer `quantityAvailable` dans la meme transaction
- empecher double application d'une reception, d'un inventaire ou d'un ajustement

## Schema et migrations

Sequence Flyway proposee :

- `Vxx__create_inventory_catalog.sql`
- `Vxx__create_inventory_stock.sql`
- `Vxx__create_inventory_asset.sql`
- `Vxx__create_inventory_procurement.sql`
- `Vxx__create_inventory_control.sql`
- `Vxx__create_inventory_intelligence.sql`
- `Vxx__seed_inventory_reference_data.sql`

Tables proposees :
- `inventory_category`
- `inventory_unit`
- `inventory_item`
- `inventory_barcode`
- `inventory_reorder_rule`
- `inventory_location`
- `stock_lot`
- `stock_level`
- `stock_movement`
- `stock_reservation`
- `asset`
- `asset_assignment`
- `asset_maintenance`
- `asset_warranty`
- `supplier`
- `purchase_request`
- `purchase_request_line`
- `purchase_order`
- `purchase_order_line`
- `goods_receipt`
- `goods_receipt_line`
- `inventory_count`
- `inventory_count_item`
- `stock_adjustment`
- `inventory_alert`
- `inventory_valuation_snapshot`

Contraintes importantes :
- `inventory_item.item_code` unique
- `inventory_category.code` unique
- `inventory_location.location_code` unique
- `stock_level` unique par `item_id`, `location_id`, `lot_id`
- `asset.asset_code` unique
- `asset.serial_number` unique si non null
- `stock_movement.movement_code` unique
- `purchase_request.request_code` unique
- `purchase_order.order_code` unique
- `goods_receipt.receipt_code` unique

Indexes importants :
- `inventory_item(name)`
- `inventory_item(item_code)`
- `inventory_item(category_id)`
- `stock_level(item_id, location_id)`
- `stock_level(location_id)`
- `stock_movement(item_id, performed_at)`
- `stock_movement(reference_type, reference_code)`
- `asset(status)`
- `asset(location_id)`
- `asset(assigned_to_type, assigned_to_code)`
- `inventory_alert(status, alert_type)`

## Relation avec les modules existants

`ressource` :
- peut consommer ou utiliser des assets
- une salle peut avoir des equipements lies via `Asset.assignedToType = RESOURCE`
- ne pas dupliquer `Resource`, `ResourceType`, `ResourceAvailability`

`document` :
- rattacher facture fournisseur, bon de livraison, garantie, certificat de conformite
- possible via `ownerType = SUPPLIER`, `ASSET`, `PURCHASE_ORDER`, `GOODS_RECEIPT`

`contract` :
- rattacher des assets ou consommables inclus dans un contrat
- reference possible par `referenceType = CONTRACT`, `referenceCode = contractCode`

`client/member/customer` :
- asset assignment vers `MEMBER` ou `CUSTOMER`
- consommation stock liee a une vente ou un service client

`audit/outbox/idempotency` :
- publier les evenements metier
- consulter l'historique admin
- proteger les appels sensibles contre double clic frontend

## Ecrans frontend/back-office

Ecrans principaux :
- Dashboard inventory
- Catalog items
- Categories et units
- Stock levels
- Stock movements
- Locations
- Assets
- Asset assignment
- Asset maintenance
- Suppliers
- Purchase requests
- Purchase orders
- Goods receipts
- Inventory counts
- Adjustments
- Alerts
- Valuation

Dashboard :
- stock bas
- ruptures
- valeur totale stock
- mouvements recents
- achats en attente
- assets en maintenance
- assets assignes
- lots expirant bientot

Ecran item :
- fiche article
- stock par localisation
- mouvements
- regles de reapprovisionnement
- lots et expirations
- assets lies si `itemType = ASSET`

Ecran asset :
- fiche asset
- statut
- localisation
- affectation actuelle
- historique affectations
- maintenances
- documents attaches

Ecran stock movement :
- type de mouvement
- item
- localisation source
- localisation destination
- quantite
- reference metier
- utilisateur
- date

Ecran procurement :
- demande achat
- approbation
- commande fournisseur
- reception
- generation stock

## Priorites d'implementation

### Phase 1

Objectif : rendre le stock utilisable en back-office.

- catalog items
- categories
- units
- locations
- stock levels
- stock movements IN/OUT/TRANSFER/ADJUSTMENT
- audit basique
- recherche et pagination
- migrations catalog + stock

### Phase 2

Objectif : gerer les assets et achats.

- assets
- asset assignment
- asset maintenance
- suppliers
- purchase requests
- purchase orders
- goods receipts
- generation automatique stock IN a la reception

### Phase 3

Objectif : rendre le module robuste et intelligent.

- inventory counts
- stock adjustments avec validation
- alerts
- valuation
- outbox events
- integration document
- integration contract/resource

### Phase 4

Objectif : automatisations avancees.

- FIFO/FEFO
- forecast
- QR code / barcode scanning
- mobile inventory
- RFID plus tard
- suggestions de reapprovisionnement

## Points a valider avant implementation

- devise et cout : utiliser le module currency existant ou une devise globale business
- lien exact avec POS/snack si le module POS n'existe pas encore
- owner types autorises pour documents inventory
- strategie de numerotation : utiliser `sequence_definition` existant
- niveau de verrouillage stock : optimistic locking par defaut, pessimistic locking pour operations critiques si necessaire
- gestion multi-business : stock global ou stock par `businessCode`
