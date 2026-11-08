# StockLotController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [inventory](../README.md) · 3 route(s).

## Routes

<a id="r-get-sni-api-v1-inventory-items-itemcode-lots"></a>
### GET /sni/api/v1/inventory/items/{itemCode}/lots

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<StockLotResponse>`
- Modèles : [StockLotResponse](#m-StockLotResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockLotController.java`, méthode `getLotsForItem`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |
| `locationCode` | requête | `String` |  |  |
| `activeOnly` | requête | `Boolean` |  |  |

<a id="r-post-sni-api-v1-inventory-items-itemcode-lots"></a>
### POST /sni/api/v1/inventory/items/{itemCode}/lots

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- Corps JSON (obligatoire) : `StockLotCreateRequest`
- Réponse : `StockLotResponse`
- Modèles : [StockLotResponse](#m-StockLotResponse), [StockLotCreateRequest](#m-StockLotCreateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockLotController.java`, méthode `createLot`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-inventory-stock-lots"></a>
### GET /sni/api/v1/inventory/stock/lots

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `Page<StockLotResponse>`
- Modèles : [StockLotResponse](#m-StockLotResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockLotController.java`, méthode `search`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | requête | `String` |  |  |
| `locationCode` | requête | `String` |  |  |
| `lotNumber` | requête | `String` |  |  |
| `expiringBefore` | requête | `LocalDate` |  |  |
| `active` | requête | `Boolean` |  |  |
| `remainingOnly` | requête | `Boolean` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=expiryDate,ASC |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-StockLotResponse"></a>
#### StockLotResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte |  |  |
| `itemName` | texte |  |  |
| `locationCode` | texte |  |  |
| `locationName` | texte |  |  |
| `lotNumber` | texte |  |  |
| `expiryDate` | date AAAA-MM-JJ |  |  |
| `receivedAt` | date-heure ISO 8601 UTC |  |  |
| `initialQuantity` | nombre décimal |  |  |
| `remainingQuantity` | nombre décimal |  |  |
| `quarantined` | booléen |  |  |
| `quarantineReasonType` | texte |  |  |
| `blocked` | booléen |  |  |
| `blockReasonType` | texte |  |  |
| `blockReason` | texte |  |  |
| `quarantineReason` | texte |  |  |
| `ownershipType` | enum : `COMPANY`, `CUSTOMER`, `SUPPLIER`, `CONSIGNMENT` |  |  |
| `ownerCode` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-StockLotCreateRequest"></a>
#### StockLotCreateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `locationCode` | texte | oui | longueur min `1` |
| `lotNumber` | texte | oui | longueur min `1` |
| `expiryDate` | date AAAA-MM-JJ |  |  |
| `initialQuantity` | nombre décimal | oui |  |
| `ownershipType` | enum : `COMPANY`, `CUSTOMER`, `SUPPLIER`, `CONSIGNMENT` |  |  |
| `ownerCode` | texte |  |  |
