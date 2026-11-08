# InventoryReferenceController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [inventory](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-inventory-reference-data"></a>
### GET /sni/api/v1/inventory/reference/data

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `Map<String, List<InventoryOptionResponse>>`
- Modèles : [InventoryOptionResponse](#m-InventoryOptionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/reference/controller/InventoryReferenceController.java`, méthode `data`

<a id="r-get-sni-api-v1-inventory-reference-enums"></a>
### GET /sni/api/v1/inventory/reference/enums

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `Map<String, List<InventoryOptionResponse>>`
- Modèles : [InventoryOptionResponse](#m-InventoryOptionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/reference/controller/InventoryReferenceController.java`, méthode `enums`

<a id="r-get-sni-api-v1-inventory-reference-enums-groups"></a>
### GET /sni/api/v1/inventory/reference/enums/groups

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<String>`
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/reference/controller/InventoryReferenceController.java`, méthode `groups`

<a id="r-get-sni-api-v1-inventory-reference-enums-group"></a>
### GET /sni/api/v1/inventory/reference/enums/{group}

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<InventoryOptionResponse>`
- Modèles : [InventoryOptionResponse](#m-InventoryOptionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/reference/controller/InventoryReferenceController.java`, méthode `enumGroup`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `group` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-InventoryOptionResponse"></a>
#### InventoryOptionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `value` | texte |  |  |
| `label` | texte |  |  |
| `description` | texte |  |  |
| `selectable` | booléen |  |  |
