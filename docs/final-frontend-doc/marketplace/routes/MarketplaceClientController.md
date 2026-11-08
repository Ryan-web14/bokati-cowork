# MarketplaceClientController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [marketplace](../README.md) · 10 route(s).

## Routes

<a id="r-get-sni-api-v1-client-companies-customerid-marketplace-orders"></a>
### GET /sni/api/v1/client/companies/{customerId}/marketplace/orders

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<MarketplaceDtos.OrderView>`
- Modèles : [MarketplaceDtos.OrderView](#m-MarketplaceDtos-OrderView)
- Code : `src/main/java/com/sni/bokaticowork/features/marketplace/controller/MarketplaceClientController.java`, méthode `companyOrders`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `limit` | requête | `int` |  | 100 |

<a id="r-get-sni-api-v1-client-marketplace-cart"></a>
### GET /sni/api/v1/client/marketplace/cart

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `MarketplaceDtos.OrderView`
- Modèles : [MarketplaceDtos.OrderView](#m-MarketplaceDtos-OrderView)
- Code : `src/main/java/com/sni/bokaticowork/features/marketplace/controller/MarketplaceClientController.java`, méthode `cart`

<a id="r-post-sni-api-v1-client-marketplace-cart-checkout"></a>
### POST /sni/api/v1/client/marketplace/cart/checkout

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (facultatif) : `MarketplaceDtos.CheckoutRequest`
- Réponse : `MarketplaceDtos.OrderView`
- Modèles : [MarketplaceDtos.OrderView](#m-MarketplaceDtos-OrderView), [MarketplaceDtos.CheckoutRequest](#m-MarketplaceDtos-CheckoutRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/marketplace/controller/MarketplaceClientController.java`, méthode `checkout`

<a id="r-post-sni-api-v1-client-marketplace-cart-lines"></a>
### POST /sni/api/v1/client/marketplace/cart/lines

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `MarketplaceDtos.CartLineRequest`
- Réponse : `MarketplaceDtos.OrderView`
- Modèles : [MarketplaceDtos.OrderView](#m-MarketplaceDtos-OrderView), [MarketplaceDtos.CartLineRequest](#m-MarketplaceDtos-CartLineRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/marketplace/controller/MarketplaceClientController.java`, méthode `addLine`

<a id="r-delete-sni-api-v1-client-marketplace-cart-lines-lineid"></a>
### DELETE /sni/api/v1/client/marketplace/cart/lines/{lineId}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `MarketplaceDtos.OrderView`
- Modèles : [MarketplaceDtos.OrderView](#m-MarketplaceDtos-OrderView)
- Code : `src/main/java/com/sni/bokaticowork/features/marketplace/controller/MarketplaceClientController.java`, méthode `removeLine`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `lineId` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-client-marketplace-cart-lines-lineid"></a>
### PATCH /sni/api/v1/client/marketplace/cart/lines/{lineId}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `MarketplaceDtos.CartQuantityRequest`
- Réponse : `MarketplaceDtos.OrderView`
- Modèles : [MarketplaceDtos.OrderView](#m-MarketplaceDtos-OrderView), [MarketplaceDtos.CartQuantityRequest](#m-MarketplaceDtos-CartQuantityRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/marketplace/controller/MarketplaceClientController.java`, méthode `updateLine`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `lineId` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-client-marketplace-offers"></a>
### GET /sni/api/v1/client/marketplace/offers

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<MarketplaceDtos.OfferView>`
- Modèles : [MarketplaceDtos.OfferView](#m-MarketplaceDtos-OfferView)
- Code : `src/main/java/com/sni/bokaticowork/features/marketplace/controller/MarketplaceClientController.java`, méthode `offers`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `category` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-client-marketplace-orders"></a>
### GET /sni/api/v1/client/marketplace/orders

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<MarketplaceDtos.OrderView>`
- Modèles : [MarketplaceDtos.OrderView](#m-MarketplaceDtos-OrderView)
- Code : `src/main/java/com/sni/bokaticowork/features/marketplace/controller/MarketplaceClientController.java`, méthode `myOrders`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `limit` | requête | `int` |  | 50 |

<a id="r-get-sni-api-v1-client-marketplace-orders-ordernumber"></a>
### GET /sni/api/v1/client/marketplace/orders/{orderNumber}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `MarketplaceDtos.OrderView`
- Modèles : [MarketplaceDtos.OrderView](#m-MarketplaceDtos-OrderView)
- Code : `src/main/java/com/sni/bokaticowork/features/marketplace/controller/MarketplaceClientController.java`, méthode `myOrder`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `orderNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-marketplace-orders-ordernumber-cancel"></a>
### POST /sni/api/v1/client/marketplace/orders/{orderNumber}/cancel

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (facultatif) : `MarketplaceDtos.MemberCancelRequest`
- Réponse : `MarketplaceDtos.OrderView`
- Modèles : [MarketplaceDtos.OrderView](#m-MarketplaceDtos-OrderView), [MarketplaceDtos.MemberCancelRequest](#m-MarketplaceDtos-MemberCancelRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/marketplace/controller/MarketplaceClientController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `orderNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

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

<a id="m-MarketplaceDtos-CheckoutRequest"></a>
#### MarketplaceDtos.CheckoutRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `billingCompanyCode` | texte |  |  |

<a id="m-MarketplaceDtos-CartLineRequest"></a>
#### MarketplaceDtos.CartLineRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `offerId` | entier int64 ⚠ | oui |  |
| `quantity` | entier | oui | min `1` |

<a id="m-MarketplaceDtos-CartQuantityRequest"></a>
#### MarketplaceDtos.CartQuantityRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `quantity` | entier | oui | min `0` |

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

<a id="m-MarketplaceDtos-MemberCancelRequest"></a>
#### MarketplaceDtos.MemberCancelRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  | longueur min `0`, longueur max `500` |

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
