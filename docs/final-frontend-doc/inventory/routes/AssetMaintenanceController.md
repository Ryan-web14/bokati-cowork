# AssetMaintenanceController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [inventory](../README.md) · 5 route(s).

## Routes

<a id="r-patch-sni-api-v1-inventory-asset-maintenances-maintenancecode-cancel"></a>
### PATCH /sni/api/v1/inventory/asset-maintenances/{maintenanceCode}/cancel

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ASSET_MAINTENANCE_CANCEL`, voir le README racine)
- Réponse : `AssetMaintenanceResponse`
- Modèles : [AssetMaintenanceResponse](#m-AssetMaintenanceResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/asset/controller/AssetMaintenanceController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `maintenanceCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-asset-maintenances-maintenancecode-complete"></a>
### PATCH /sni/api/v1/inventory/asset-maintenances/{maintenanceCode}/complete

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ASSET_MAINTENANCE_COMPLETE`, voir le README racine)
- Corps JSON (obligatoire) : `AssetMaintenanceCompleteRequest`
- Réponse : `AssetMaintenanceResponse`
- Modèles : [AssetMaintenanceResponse](#m-AssetMaintenanceResponse), [AssetMaintenanceCompleteRequest](#m-AssetMaintenanceCompleteRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/asset/controller/AssetMaintenanceController.java`, méthode `complete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `maintenanceCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-asset-maintenances-maintenancecode-start"></a>
### PATCH /sni/api/v1/inventory/asset-maintenances/{maintenanceCode}/start

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ASSET_MAINTENANCE_START`, voir le README racine)
- Réponse : `AssetMaintenanceResponse`
- Modèles : [AssetMaintenanceResponse](#m-AssetMaintenanceResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/asset/controller/AssetMaintenanceController.java`, méthode `start`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `maintenanceCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-inventory-assets-assetcode-maintenances"></a>
### GET /sni/api/v1/inventory/assets/{assetCode}/maintenances

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<AssetMaintenanceResponse>`
- Modèles : [AssetMaintenanceResponse](#m-AssetMaintenanceResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/asset/controller/AssetMaintenanceController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `assetCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-inventory-assets-assetcode-maintenances"></a>
### POST /sni/api/v1/inventory/assets/{assetCode}/maintenances

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_ASSET_MAINTENANCE_PLAN`, voir le README racine)
- Corps JSON (obligatoire) : `AssetMaintenanceRequest`
- Réponse : `AssetMaintenanceResponse`
- Modèles : [AssetMaintenanceResponse](#m-AssetMaintenanceResponse), [AssetMaintenanceRequest](#m-AssetMaintenanceRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/asset/controller/AssetMaintenanceController.java`, méthode `plan`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `assetCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-AssetMaintenanceResponse"></a>
#### AssetMaintenanceResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `maintenanceCode` | texte |  |  |
| `assetCode` | texte |  |  |
| `maintenanceType` | enum : `PREVENTIVE`, `CORRECTIVE`, `WARRANTY`, `INSPECTION` |  |  |
| `status` | enum : `PLANNED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |  |  |
| `scheduledAt` | date-heure ISO 8601 UTC |  |  |
| `startedAt` | date-heure ISO 8601 UTC |  |  |
| `completedAt` | date-heure ISO 8601 UTC |  |  |
| `providerName` | texte |  |  |
| `cost` | entier int64 ⚠ |  |  |
| `description` | texte |  |  |
| `resolution` | texte |  |  |

<a id="m-AssetMaintenanceCompleteRequest"></a>
#### AssetMaintenanceCompleteRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resolution` | texte |  |  |
| `cost` | entier int64 ⚠ |  |  |
| `assetCondition` | enum : `NEW`, `GOOD`, `FAIR`, `DAMAGED`, `UNUSABLE` |  |  |

<a id="m-AssetMaintenanceRequest"></a>
#### AssetMaintenanceRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `maintenanceType` | enum : `PREVENTIVE`, `CORRECTIVE`, `WARRANTY`, `INSPECTION` | oui |  |
| `scheduledAt` | date-heure ISO 8601 UTC |  |  |
| `providerName` | texte |  |  |
| `cost` | entier int64 ⚠ |  |  |
| `description` | texte |  |  |
