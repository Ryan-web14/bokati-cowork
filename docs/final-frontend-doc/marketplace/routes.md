# marketplace : routes

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

20 routes. Le fonctionnement et les parcours sont dans [README.md](README.md).

| Verbe | Chemin | Accès | Détail |
| --- | --- | --- | --- |
| GET | `/sni/api/v1/marketplace/offers` | ADMIN:ACCESS | [MarketplaceAdminController](routes/MarketplaceAdminController.md#r-get-sni-api-v1-marketplace-offers) |
| POST | `/sni/api/v1/marketplace/offers` | ADMIN:ACCESS | [MarketplaceAdminController](routes/MarketplaceAdminController.md#r-post-sni-api-v1-marketplace-offers) |
| GET | `/sni/api/v1/marketplace/offers/{id}` | ADMIN:ACCESS | [MarketplaceAdminController](routes/MarketplaceAdminController.md#r-get-sni-api-v1-marketplace-offers-id) |
| PATCH | `/sni/api/v1/marketplace/offers/{id}` | ADMIN:ACCESS | [MarketplaceAdminController](routes/MarketplaceAdminController.md#r-patch-sni-api-v1-marketplace-offers-id) |
| POST | `/sni/api/v1/marketplace/offers/{id}/publish` | ADMIN:ACCESS | [MarketplaceAdminController](routes/MarketplaceAdminController.md#r-post-sni-api-v1-marketplace-offers-id-publish) |
| POST | `/sni/api/v1/marketplace/offers/{id}/unpublish` | ADMIN:ACCESS | [MarketplaceAdminController](routes/MarketplaceAdminController.md#r-post-sni-api-v1-marketplace-offers-id-unpublish) |
| GET | `/sni/api/v1/marketplace/orders` | ADMIN:ACCESS | [MarketplaceAdminController](routes/MarketplaceAdminController.md#r-get-sni-api-v1-marketplace-orders) |
| GET | `/sni/api/v1/marketplace/orders/{orderNumber}` | ADMIN:ACCESS | [MarketplaceAdminController](routes/MarketplaceAdminController.md#r-get-sni-api-v1-marketplace-orders-ordernumber) |
| POST | `/sni/api/v1/marketplace/orders/{orderNumber}/cancel` | ADMIN:ACCESS | [MarketplaceAdminController](routes/MarketplaceAdminController.md#r-post-sni-api-v1-marketplace-orders-ordernumber-cancel) |
| POST | `/sni/api/v1/marketplace/orders/{orderNumber}/fulfil` | ADMIN:ACCESS | [MarketplaceAdminController](routes/MarketplaceAdminController.md#r-post-sni-api-v1-marketplace-orders-ordernumber-fulfil) |
| GET | `/sni/api/v1/client/companies/{customerId}/marketplace/orders` | membre | [MarketplaceClientController](routes/MarketplaceClientController.md#r-get-sni-api-v1-client-companies-customerid-marketplace-orders) |
| GET | `/sni/api/v1/client/marketplace/cart` | membre | [MarketplaceClientController](routes/MarketplaceClientController.md#r-get-sni-api-v1-client-marketplace-cart) |
| POST | `/sni/api/v1/client/marketplace/cart/checkout` | membre | [MarketplaceClientController](routes/MarketplaceClientController.md#r-post-sni-api-v1-client-marketplace-cart-checkout) |
| POST | `/sni/api/v1/client/marketplace/cart/lines` | membre | [MarketplaceClientController](routes/MarketplaceClientController.md#r-post-sni-api-v1-client-marketplace-cart-lines) |
| DELETE | `/sni/api/v1/client/marketplace/cart/lines/{lineId}` | membre | [MarketplaceClientController](routes/MarketplaceClientController.md#r-delete-sni-api-v1-client-marketplace-cart-lines-lineid) |
| PATCH | `/sni/api/v1/client/marketplace/cart/lines/{lineId}` | membre | [MarketplaceClientController](routes/MarketplaceClientController.md#r-patch-sni-api-v1-client-marketplace-cart-lines-lineid) |
| GET | `/sni/api/v1/client/marketplace/offers` | membre | [MarketplaceClientController](routes/MarketplaceClientController.md#r-get-sni-api-v1-client-marketplace-offers) |
| GET | `/sni/api/v1/client/marketplace/orders` | membre | [MarketplaceClientController](routes/MarketplaceClientController.md#r-get-sni-api-v1-client-marketplace-orders) |
| GET | `/sni/api/v1/client/marketplace/orders/{orderNumber}` | membre | [MarketplaceClientController](routes/MarketplaceClientController.md#r-get-sni-api-v1-client-marketplace-orders-ordernumber) |
| POST | `/sni/api/v1/client/marketplace/orders/{orderNumber}/cancel` | membre | [MarketplaceClientController](routes/MarketplaceClientController.md#r-post-sni-api-v1-client-marketplace-orders-ordernumber-cancel) |
