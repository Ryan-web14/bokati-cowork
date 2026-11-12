# InventoryCountController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [inventory](../README.md) · 8 route(s).

## Routes

<a id="r-get-sni-api-v1-inventory-counts"></a>
### GET /sni/api/v1/inventory/counts

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `Page<InventoryCountResponse>`
- Modèles : [InventoryCountResponse](#m-InventoryCountResponse), [InventoryCountStatus](#m-InventoryCountStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/control/controller/InventoryCountController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `InventoryCountStatus` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,ASC |

<a id="r-post-sni-api-v1-inventory-counts"></a>
### POST /sni/api/v1/inventory/counts

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_COUNT_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `InventoryCountCreateRequest`
- Réponse : `InventoryCountResponse`
- Modèles : [InventoryCountResponse](#m-InventoryCountResponse), [InventoryCountCreateRequest](#m-InventoryCountCreateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/control/controller/InventoryCountController.java`, méthode `create`

<a id="r-get-sni-api-v1-inventory-counts-countcode"></a>
### GET /sni/api/v1/inventory/counts/{countCode}

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `InventoryCountResponse`
- Modèles : [InventoryCountResponse](#m-InventoryCountResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/control/controller/InventoryCountController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `countCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-counts-countcode-cancel"></a>
### PATCH /sni/api/v1/inventory/counts/{countCode}/cancel

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- Réponse : `InventoryCountResponse`
- Modèles : [InventoryCountResponse](#m-InventoryCountResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/control/controller/InventoryCountController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `countCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-inventory-counts-countcode-lines"></a>
### PUT /sni/api/v1/inventory/counts/{countCode}/lines

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_COUNT_LINE_RECORD`, voir le README racine)
- Corps JSON (obligatoire) : `InventoryCountLineRequest`
- Réponse : `InventoryCountResponse`
- Modèles : [InventoryCountResponse](#m-InventoryCountResponse), [InventoryCountLineRequest](#m-InventoryCountLineRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/control/controller/InventoryCountController.java`, méthode `recordLine`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `countCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-counts-countcode-review"></a>
### PATCH /sni/api/v1/inventory/counts/{countCode}/review

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- Réponse : `InventoryCountResponse`
- Modèles : [InventoryCountResponse](#m-InventoryCountResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/control/controller/InventoryCountController.java`, méthode `review`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `countCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-counts-countcode-start"></a>
### PATCH /sni/api/v1/inventory/counts/{countCode}/start

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- Réponse : `InventoryCountResponse`
- Modèles : [InventoryCountResponse](#m-InventoryCountResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/control/controller/InventoryCountController.java`, méthode `start`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `countCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-counts-countcode-validate"></a>
### PATCH /sni/api/v1/inventory/counts/{countCode}/validate

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_COUNT_VALIDATE`, voir le README racine)
- Réponse : `InventoryCountResponse`
- Modèles : [InventoryCountResponse](#m-InventoryCountResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/control/controller/InventoryCountController.java`, méthode `validate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `countCode` | chemin | `String` | oui |  |
| `performedBy` | requête | `String` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-InventoryCountResponse"></a>
#### InventoryCountResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `countCode` | texte |  |  |
| `locationCode` | texte |  |  |
| `locationName` | texte |  |  |
| `status` | enum : `DRAFT`, `IN_PROGRESS`, `REVIEW`, `VALIDATED`, `CANCELLED` |  |  |
| `createdBy` | texte |  |  |
| `notes` | texte |  |  |
| `startedAt` | date-heure ISO 8601 UTC |  |  |
| `reviewedAt` | date-heure ISO 8601 UTC |  |  |
| `validatedAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `items` | [InventoryCountItemResponse](#m-InventoryCountItemResponse)[] |  |  |

<a id="m-InventoryCountStatus"></a>
#### InventoryCountStatus

Énumération : `DRAFT`, `IN_PROGRESS`, `REVIEW`, `VALIDATED`, `CANCELLED`

<a id="m-InventoryCountCreateRequest"></a>
#### InventoryCountCreateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `locationCode` | texte | oui | longueur min `1` |
| `createdBy` | texte |  |  |
| `notes` | texte |  |  |

<a id="m-InventoryCountLineRequest"></a>
#### InventoryCountLineRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte | oui | longueur min `1` |
| `countedQuantity` | nombre décimal | oui |  |
| `notes` | texte |  |  |

<a id="m-InventoryCountItemResponse"></a>
#### InventoryCountItemResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte |  |  |
| `itemName` | texte |  |  |
| `expectedQuantity` | nombre décimal |  |  |
| `countedQuantity` | nombre décimal |  |  |
| `varianceQuantity` | nombre décimal |  |  |
| `notes` | texte |  |  |
