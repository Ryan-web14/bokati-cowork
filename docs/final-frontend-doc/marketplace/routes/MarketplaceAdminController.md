# MarketplaceAdminController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [marketplace](../README.md) · 10 route(s).

## Routes

<a id="r-get-sni-api-v1-marketplace-offers"></a>
### GET /sni/api/v1/marketplace/offers

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER') or hasRole('OPERATIONS_AGENT')`
- Réponse : `List<MarketplaceDtos.OfferView>`
- Modèles : [MarketplaceDtos.OfferView](#m-MarketplaceDtos-OfferView)
- Code : `src/main/java/com/sni/bokaticowork/features/marketplace/controller/MarketplaceAdminController.java`, méthode `offers`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `published` | requête | `Boolean` |  |  |
| `category` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-marketplace-offers"></a>
### POST /sni/api/v1/marketplace/offers

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Corps JSON (obligatoire) : `MarketplaceDtos.OfferRequest`
- Réponse : `MarketplaceDtos.OfferView`
- Modèles : [MarketplaceDtos.OfferView](#m-MarketplaceDtos-OfferView), [MarketplaceDtos.OfferRequest](#m-MarketplaceDtos-OfferRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/marketplace/controller/MarketplaceAdminController.java`, méthode `create`

<a id="r-get-sni-api-v1-marketplace-offers-id"></a>
### GET /sni/api/v1/marketplace/offers/{id}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER') or hasRole('OPERATIONS_AGENT')`
- Réponse : `MarketplaceDtos.OfferView`
- Modèles : [MarketplaceDtos.OfferView](#m-MarketplaceDtos-OfferView)
- Code : `src/main/java/com/sni/bokaticowork/features/marketplace/controller/MarketplaceAdminController.java`, méthode `offer`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-marketplace-offers-id"></a>
### PATCH /sni/api/v1/marketplace/offers/{id}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Corps JSON (obligatoire) : `MarketplaceDtos.OfferUpdateRequest`
- Réponse : `MarketplaceDtos.OfferView`
- Modèles : [MarketplaceDtos.OfferView](#m-MarketplaceDtos-OfferView), [MarketplaceDtos.OfferUpdateRequest](#m-MarketplaceDtos-OfferUpdateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/marketplace/controller/MarketplaceAdminController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-marketplace-offers-id-publish"></a>
### POST /sni/api/v1/marketplace/offers/{id}/publish

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `MarketplaceDtos.OfferView`
- Modèles : [MarketplaceDtos.OfferView](#m-MarketplaceDtos-OfferView)
- Code : `src/main/java/com/sni/bokaticowork/features/marketplace/controller/MarketplaceAdminController.java`, méthode `publish`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-marketplace-offers-id-unpublish"></a>
### POST /sni/api/v1/marketplace/offers/{id}/unpublish

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `MarketplaceDtos.OfferView`
- Modèles : [MarketplaceDtos.OfferView](#m-MarketplaceDtos-OfferView)
- Code : `src/main/java/com/sni/bokaticowork/features/marketplace/controller/MarketplaceAdminController.java`, méthode `unpublish`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-marketplace-orders"></a>
### GET /sni/api/v1/marketplace/orders

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER') or hasRole('OPERATIONS_AGENT')`
- Réponse : `List<MarketplaceDtos.OrderView>`
- Modèles : [MarketplaceDtos.OrderView](#m-MarketplaceDtos-OrderView), [MarketplaceOrder.Status](#m-MarketplaceOrder-Status)
- Code : `src/main/java/com/sni/bokaticowork/features/marketplace/controller/MarketplaceAdminController.java`, méthode `orders`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `MarketplaceOrder.Status` |  |  |
| `stockPending` | requête | `boolean` |  | false |
| `limit` | requête | `int` |  | 100 |

<a id="r-get-sni-api-v1-marketplace-orders-ordernumber"></a>
### GET /sni/api/v1/marketplace/orders/{orderNumber}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER') or hasRole('OPERATIONS_AGENT')`
- Réponse : `MarketplaceDtos.OrderView`
- Modèles : [MarketplaceDtos.OrderView](#m-MarketplaceDtos-OrderView)
- Code : `src/main/java/com/sni/bokaticowork/features/marketplace/controller/MarketplaceAdminController.java`, méthode `order`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `orderNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-marketplace-orders-ordernumber-cancel"></a>
### POST /sni/api/v1/marketplace/orders/{orderNumber}/cancel

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Corps JSON (obligatoire) : `MarketplaceDtos.CancelRequest`
- Réponse : `MarketplaceDtos.OrderView`
- Modèles : [MarketplaceDtos.OrderView](#m-MarketplaceDtos-OrderView), [MarketplaceDtos.CancelRequest](#m-MarketplaceDtos-CancelRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/marketplace/controller/MarketplaceAdminController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `orderNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-marketplace-orders-ordernumber-fulfil"></a>
### POST /sni/api/v1/marketplace/orders/{orderNumber}/fulfil

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER') or hasRole('OPERATIONS_AGENT')`
- Corps JSON (facultatif) : `MarketplaceDtos.FulfilRequest`
- Réponse : `MarketplaceDtos.OrderView`
- Modèles : [MarketplaceDtos.OrderView](#m-MarketplaceDtos-OrderView), [MarketplaceDtos.FulfilRequest](#m-MarketplaceDtos-FulfilRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/marketplace/controller/MarketplaceAdminController.java`, méthode `fulfil`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `orderNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-MarketplaceDtos-OfferView"></a>
#### MarketplaceDtos.OfferView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `offerCode` | texte |  |  |
| `kind` | enum : `PRODUCT`, `SERVICE` |  |  |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `imageUrl` | texte |  |  |
| `category` | texte |  |  |
| `price` | nombre décimal |  |  |
| `ownPrice` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `available` | nombre décimal |  |  |
| `maxPerOrder` | entier |  |  |
| `published` | booléen |  |  |
| `displayOrder` | entier |  |  |
| `inventoryItemCode` | texte |  |  |
| `locationCode` | texte |  |  |
| `serviceItemCode` | texte |  |  |

<a id="m-MarketplaceDtos-OfferRequest"></a>
#### MarketplaceDtos.OfferRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `kind` | enum : `PRODUCT`, `SERVICE` | oui |  |
| `inventoryItemCode` | texte |  |  |
| `locationCode` | texte |  |  |
| `serviceItemCode` | texte |  |  |
| `title` | texte |  | longueur min `0`, longueur max `200` |
| `description` | texte |  |  |
| `imageUrl` | texte |  | longueur min `0`, longueur max `500` |
| `category` | texte |  | longueur min `0`, longueur max `120` |
| `price` | nombre décimal |  |  |
| `currency` | texte |  | longueur min `3`, longueur max `3` |
| `maxPerOrder` | entier |  |  |
| `displayOrder` | entier |  |  |

<a id="m-MarketplaceDtos-OfferUpdateRequest"></a>
#### MarketplaceDtos.OfferUpdateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `title` | texte |  | longueur min `0`, longueur max `200` |
| `description` | texte |  |  |
| `imageUrl` | texte |  | longueur min `0`, longueur max `500` |
| `category` | texte |  | longueur min `0`, longueur max `120` |
| `price` | nombre décimal |  |  |
| `resetPrice` | booléen |  |  |
| `maxPerOrder` | entier |  |  |
| `clearMaxPerOrder` | booléen |  |  |
| `displayOrder` | entier |  |  |

<a id="m-MarketplaceDtos-OrderView"></a>
#### MarketplaceDtos.OrderView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `orderNumber` | texte |  |  |
| `status` | enum : `CART`, `PENDING_PAYMENT`, `PAID`, `FULFILLED`, `CANCELLED`, `EXPIRED` |  |  |
| `buyerMemberCode` | texte |  |  |
| `billingCompanyCode` | texte |  |  |
| `currency` | texte |  |  |
| `totalAmount` | nombre décimal |  |  |
| `invoiceNumber` | texte |  |  |
| `invoiceBalanceDue` | nombre décimal |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |
| `placedAt` | date-heure ISO 8601 UTC |  |  |
| `paidAt` | date-heure ISO 8601 UTC |  |  |
| `fulfilledAt` | date-heure ISO 8601 UTC |  |  |
| `fulfilledBy` | texte |  |  |
| `collectedBy` | texte |  |  |
| `cancelledAt` | date-heure ISO 8601 UTC |  |  |
| `cancelledBy` | texte |  |  |
| `cancellationReason` | texte |  |  |
| `creditNoteNumber` | texte |  |  |
| `stockPending` | booléen |  |  |
| `lines` | [MarketplaceDtos.LineView](#m-MarketplaceDtos-LineView)[] |  |  |

<a id="m-MarketplaceOrder-Status"></a>
#### MarketplaceOrder.Status

Énumération : `CART`, `PENDING_PAYMENT`, `PAID`, `FULFILLED`, `CANCELLED`, `EXPIRED`

<a id="m-MarketplaceDtos-CancelRequest"></a>
#### MarketplaceDtos.CancelRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte | oui | longueur min `0`, longueur max `500` |
| `returnToStock` | booléen |  |  |

<a id="m-MarketplaceDtos-FulfilRequest"></a>
#### MarketplaceDtos.FulfilRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `collectedBy` | texte |  | longueur min `0`, longueur max `200` |

<a id="m-MarketplaceDtos-LineView"></a>
#### MarketplaceDtos.LineView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `offerId` | entier int64 ⚠ |  |  |
| `kind` | enum : `PRODUCT`, `SERVICE` |  |  |
| `title` | texte |  |  |
| `imageUrl` | texte |  |  |
| `quantity` | entier |  |  |
| `unitPrice` | nombre décimal |  |  |
| `amount` | nombre décimal |  |  |
| `available` | nombre décimal |  |  |
| `stockError` | texte |  |  |
