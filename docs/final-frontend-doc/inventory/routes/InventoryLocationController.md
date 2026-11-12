# InventoryLocationController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [inventory](../README.md) · 6 route(s).

## Routes

<a id="r-get-sni-api-v1-inventory-locations"></a>
### GET /sni/api/v1/inventory/locations

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<InventoryLocationResponse>`
- Modèles : [InventoryLocationResponse](#m-InventoryLocationResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryLocationController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `active` | requête | `Boolean` |  |  |

<a id="r-post-sni-api-v1-inventory-locations"></a>
### POST /sni/api/v1/inventory/locations

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_LOCATION_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `InventoryLocationRequest`
- Réponse : `InventoryLocationResponse`
- Modèles : [InventoryLocationResponse](#m-InventoryLocationResponse), [InventoryLocationRequest](#m-InventoryLocationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryLocationController.java`, méthode `create`

<a id="r-get-sni-api-v1-inventory-locations-locationcode"></a>
### GET /sni/api/v1/inventory/locations/{locationCode}

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `InventoryLocationResponse`
- Modèles : [InventoryLocationResponse](#m-InventoryLocationResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryLocationController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `locationCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-inventory-locations-locationcode"></a>
### PUT /sni/api/v1/inventory/locations/{locationCode}

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_LOCATION_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `InventoryLocationRequest`
- Réponse : `InventoryLocationResponse`
- Modèles : [InventoryLocationResponse](#m-InventoryLocationResponse), [InventoryLocationRequest](#m-InventoryLocationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryLocationController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `locationCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-locations-locationcode-activate"></a>
### PATCH /sni/api/v1/inventory/locations/{locationCode}/activate

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_LOCATION_ACTIVATE`, voir le README racine)
- Réponse : `InventoryLocationResponse`
- Modèles : [InventoryLocationResponse](#m-InventoryLocationResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryLocationController.java`, méthode `activate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `locationCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-locations-locationcode-deactivate"></a>
### PATCH /sni/api/v1/inventory/locations/{locationCode}/deactivate

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_LOCATION_DEACTIVATE`, voir le README racine)
- Réponse : `InventoryLocationResponse`
- Modèles : [InventoryLocationResponse](#m-InventoryLocationResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryLocationController.java`, méthode `deactivate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `locationCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-InventoryLocationResponse"></a>
#### InventoryLocationResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `locationCode` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `locationType` | enum : `SITE`, `WAREHOUSE`, `ROOM`, `SHELF`, `LOCKER`, `VIRTUAL` |  |  |
| `parentLocationCode` | texte |  |  |
| `businessCode` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-InventoryLocationRequest"></a>
#### InventoryLocationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `locationType` | enum : `SITE`, `WAREHOUSE`, `ROOM`, `SHELF`, `LOCKER`, `VIRTUAL` | oui |  |
| `parentLocationCode` | texte |  |  |
| `businessCode` | texte |  |  |
| `active` | booléen |  |  |
