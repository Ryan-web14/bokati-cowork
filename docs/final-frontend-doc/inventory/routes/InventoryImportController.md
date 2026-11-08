# InventoryImportController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [inventory](../README.md) · 1 route(s).

## Routes

<a id="r-post-sni-api-v1-inventory-import-type"></a>
### POST /sni/api/v1/inventory/import/{type}

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- Envoi : `multipart/form-data`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_IMPORT`, voir le README racine)
- Réponse : `InventoryImportResponse`
- Modèles : [InventoryImportResponse](#m-InventoryImportResponse), [InventoryImportType](#m-InventoryImportType)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/importer/controller/InventoryImportController.java`, méthode `importFile`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `type` | chemin | `InventoryImportType` | oui |  |
| `file` | partie multipart | `MultipartFile` | oui |  |
| `dryRun` | requête | `boolean` |  | false |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-InventoryImportResponse"></a>
#### InventoryImportResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `importType` | enum : `ITEMS`, `INITIAL_STOCK`, `SUPPLIERS`, `ASSETS` |  |  |
| `fileName` | texte |  |  |
| `dryRun` | booléen |  |  |
| `totalRows` | entier |  |  |
| `successCount` | entier |  |  |
| `errorCount` | entier |  |  |
| `rows` | [InventoryImportRowResult](#m-InventoryImportRowResult)[] |  |  |

<a id="m-InventoryImportType"></a>
#### InventoryImportType

Énumération : `ITEMS`, `INITIAL_STOCK`, `SUPPLIERS`, `ASSETS`

<a id="m-InventoryImportRowResult"></a>
#### InventoryImportRowResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `rowNumber` | entier |  |  |
| `success` | booléen |  |  |
| `referenceCode` | texte |  |  |
| `message` | texte |  |  |
