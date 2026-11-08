# InventoryUnitController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [inventory](../README.md) · 6 route(s).

## Routes

<a id="r-get-sni-api-v1-inventory-units"></a>
### GET /sni/api/v1/inventory/units

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<InventoryUnitResponse>`
- Modèles : [InventoryUnitResponse](#m-InventoryUnitResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryUnitController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `active` | requête | `Boolean` |  |  |

<a id="r-post-sni-api-v1-inventory-units"></a>
### POST /sni/api/v1/inventory/units

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_UNIT_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `InventoryUnitRequest`
- Réponse : `InventoryUnitResponse`
- Modèles : [InventoryUnitResponse](#m-InventoryUnitResponse), [InventoryUnitRequest](#m-InventoryUnitRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryUnitController.java`, méthode `create`

<a id="r-get-sni-api-v1-inventory-units-code"></a>
### GET /sni/api/v1/inventory/units/{code}

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `InventoryUnitResponse`
- Modèles : [InventoryUnitResponse](#m-InventoryUnitResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryUnitController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-inventory-units-code"></a>
### PUT /sni/api/v1/inventory/units/{code}

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_UNIT_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `InventoryUnitRequest`
- Réponse : `InventoryUnitResponse`
- Modèles : [InventoryUnitResponse](#m-InventoryUnitResponse), [InventoryUnitRequest](#m-InventoryUnitRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryUnitController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-units-code-activate"></a>
### PATCH /sni/api/v1/inventory/units/{code}/activate

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_UNIT_ACTIVATE`, voir le README racine)
- Réponse : `InventoryUnitResponse`
- Modèles : [InventoryUnitResponse](#m-InventoryUnitResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryUnitController.java`, méthode `activate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-units-code-deactivate"></a>
### PATCH /sni/api/v1/inventory/units/{code}/deactivate

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_UNIT_DEACTIVATE`, voir le README racine)
- Réponse : `InventoryUnitResponse`
- Modèles : [InventoryUnitResponse](#m-InventoryUnitResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryUnitController.java`, méthode `deactivate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-InventoryUnitResponse"></a>
#### InventoryUnitResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `unitType` | enum : `UNIT`, `KG`, `LITER`, `BOX`, `PACK`, `METER` |  |  |
| `active` | booléen |  |  |

<a id="m-InventoryUnitRequest"></a>
#### InventoryUnitRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte | oui | longueur min `1` |
| `unitType` | enum : `UNIT`, `KG`, `LITER`, `BOX`, `PACK`, `METER` | oui |  |
| `active` | booléen |  |  |
