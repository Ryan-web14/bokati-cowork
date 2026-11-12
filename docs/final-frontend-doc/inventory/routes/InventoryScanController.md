# InventoryScanController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [inventory](../README.md) · 1 route(s).

## Routes

<a id="r-get-sni-api-v1-inventory-scan-code"></a>
### GET /sni/api/v1/inventory/scan/{code}

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `InventoryScanResponse`
- Modèles : [InventoryScanResponse](#m-InventoryScanResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/catalog/controller/InventoryScanController.java`, méthode `resolve`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-InventoryScanResponse"></a>
#### InventoryScanResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `scannedCode` | texte |  |  |
| `resolvedType` | texte |  |  |
| `resolved` | booléen |  |  |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `itemCode` | texte |  |  |
| `itemName` | texte |  |  |
| `locationCode` | texte |  |  |
| `quantityPerScan` | nombre décimal |  |  |
| `unitCode` | texte |  |  |
| `suggestedActions` | texte[] |  |  |
| `message` | texte |  |  |
