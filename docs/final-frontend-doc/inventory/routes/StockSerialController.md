# StockSerialController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [inventory](../README.md) · 1 route(s).

## Routes

<a id="r-get-sni-api-v1-inventory-items-itemcode-serials"></a>
### GET /sni/api/v1/inventory/items/{itemCode}/serials

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<InventorySerialResponse>`
- Modèles : [InventorySerialResponse](#m-InventorySerialResponse), [InventorySerialStatus](#m-InventorySerialStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockSerialController.java`, méthode `serials`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |
| `locationCode` | requête | `String` |  |  |
| `status` | requête | `InventorySerialStatus` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-InventorySerialResponse"></a>
#### InventorySerialResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte |  |  |
| `locationCode` | texte |  |  |
| `serialNumber` | texte |  |  |
| `status` | enum : `AVAILABLE`, `ISSUED`, `TRANSFERRED`, `LOST`, `DAMAGED` |  |  |
| `lotNumber` | texte |  |  |
| `receivedAt` | date-heure ISO 8601 UTC |  |  |
| `issuedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-InventorySerialStatus"></a>
#### InventorySerialStatus

Énumération : `AVAILABLE`, `ISSUED`, `TRANSFERRED`, `LOST`, `DAMAGED`
