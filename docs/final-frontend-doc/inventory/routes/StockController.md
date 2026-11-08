# StockController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [inventory](../README.md) · 11 route(s).

## Routes

<a id="r-post-sni-api-v1-inventory-stock-adjust"></a>
### POST /sni/api/v1/inventory/stock/adjust

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_STOCK_ADJUST`, voir le README racine)
- Corps JSON (obligatoire) : `StockAdjustmentRequest`
- Réponse : `StockMovementResponse`
- Modèles : [StockMovementResponse](#m-StockMovementResponse), [StockAdjustmentRequest](#m-StockAdjustmentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockController.java`, méthode `adjust`

<a id="r-post-sni-api-v1-inventory-stock-in"></a>
### POST /sni/api/v1/inventory/stock/in

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_STOCK_IN`, voir le README racine)
- Corps JSON (obligatoire) : `StockInRequest`
- Réponse : `StockMovementResponse`
- Modèles : [StockMovementResponse](#m-StockMovementResponse), [StockInRequest](#m-StockInRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockController.java`, méthode `receive`

<a id="r-get-sni-api-v1-inventory-stock-levels"></a>
### GET /sni/api/v1/inventory/stock/levels

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `Page<StockLevelResponse>`
- Modèles : [StockLevelResponse](#m-StockLevelResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockController.java`, méthode `levels`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | requête | `String` |  |  |
| `locationCode` | requête | `String` |  |  |
| `categoryCode` | requête | `String` |  |  |
| `availableOnly` | requête | `Boolean` |  |  |
| `lowStock` | requête | `Boolean` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=item.name,ASC |

<a id="r-get-sni-api-v1-inventory-stock-movements"></a>
### GET /sni/api/v1/inventory/stock/movements

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `Page<StockMovementResponse>`
- Modèles : [StockMovementResponse](#m-StockMovementResponse), [StockMovementType](#m-StockMovementType), [StockReferenceType](#m-StockReferenceType)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockController.java`, méthode `movements`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | requête | `String` |  |  |
| `locationCode` | requête | `String` |  |  |
| `movementType` | requête | `StockMovementType` |  |  |
| `referenceType` | requête | `StockReferenceType` |  |  |
| `referenceCode` | requête | `String` |  |  |
| `fromDate` | requête | `Instant` |  |  |
| `toDate` | requête | `Instant` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=performedAt,ASC |

<a id="r-post-sni-api-v1-inventory-stock-movements-movementcode-reverse"></a>
### POST /sni/api/v1/inventory/stock/movements/{movementCode}/reverse

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_STOCK_MOVEMENT_REVERSE`, voir le README racine)
- Réponse : `StockMovementResponse`
- Modèles : [StockMovementResponse](#m-StockMovementResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockController.java`, méthode `reverseMovement`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `movementCode` | chemin | `String` | oui |  |
| `performedBy` | requête | `String` |  |  |
| `reason` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-inventory-stock-out"></a>
### POST /sni/api/v1/inventory/stock/out

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_STOCK_OUT`, voir le README racine)
- Corps JSON (obligatoire) : `StockOutRequest`
- Réponse : `StockMovementResponse`
- Modèles : [StockMovementResponse](#m-StockMovementResponse), [StockOutRequest](#m-StockOutRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockController.java`, méthode `issue`

<a id="r-get-sni-api-v1-inventory-stock-reservations"></a>
### GET /sni/api/v1/inventory/stock/reservations

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `Page<StockReservationResponse>`
- Modèles : [StockReservationResponse](#m-StockReservationResponse), [StockReservationStatus](#m-StockReservationStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockController.java`, méthode `reservations`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `StockReservationStatus` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=reservedAt,ASC |

<a id="r-post-sni-api-v1-inventory-stock-reservations"></a>
### POST /sni/api/v1/inventory/stock/reservations

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_STOCK_RESERVE`, voir le README racine)
- Corps JSON (obligatoire) : `StockReservationRequest`
- Réponse : `StockReservationResponse`
- Modèles : [StockReservationResponse](#m-StockReservationResponse), [StockReservationRequest](#m-StockReservationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockController.java`, méthode `reserve`

<a id="r-patch-sni-api-v1-inventory-stock-reservations-reservationcode-consume"></a>
### PATCH /sni/api/v1/inventory/stock/reservations/{reservationCode}/consume

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_STOCK_RESERVATION_CONSUME`, voir le README racine)
- Réponse : `StockMovementResponse`
- Modèles : [StockMovementResponse](#m-StockMovementResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockController.java`, méthode `consumeReservation`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `reservationCode` | chemin | `String` | oui |  |
| `performedBy` | requête | `String` |  |  |

<a id="r-patch-sni-api-v1-inventory-stock-reservations-reservationcode-release"></a>
### PATCH /sni/api/v1/inventory/stock/reservations/{reservationCode}/release

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_STOCK_RESERVATION_RELEASE`, voir le README racine)
- Réponse : `StockReservationResponse`
- Modèles : [StockReservationResponse](#m-StockReservationResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockController.java`, méthode `releaseReservation`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `reservationCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-inventory-stock-transfer"></a>
### POST /sni/api/v1/inventory/stock/transfer

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_STOCK_TRANSFER`, voir le README racine)
- Corps JSON (obligatoire) : `StockTransferRequest`
- Réponse : `StockMovementResponse`
- Modèles : [StockMovementResponse](#m-StockMovementResponse), [StockTransferRequest](#m-StockTransferRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockController.java`, méthode `transfer`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-StockMovementResponse"></a>
#### StockMovementResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `movementCode` | texte |  |  |
| `itemCode` | texte |  |  |
| `itemName` | texte |  |  |
| `locationFromCode` | texte |  |  |
| `locationToCode` | texte |  |  |
| `movementType` | enum : `IN`, `OUT`, `TRANSFER`, `ADJUSTMENT_IN`, `ADJUSTMENT_OUT` |  |  |
| `quantity` | nombre décimal |  |  |
| `unitCost` | entier int64 ⚠ |  |  |
| `totalCost` | entier int64 ⚠ |  |  |
| `referenceType` | enum : `PURCHASE_ORDER`, `GOODS_RECEIPT`, `POS_SALE`, `BOOKING`, `CONTRACT`, `MANUAL_ADJUSTMENT`, `INVENTORY_COUNT`, `ASSET_ASSIGNMENT`, `OTHER` |  |  |
| `referenceCode` | texte |  |  |
| `reasonCode` | enum : `CONSUMPTION`, `DAMAGE`, `LOSS`, `INTERNAL_USE`, `SAMPLE`, `DONATION`, `OTHER` |  |  |
| `reason` | texte |  |  |
| `allowNegativeOverride` | booléen |  |  |
| `reversed` | booléen |  |  |
| `reversalOfMovementCode` | texte |  |  |
| `reversalMovementCode` | texte |  |  |
| `originalMovementCode` | texte |  |  |
| `reversedAt` | date-heure ISO 8601 UTC |  |  |
| `reversedBy` | texte |  |  |
| `reversalReason` | texte |  |  |
| `lots` | [StockMovementLotResponse](#m-StockMovementLotResponse)[] |  |  |
| `performedBy` | texte |  |  |
| `performedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-StockAdjustmentRequest"></a>
#### StockAdjustmentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte | oui | longueur min `1` |
| `locationCode` | texte | oui | longueur min `1` |
| `quantityDelta` | nombre décimal | oui |  |
| `unitCost` | entier int64 ⚠ |  |  |
| `lotNumber` | texte |  |  |
| `expiryDate` | date AAAA-MM-JJ |  |  |
| `reasonCode` | enum : `CONSUMPTION`, `DAMAGE`, `LOSS`, `INTERNAL_USE`, `SAMPLE`, `DONATION`, `OTHER` |  |  |
| `adjustmentReasonCode` | texte |  |  |
| `approvedBy` | texte |  |  |
| `referenceType` | enum : `PURCHASE_ORDER`, `GOODS_RECEIPT`, `POS_SALE`, `BOOKING`, `CONTRACT`, `MANUAL_ADJUSTMENT`, `INVENTORY_COUNT`, `ASSET_ASSIGNMENT`, `OTHER` |  |  |
| `referenceCode` | texte |  |  |
| `reasonDetails` | texte |  |  |
| `allowNegativeOverride` | booléen |  |  |
| `performedBy` | texte | oui | longueur min `1` |

<a id="m-StockInRequest"></a>
#### StockInRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte | oui | longueur min `1` |
| `locationCode` | texte | oui | longueur min `1` |
| `quantity` | nombre décimal | oui |  |
| `unitCost` | entier int64 ⚠ |  |  |
| `lotNumber` | texte |  |  |
| `expiryDate` | date AAAA-MM-JJ |  |  |
| `assetSerialNumbers` | texte[] |  |  |
| `assetTags` | texte[] |  |  |
| `quarantined` | booléen |  |  |
| `quarantineReason` | texte |  |  |
| `ownershipType` | enum : `COMPANY`, `CUSTOMER`, `SUPPLIER`, `CONSIGNMENT` |  |  |
| `ownerCode` | texte |  |  |
| `referenceType` | enum : `PURCHASE_ORDER`, `GOODS_RECEIPT`, `POS_SALE`, `BOOKING`, `CONTRACT`, `MANUAL_ADJUSTMENT`, `INVENTORY_COUNT`, `ASSET_ASSIGNMENT`, `OTHER` |  |  |
| `referenceCode` | texte |  |  |
| `reason` | texte |  |  |
| `performedBy` | texte |  |  |

<a id="m-StockLevelResponse"></a>
#### StockLevelResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte |  |  |
| `itemName` | texte |  |  |
| `locationCode` | texte |  |  |
| `locationName` | texte |  |  |
| `quantityOnHand` | nombre décimal |  |  |
| `quantityReserved` | nombre décimal |  |  |
| `quantityAvailable` | nombre décimal |  |  |
| `averageCost` | entier int64 ⚠ |  |  |
| `lastMovementAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-StockMovementType"></a>
#### StockMovementType

Énumération : `IN`, `OUT`, `TRANSFER`, `ADJUSTMENT_IN`, `ADJUSTMENT_OUT`

<a id="m-StockReferenceType"></a>
#### StockReferenceType

Énumération : `PURCHASE_ORDER`, `GOODS_RECEIPT`, `POS_SALE`, `BOOKING`, `CONTRACT`, `MANUAL_ADJUSTMENT`, `INVENTORY_COUNT`, `ASSET_ASSIGNMENT`, `OTHER`

<a id="m-StockOutRequest"></a>
#### StockOutRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte | oui | longueur min `1` |
| `locationCode` | texte | oui | longueur min `1` |
| `quantity` | nombre décimal | oui |  |
| `reasonCode` | enum : `CONSUMPTION`, `DAMAGE`, `LOSS`, `INTERNAL_USE`, `SAMPLE`, `DONATION`, `OTHER` | oui |  |
| `referenceType` | enum : `PURCHASE_ORDER`, `GOODS_RECEIPT`, `POS_SALE`, `BOOKING`, `CONTRACT`, `MANUAL_ADJUSTMENT`, `INVENTORY_COUNT`, `ASSET_ASSIGNMENT`, `OTHER` |  |  |
| `referenceCode` | texte |  |  |
| `reasonDetails` | texte |  |  |
| `allowNegativeOverride` | booléen |  |  |
| `performedBy` | texte | oui | longueur min `1` |
| `serialNumbers` | texte[] |  |  |
| `lotNumber` | texte |  |  |
| `confirmCreateLot` | booléen |  |  |

<a id="m-StockReservationResponse"></a>
#### StockReservationResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reservationCode` | texte |  |  |
| `itemCode` | texte |  |  |
| `itemName` | texte |  |  |
| `locationCode` | texte |  |  |
| `locationName` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `status` | enum : `ACTIVE`, `CONSUMED`, `RELEASED`, `EXPIRED`, `CANCELLED` |  |  |
| `referenceType` | enum : `PURCHASE_ORDER`, `GOODS_RECEIPT`, `POS_SALE`, `BOOKING`, `CONTRACT`, `MANUAL_ADJUSTMENT`, `INVENTORY_COUNT`, `ASSET_ASSIGNMENT`, `OTHER` |  |  |
| `referenceCode` | texte |  |  |
| `reservedBy` | texte |  |  |
| `reservedAt` | date-heure ISO 8601 UTC |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |
| `closedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-StockReservationStatus"></a>
#### StockReservationStatus

Énumération : `ACTIVE`, `CONSUMED`, `RELEASED`, `EXPIRED`, `CANCELLED`

<a id="m-StockReservationRequest"></a>
#### StockReservationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte | oui | longueur min `1` |
| `locationCode` | texte | oui | longueur min `1` |
| `quantity` | nombre décimal | oui |  |
| `referenceType` | enum : `PURCHASE_ORDER`, `GOODS_RECEIPT`, `POS_SALE`, `BOOKING`, `CONTRACT`, `MANUAL_ADJUSTMENT`, `INVENTORY_COUNT`, `ASSET_ASSIGNMENT`, `OTHER` |  |  |
| `referenceCode` | texte |  |  |
| `reservedBy` | texte |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-StockTransferRequest"></a>
#### StockTransferRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte | oui | longueur min `1` |
| `fromLocationCode` | texte | oui | longueur min `1` |
| `toLocationCode` | texte | oui | longueur min `1` |
| `quantity` | nombre décimal | oui |  |
| `reasonCode` | enum : `CONSUMPTION`, `DAMAGE`, `LOSS`, `INTERNAL_USE`, `SAMPLE`, `DONATION`, `OTHER` | oui |  |
| `referenceType` | enum : `PURCHASE_ORDER`, `GOODS_RECEIPT`, `POS_SALE`, `BOOKING`, `CONTRACT`, `MANUAL_ADJUSTMENT`, `INVENTORY_COUNT`, `ASSET_ASSIGNMENT`, `OTHER` |  |  |
| `referenceCode` | texte |  |  |
| `reasonDetails` | texte |  |  |
| `allowNegativeOverride` | booléen |  |  |
| `performedBy` | texte | oui | longueur min `1` |
| `serialNumbers` | texte[] |  |  |

<a id="m-StockMovementLotResponse"></a>
#### StockMovementLotResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `lotNumber` | texte |  |  |
| `expiryDate` | date AAAA-MM-JJ |  |  |
| `quantity` | nombre décimal |  |  |
