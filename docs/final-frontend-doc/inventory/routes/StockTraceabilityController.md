# StockTraceabilityController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [inventory](../README.md) · 6 route(s).

## Routes

<a id="r-get-sni-api-v1-inventory-stock-lots-immobilised"></a>
### GET /sni/api/v1/inventory/stock/lots/immobilised

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<StockLotResponse>`
- Modèles : [StockLotResponse](#m-StockLotResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockTraceabilityController.java`, méthode `immobilised`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | requête | `String` |  |  |
| `locationCode` | requête | `String` |  |  |

<a id="r-patch-sni-api-v1-inventory-stock-lots-lotid-block"></a>
### PATCH /sni/api/v1/inventory/stock/lots/{lotId}/block

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_LOT_BLOCK`, voir le README racine)
- Corps JSON (obligatoire) : `LotBlockRequest`
- Réponse : `StockLotResponse`
- Modèles : [StockLotResponse](#m-StockLotResponse), [LotBlockRequest](#m-LotBlockRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockTraceabilityController.java`, méthode `block`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `lotId` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-inventory-stock-lots-lotid-quarantine"></a>
### PATCH /sni/api/v1/inventory/stock/lots/{lotId}/quarantine

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_LOT_QUARANTINE`, voir le README racine)
- Corps JSON (obligatoire) : `LotQuarantineRequest`
- Réponse : `StockLotResponse`
- Modèles : [StockLotResponse](#m-StockLotResponse), [LotQuarantineRequest](#m-LotQuarantineRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockTraceabilityController.java`, méthode `quarantine`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `lotId` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-inventory-stock-lots-lotid-release"></a>
### PATCH /sni/api/v1/inventory/stock/lots/{lotId}/release

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_LOT_RELEASE`, voir le README racine)
- Corps JSON (obligatoire) : `LotReleaseRequest`
- Réponse : `StockLotResponse`
- Modèles : [StockLotResponse](#m-StockLotResponse), [LotReleaseRequest](#m-LotReleaseRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockTraceabilityController.java`, méthode `release`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `lotId` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-inventory-stock-lots-lotid-unblock"></a>
### PATCH /sni/api/v1/inventory/stock/lots/{lotId}/unblock

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_LOT_UNBLOCK`, voir le README racine)
- Réponse : `StockLotResponse`
- Modèles : [StockLotResponse](#m-StockLotResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockTraceabilityController.java`, méthode `unblock`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `lotId` | chemin | `Long` | oui |  |
| `unblockedBy` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-inventory-traceability-lotnumber"></a>
### GET /sni/api/v1/inventory/traceability/{lotNumber}

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `LotTraceabilityResponse`
- Modèles : [LotTraceabilityResponse](#m-LotTraceabilityResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockTraceabilityController.java`, méthode `trace`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `lotNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-StockLotResponse"></a>
#### StockLotResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte |  |  |
| `itemName` | texte |  |  |
| `locationCode` | texte |  |  |
| `locationName` | texte |  |  |
| `lotNumber` | texte |  |  |
| `expiryDate` | date AAAA-MM-JJ |  |  |
| `receivedAt` | date-heure ISO 8601 UTC |  |  |
| `initialQuantity` | nombre décimal |  |  |
| `remainingQuantity` | nombre décimal |  |  |
| `quarantined` | booléen |  |  |
| `quarantineReasonType` | texte |  |  |
| `blocked` | booléen |  |  |
| `blockReasonType` | texte |  |  |
| `blockReason` | texte |  |  |
| `quarantineReason` | texte |  |  |
| `ownershipType` | enum : `COMPANY`, `CUSTOMER`, `SUPPLIER`, `CONSIGNMENT` |  |  |
| `ownerCode` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-LotBlockRequest"></a>
#### LotBlockRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reasonType` | enum : `PENDING_INSPECTION`, `FAILED_INSPECTION`, `SUPPLIER_DISPUTE`, `RECALL`, `EXPIRY`, `ADMINISTRATIVE` | oui |  |
| `reason` | texte | oui | longueur min `1` |
| `blockedBy` | texte |  |  |

<a id="m-LotQuarantineRequest"></a>
#### LotQuarantineRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte | oui | longueur min `1` |
| `quarantinedBy` | texte |  |  |

<a id="m-LotReleaseRequest"></a>
#### LotReleaseRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `requestedBy` | texte | oui | longueur min `1` |
| `approvedBy` | texte | oui | longueur min `1` |
| `notes` | texte |  |  |

<a id="m-LotTraceabilityResponse"></a>
#### LotTraceabilityResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `lotNumber` | texte |  |  |
| `resolved` | booléen |  |  |
| `positions` | [LotTraceabilityResponse.LotPosition](#m-LotTraceabilityResponse-LotPosition)[] |  |  |
| `upstream` | [LotTraceabilityResponse.UpstreamEvent](#m-LotTraceabilityResponse-UpstreamEvent)[] |  |  |
| `downstream` | [LotTraceabilityResponse.DownstreamEvent](#m-LotTraceabilityResponse-DownstreamEvent)[] |  |  |
| `parents` | [LotTraceabilityResponse.GenealogyLink](#m-LotTraceabilityResponse-GenealogyLink)[] |  |  |
| `children` | [LotTraceabilityResponse.GenealogyLink](#m-LotTraceabilityResponse-GenealogyLink)[] |  |  |

<a id="m-LotTraceabilityResponse-LotPosition"></a>
#### LotTraceabilityResponse.LotPosition

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `lotId` | entier int64 ⚠ |  |  |
| `itemCode` | texte |  |  |
| `itemName` | texte |  |  |
| `locationCode` | texte |  |  |
| `initialQuantity` | nombre décimal |  |  |
| `remainingQuantity` | nombre décimal |  |  |
| `expiryDate` | date AAAA-MM-JJ |  |  |
| `receivedAt` | date-heure ISO 8601 UTC |  |  |
| `quarantined` | booléen |  |  |
| `quarantineReason` | texte |  |  |
| `blocked` | booléen |  |  |
| `blockReason` | texte |  |  |
| `ownershipType` | texte |  |  |
| `ownerCode` | texte |  |  |

<a id="m-LotTraceabilityResponse-UpstreamEvent"></a>
#### LotTraceabilityResponse.UpstreamEvent

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `movementCode` | texte |  |  |
| `movementType` | texte |  |  |
| `itemCode` | texte |  |  |
| `locationCode` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `unitCost` | entier int64 ⚠ |  |  |
| `referenceType` | texte |  |  |
| `referenceCode` | texte |  |  |
| `performedBy` | texte |  |  |
| `performedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-LotTraceabilityResponse-DownstreamEvent"></a>
#### LotTraceabilityResponse.DownstreamEvent

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `movementCode` | texte |  |  |
| `movementType` | texte |  |  |
| `itemCode` | texte |  |  |
| `fromLocationCode` | texte |  |  |
| `toLocationCode` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `reasonCode` | texte |  |  |
| `referenceType` | texte |  |  |
| `referenceCode` | texte |  |  |
| `performedBy` | texte |  |  |
| `performedAt` | date-heure ISO 8601 UTC |  |  |
| `reversed` | booléen |  |  |

<a id="m-LotTraceabilityResponse-GenealogyLink"></a>
#### LotTraceabilityResponse.GenealogyLink

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `lotNumber` | texte |  |  |
| `itemCode` | texte |  |  |
| `relation` | enum : `TRANSFORMATION`, `REPACKAGING`, `MERGE`, `SPLIT`, `TRANSFER`, `CORRECTION` |  |  |
| `quantity` | nombre décimal |  |  |
| `sourceMovementCode` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
