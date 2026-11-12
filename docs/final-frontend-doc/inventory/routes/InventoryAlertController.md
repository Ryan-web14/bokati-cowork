# InventoryAlertController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [inventory](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-inventory-alerts"></a>
### GET /sni/api/v1/inventory/alerts

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `Page<InventoryAlertResponse>`
- Modèles : [InventoryAlertResponse](#m-InventoryAlertResponse), [InventoryAlertStatus](#m-InventoryAlertStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/intelligence/controller/InventoryAlertController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `InventoryAlertStatus` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,ASC |

<a id="r-patch-sni-api-v1-inventory-alerts-alertcode-acknowledge"></a>
### PATCH /sni/api/v1/inventory/alerts/{alertCode}/acknowledge

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ALERT_ACKNOWLEDGE`, voir le README racine)
- Réponse : `InventoryAlertResponse`
- Modèles : [InventoryAlertResponse](#m-InventoryAlertResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/intelligence/controller/InventoryAlertController.java`, méthode `acknowledge`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `alertCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-alerts-alertcode-dismiss"></a>
### PATCH /sni/api/v1/inventory/alerts/{alertCode}/dismiss

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ALERT_DISMISS`, voir le README racine)
- Réponse : `InventoryAlertResponse`
- Modèles : [InventoryAlertResponse](#m-InventoryAlertResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/intelligence/controller/InventoryAlertController.java`, méthode `dismiss`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `alertCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-alerts-alertcode-resolve"></a>
### PATCH /sni/api/v1/inventory/alerts/{alertCode}/resolve

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ALERT_RESOLVE`, voir le README racine)
- Réponse : `InventoryAlertResponse`
- Modèles : [InventoryAlertResponse](#m-InventoryAlertResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/intelligence/controller/InventoryAlertController.java`, méthode `resolve`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `alertCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-InventoryAlertResponse"></a>
#### InventoryAlertResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `alertCode` | texte |  |  |
| `alertType` | enum : `LOW_STOCK`, `RECURRING_LOW_STOCK`, `OUT_OF_STOCK`, `NEGATIVE_STOCK`, `OVERSTOCK`, `SLOW_MOVING`, `EXPIRY_SOON`, `EXPIRY_IMMINENT`, `WARRANTY_SOON`, `MAINTENANCE_DUE`, `ASSET_RETURN_OVERDUE`, `ASSET_RETURN_DUE_SOON`, `SUSPICIOUS_ADJUSTMENT`, `STOCK_LEVEL_DIVERGENCE` |  |  |
| `status` | enum : `OPEN`, `ACKNOWLEDGED`, `RESOLVED`, `DISMISSED` |  |  |
| `itemCode` | texte |  |  |
| `itemName` | texte |  |  |
| `locationCode` | texte |  |  |
| `locationName` | texte |  |  |
| `assetCode` | texte |  |  |
| `currentQuantity` | nombre décimal |  |  |
| `thresholdQuantity` | nombre décimal |  |  |
| `message` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `acknowledgedAt` | date-heure ISO 8601 UTC |  |  |
| `resolvedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-InventoryAlertStatus"></a>
#### InventoryAlertStatus

Énumération : `OPEN`, `ACKNOWLEDGED`, `RESOLVED`, `DISMISSED`
