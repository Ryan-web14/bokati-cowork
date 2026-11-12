# StockTransferWorkflowController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [inventory](../README.md) · 6 route(s).

## Routes

<a id="r-get-sni-api-v1-inventory-stock-transfers"></a>
### GET /sni/api/v1/inventory/stock/transfers

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `Page<StockTransferWorkflowResponse>`
- Modèles : [StockTransferWorkflowResponse](#m-StockTransferWorkflowResponse), [StockTransferWorkflowStatus](#m-StockTransferWorkflowStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockTransferWorkflowController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `StockTransferWorkflowStatus` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=requestedAt,ASC |

<a id="r-post-sni-api-v1-inventory-stock-transfers"></a>
### POST /sni/api/v1/inventory/stock/transfers

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_STOCK_TRANSFER_REQUEST`, voir le README racine)
- Corps JSON (obligatoire) : `StockTransferWorkflowRequest`
- Réponse : `StockTransferWorkflowResponse`
- Modèles : [StockTransferWorkflowResponse](#m-StockTransferWorkflowResponse), [StockTransferWorkflowRequest](#m-StockTransferWorkflowRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockTransferWorkflowController.java`, méthode `request`

<a id="r-patch-sni-api-v1-inventory-stock-transfers-transfercode-approve"></a>
### PATCH /sni/api/v1/inventory/stock/transfers/{transferCode}/approve

- Accès : back-office : rôle d'administration et droit `INVENTORY:APPROVE`
- Réponse : `StockTransferWorkflowResponse`
- Modèles : [StockTransferWorkflowResponse](#m-StockTransferWorkflowResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockTransferWorkflowController.java`, méthode `approve`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `transferCode` | chemin | `String` | oui |  |
| `approvedBy` | requête | `String` |  |  |

<a id="r-patch-sni-api-v1-inventory-stock-transfers-transfercode-cancel"></a>
### PATCH /sni/api/v1/inventory/stock/transfers/{transferCode}/cancel

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- Réponse : `StockTransferWorkflowResponse`
- Modèles : [StockTransferWorkflowResponse](#m-StockTransferWorkflowResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockTransferWorkflowController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `transferCode` | chemin | `String` | oui |  |
| `cancelledBy` | requête | `String` |  |  |
| `reason` | requête | `String` |  |  |

<a id="r-patch-sni-api-v1-inventory-stock-transfers-transfercode-receive"></a>
### PATCH /sni/api/v1/inventory/stock/transfers/{transferCode}/receive

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- Réponse : `StockTransferWorkflowResponse`
- Modèles : [StockTransferWorkflowResponse](#m-StockTransferWorkflowResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockTransferWorkflowController.java`, méthode `receive`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `transferCode` | chemin | `String` | oui |  |
| `receivedBy` | requête | `String` |  |  |

<a id="r-patch-sni-api-v1-inventory-stock-transfers-transfercode-ship"></a>
### PATCH /sni/api/v1/inventory/stock/transfers/{transferCode}/ship

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- Réponse : `StockTransferWorkflowResponse`
- Modèles : [StockTransferWorkflowResponse](#m-StockTransferWorkflowResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockTransferWorkflowController.java`, méthode `ship`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `transferCode` | chemin | `String` | oui |  |
| `shippedBy` | requête | `String` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-StockTransferWorkflowResponse"></a>
#### StockTransferWorkflowResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `transferCode` | texte |  |  |
| `itemCode` | texte |  |  |
| `itemName` | texte |  |  |
| `fromLocationCode` | texte |  |  |
| `toLocationCode` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `status` | enum : `REQUESTED`, `APPROVED`, `SHIPPED`, `RECEIVED`, `CANCELLED` |  |  |
| `requestedBy` | texte |  |  |
| `approvedBy` | texte |  |  |
| `shippedBy` | texte |  |  |
| `receivedBy` | texte |  |  |
| `movementCode` | texte |  |  |
| `reason` | texte |  |  |
| `requestedAt` | date-heure ISO 8601 UTC |  |  |
| `approvedAt` | date-heure ISO 8601 UTC |  |  |
| `shippedAt` | date-heure ISO 8601 UTC |  |  |
| `receivedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-StockTransferWorkflowStatus"></a>
#### StockTransferWorkflowStatus

Énumération : `REQUESTED`, `APPROVED`, `SHIPPED`, `RECEIVED`, `CANCELLED`

<a id="m-StockTransferWorkflowRequest"></a>
#### StockTransferWorkflowRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte | oui | longueur min `1` |
| `fromLocationCode` | texte | oui | longueur min `1` |
| `toLocationCode` | texte | oui | longueur min `1` |
| `quantity` | nombre décimal | oui |  |
| `requestedBy` | texte |  |  |
| `reason` | texte |  |  |
