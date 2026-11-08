# AssetController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [inventory](../README.md) · 13 route(s).

## Routes

<a id="r-get-sni-api-v1-inventory-assets"></a>
### GET /sni/api/v1/inventory/assets

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `Page<AssetResponse>`
- Modèles : [AssetResponse](#m-AssetResponse), [AssetStatus](#m-AssetStatus), [AssetAssigneeType](#m-AssetAssigneeType)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/asset/controller/AssetController.java`, méthode `search`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `q` | requête | `String` |  |  |
| `itemCode` | requête | `String` |  |  |
| `status` | requête | `AssetStatus` |  |  |
| `locationCode` | requête | `String` |  |  |
| `assignedToType` | requête | `AssetAssigneeType` |  |  |
| `assignedToCode` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=assetCode,ASC |

<a id="r-post-sni-api-v1-inventory-assets"></a>
### POST /sni/api/v1/inventory/assets

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ASSET_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `AssetRequest`
- Réponse : `AssetResponse`
- Modèles : [AssetResponse](#m-AssetResponse), [AssetRequest](#m-AssetRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/asset/controller/AssetController.java`, méthode `create`

<a id="r-get-sni-api-v1-inventory-assets-assetcode"></a>
### GET /sni/api/v1/inventory/assets/{assetCode}

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `AssetResponse`
- Modèles : [AssetResponse](#m-AssetResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/asset/controller/AssetController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `assetCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-inventory-assets-assetcode"></a>
### PUT /sni/api/v1/inventory/assets/{assetCode}

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ASSET_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `AssetRequest`
- Réponse : `AssetResponse`
- Modèles : [AssetResponse](#m-AssetResponse), [AssetRequest](#m-AssetRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/asset/controller/AssetController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `assetCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-assets-assetcode-assign"></a>
### PATCH /sni/api/v1/inventory/assets/{assetCode}/assign

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ASSET_ASSIGN`, voir le README racine)
- Corps JSON (obligatoire) : `AssetAssignRequest`
- Réponse : `AssetAssignmentResponse`
- Modèles : [AssetAssignmentResponse](#m-AssetAssignmentResponse), [AssetAssignRequest](#m-AssetAssignRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/asset/controller/AssetController.java`, méthode `assign`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `assetCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-inventory-assets-assetcode-assignments-assignmentid-loan-sheet-pdf"></a>
### GET /sni/api/v1/inventory/assets/{assetCode}/assignments/{assignmentId}/loan-sheet.pdf

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/asset/controller/AssetController.java`, méthode `loanSheetPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `assetCode` | chemin | `String` | oui |  |
| `assignmentId` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-inventory-assets-assetcode-cancel-reservation"></a>
### PATCH /sni/api/v1/inventory/assets/{assetCode}/cancel-reservation

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ASSET_CANCEL_RESERVATION`, voir le README racine)
- Réponse : `AssetAssignmentResponse`
- Modèles : [AssetAssignmentResponse](#m-AssetAssignmentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/asset/controller/AssetController.java`, méthode `cancelReservation`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `assetCode` | chemin | `String` | oui |  |
| `cancelledBy` | requête | `String` |  |  |
| `reason` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-inventory-assets-assetcode-location-history"></a>
### GET /sni/api/v1/inventory/assets/{assetCode}/location-history

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<AssetLocationHistoryResponse>`
- Modèles : [AssetLocationHistoryResponse](#m-AssetLocationHistoryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/asset/controller/AssetController.java`, méthode `locationHistory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `assetCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-assets-assetcode-mark-damaged"></a>
### PATCH /sni/api/v1/inventory/assets/{assetCode}/mark-damaged

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ASSET_MARK_DAMAGED`, voir le README racine)
- Réponse : `AssetResponse`
- Modèles : [AssetResponse](#m-AssetResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/asset/controller/AssetController.java`, méthode `markDamaged`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `assetCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-assets-assetcode-mark-lost"></a>
### PATCH /sni/api/v1/inventory/assets/{assetCode}/mark-lost

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ASSET_MARK_LOST`, voir le README racine)
- Réponse : `AssetResponse`
- Modèles : [AssetResponse](#m-AssetResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/asset/controller/AssetController.java`, méthode `markLost`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `assetCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-assets-assetcode-reserve"></a>
### PATCH /sni/api/v1/inventory/assets/{assetCode}/reserve

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ASSET_RESERVE`, voir le README racine)
- Corps JSON (obligatoire) : `AssetReserveRequest`
- Réponse : `AssetAssignmentResponse`
- Modèles : [AssetAssignmentResponse](#m-AssetAssignmentResponse), [AssetReserveRequest](#m-AssetReserveRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/asset/controller/AssetController.java`, méthode `reserve`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `assetCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-assets-assetcode-retire"></a>
### PATCH /sni/api/v1/inventory/assets/{assetCode}/retire

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ASSET_RETIRE`, voir le README racine)
- Réponse : `AssetResponse`
- Modèles : [AssetResponse](#m-AssetResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/asset/controller/AssetController.java`, méthode `retire`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `assetCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-assets-assetcode-return"></a>
### PATCH /sni/api/v1/inventory/assets/{assetCode}/return

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ASSET_RETURN`, voir le README racine)
- Corps JSON (obligatoire) : `AssetReturnRequest`
- Réponse : `AssetAssignmentResponse`
- Modèles : [AssetAssignmentResponse](#m-AssetAssignmentResponse), [AssetReturnRequest](#m-AssetReturnRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/asset/controller/AssetController.java`, méthode `returnAsset`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `assetCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-AssetResponse"></a>
#### AssetResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `assetCode` | texte |  |  |
| `itemCode` | texte |  |  |
| `itemName` | texte |  |  |
| `serialNumber` | texte |  |  |
| `assetTag` | texte |  |  |
| `status` | enum : `AVAILABLE`, `RESERVED`, `ASSIGNED`, `IN_USE`, `IN_MAINTENANCE`, `LOST`, `RETIRED`, `DAMAGED` |  |  |
| `condition` | enum : `NEW`, `GOOD`, `FAIR`, `DAMAGED`, `UNUSABLE` |  |  |
| `locationCode` | texte |  |  |
| `locationName` | texte |  |  |
| `assignedToType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS`, `USER`, `RESOURCE` |  |  |
| `assignedToCode` | texte |  |  |
| `purchaseDate` | date AAAA-MM-JJ |  |  |
| `purchaseCost` | entier int64 ⚠ |  |  |
| `warrantyEndDate` | date AAAA-MM-JJ |  |  |
| `usefulLifeMonths` | entier |  |  |
| `residualValue` | entier int64 ⚠ |  |  |
| `depreciatedValue` | entier int64 ⚠ |  |  |
| `notes` | texte |  |  |

<a id="m-AssetStatus"></a>
#### AssetStatus

Énumération : `AVAILABLE`, `RESERVED`, `ASSIGNED`, `IN_USE`, `IN_MAINTENANCE`, `LOST`, `RETIRED`, `DAMAGED`

<a id="m-AssetAssigneeType"></a>
#### AssetAssigneeType

Énumération : `MEMBER`, `CUSTOMER`, `BUSINESS`, `USER`, `RESOURCE`

<a id="m-AssetRequest"></a>
#### AssetRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `assetCode` | texte |  |  |
| `itemCode` | texte | oui | longueur min `1` |
| `serialNumber` | texte |  |  |
| `assetTag` | texte |  |  |
| `status` | enum : `AVAILABLE`, `RESERVED`, `ASSIGNED`, `IN_USE`, `IN_MAINTENANCE`, `LOST`, `RETIRED`, `DAMAGED` |  |  |
| `condition` | enum : `NEW`, `GOOD`, `FAIR`, `DAMAGED`, `UNUSABLE` |  |  |
| `locationCode` | texte |  |  |
| `purchaseDate` | date AAAA-MM-JJ |  |  |
| `purchaseCost` | entier int64 ⚠ |  |  |
| `warrantyEndDate` | date AAAA-MM-JJ |  |  |
| `usefulLifeMonths` | entier |  |  |
| `residualValue` | entier int64 ⚠ |  |  |
| `notes` | texte |  |  |

<a id="m-AssetAssignmentResponse"></a>
#### AssetAssignmentResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `assetCode` | texte |  |  |
| `assigneeType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS`, `USER`, `RESOURCE` |  |  |
| `assigneeCode` | texte |  |  |
| `status` | enum : `RESERVED`, `ACTIVE`, `RETURNED`, `CANCELLED` |  |  |
| `startAt` | date-heure ISO 8601 UTC |  |  |
| `expectedReturnAt` | date-heure ISO 8601 UTC |  |  |
| `endAt` | date-heure ISO 8601 UTC |  |  |
| `assignedBy` | texte |  |  |
| `purpose` | texte |  |  |
| `returnedBy` | texte |  |  |
| `checkoutCondition` | enum : `NEW`, `GOOD`, `FAIR`, `DAMAGED`, `UNUSABLE` |  |  |
| `returnCondition` | enum : `NEW`, `GOOD`, `FAIR`, `DAMAGED`, `UNUSABLE` |  |  |
| `checkoutPhotoUrl` | texte |  |  |
| `returnPhotoUrl` | texte |  |  |
| `receiverSignatureUrl` | texte |  |  |
| `notes` | texte |  |  |

<a id="m-AssetAssignRequest"></a>
#### AssetAssignRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `assigneeType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS`, `USER`, `RESOURCE` | oui |  |
| `assigneeCode` | texte | oui | longueur min `1` |
| `startAt` | date-heure ISO 8601 UTC |  |  |
| `expectedReturnAt` | date-heure ISO 8601 UTC | oui |  |
| `assignedBy` | texte | oui | longueur min `1` |
| `purpose` | texte | oui | longueur min `1` |
| `checkoutPhotoUrl` | texte |  |  |
| `receiverSignatureUrl` | texte |  |  |
| `notes` | texte |  |  |

<a id="m-AssetLocationHistoryResponse"></a>
#### AssetLocationHistoryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `assetCode` | texte |  |  |
| `fromLocationCode` | texte |  |  |
| `toLocationCode` | texte |  |  |
| `changedBy` | texte |  |  |
| `reason` | texte |  |  |
| `changedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-AssetReserveRequest"></a>
#### AssetReserveRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `assigneeType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS`, `USER`, `RESOURCE` | oui |  |
| `assigneeCode` | texte | oui | longueur min `1` |
| `reservedFrom` | date-heure ISO 8601 UTC |  |  |
| `expectedReturnAt` | date-heure ISO 8601 UTC |  |  |
| `reservedBy` | texte |  |  |
| `notes` | texte |  |  |

<a id="m-AssetReturnRequest"></a>
#### AssetReturnRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `returnCondition` | enum : `NEW`, `GOOD`, `FAIR`, `DAMAGED`, `UNUSABLE` |  |  |
| `returnedBy` | texte |  |  |
| `returnPhotoUrl` | texte |  |  |
| `notes` | texte |  |  |
