# ClientPricingController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 5 route(s).

## Routes

<a id="r-post-sni-api-v1-client-cart-coupons"></a>
### POST /sni/api/v1/client/cart/coupons

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `PricingSimulationRequest`
- Réponse : `PricingSimulationResponse`
- Modèles : [PricingSimulationResponse](#m-PricingSimulationResponse), [PricingSimulationRequest](#m-PricingSimulationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/controller/ClientPricingController.java`, méthode `applyCoupon`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `cartReference` | requête | `String` | oui |  |
| `code` | requête | `String` | oui |  |

<a id="r-delete-sni-api-v1-client-cart-coupons-code"></a>
### DELETE /sni/api/v1/client/cart/coupons/{code}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `Map<String, Integer>`
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/controller/ClientPricingController.java`, méthode `removeCoupon`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `cartReference` | requête | `String` | oui |  |

<a id="r-post-sni-api-v1-client-cart-preview"></a>
### POST /sni/api/v1/client/cart/preview

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `PricingSimulationRequest`
- Réponse : `PricingSimulationResponse`
- Modèles : [PricingSimulationResponse](#m-PricingSimulationResponse), [PricingSimulationRequest](#m-PricingSimulationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/controller/ClientPricingController.java`, méthode `previewCart`

<a id="r-post-sni-api-v1-client-catalogue-prices"></a>
### POST /sni/api/v1/client/catalogue/prices

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `ClientPricingController.CatalogueRequest`
- Réponse : `List<EffectivePrice>`
- Modèles : [EffectivePrice](#m-EffectivePrice), [ClientPricingController.CatalogueRequest](#m-ClientPricingController-CatalogueRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/controller/ClientPricingController.java`, méthode `cataloguePrices`

<a id="r-post-sni-api-v1-client-promotions-available"></a>
### POST /sni/api/v1/client/promotions/available

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `PricingSimulationRequest`
- Réponse : `List<ClientPromotionView>`
- Modèles : [ClientPromotionView](#m-ClientPromotionView), [PricingSimulationRequest](#m-PricingSimulationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/controller/ClientPricingController.java`, méthode `availablePromotions`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-PricingSimulationResponse"></a>
#### PricingSimulationResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `catalogTotal` | nombre décimal |  |  |
| `originalTotal` | nombre décimal |  |  |
| `discountTotal` | nombre décimal |  |  |
| `finalTotal` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `appliedRules` | [PricingResult.AppliedRule](#m-PricingResult-AppliedRule)[] |  |  |
| `rejections` | [PricingResult.Rejection](#m-PricingResult-Rejection)[] |  |  |
| `grants` | [PricingResult.NonMonetaryGrant](#m-PricingResult-NonMonetaryGrant)[] |  |  |
| `priceOverrides` | [PricingResult.PriceOverride](#m-PricingResult-PriceOverride)[] |  |  |

<a id="m-PricingSimulationRequest"></a>
#### PricingSimulationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subscriberType` | texte |  |  |
| `subscriberCode` | texte |  |  |
| `subscriberSegment` | texte |  |  |
| `kycLevel` | entier |  |  |
| `tenureMonths` | entier |  |  |
| `firstPurchase` | booléen |  |  |
| `lines` | [PricingSimulationRequest.Line](#m-PricingSimulationRequest-Line)[] | oui | éléments min `1` |
| `billingCycle` | texte |  |  |
| `channel` | texte |  |  |
| `paymentMethod` | texte |  |  |
| `locationCode` | texte |  |  |
| `currency` | texte |  |  |
| `couponCodes` | texte[] |  |  |
| `evaluationDate` | date-heure ISO 8601 UTC |  |  |
| `promotionsAllowed` | booléen |  |  |

<a id="m-EffectivePrice"></a>
#### EffectivePrice

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `scope` | enum : `WHOLE_ORDER`, `LINE`, `PLAN`, `ADDON`, `PASS`, `ENTITLEMENT`, `SERVICE`, `RESOURCE`, `INVENTORY_ITEM`, `CATEGORY` |  |  |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `listPrice` | nombre décimal |  |  |
| `effectivePrice` | nombre décimal |  |  |
| `savingsAmount` | nombre décimal |  |  |
| `savingsPercent` | nombre décimal |  |  |
| `priceSource` | enum : `CATALOGUE`, `PRICE_LIST`, `PROMOTION`, `OVERRIDE` |  |  |
| `promotionCode` | texte |  |  |
| `promotionLabel` | texte |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `requiresCoupon` | booléen |  |  |
| `conditionsSummary` | texte |  |  |
| `currency` | texte |  |  |

<a id="m-ClientPricingController-CatalogueRequest"></a>
#### ClientPricingController.CatalogueRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `context` | [PricingSimulationRequest](#m-PricingSimulationRequest) |  |  |
| `items` | [PriceableRef](#m-PriceableRef)[] | oui | éléments min `1` |

<a id="m-ClientPromotionView"></a>
#### ClientPromotionView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `kind` | enum : `AUTOMATIC`, `NOMINATIVE`, `COUPON_ASSIGNED`, `COUPON_PUBLIC` |  |  |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `description` | texte |  |  |
| `estimatedSaving` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `status` | enum : `APPLICABLE`, `ALREADY_APPLIED`, `NOT_ELIGIBLE`, `EXPIRED`, `EXHAUSTED` |  |  |
| `ineligibilityReason` | texte |  |  |

<a id="m-PricingResult-AppliedRule"></a>
#### PricingResult.AppliedRule

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `order` | entier |  |  |
| `sourceType` | enum : `PROMOTION`, `COUPON`, `PRICE_LIST`, `REFERRAL`, `MANUAL` |  |  |
| `sourceCode` | texte |  |  |
| `sourceName` | texte |  |  |
| `rewardType` | enum : `PERCENTAGE_OFF`, `FIXED_AMOUNT_OFF`, `WAIVE_SETUP_FEE`, `FREE_PERIODS`, `FREE_TRIAL_DAYS`, `FREE_ENTITLEMENT`, `EXTRA_ENTITLEMENT_UNITS`, `FREE_ADDON`, `UPGRADE_TIER`, `WALLET_CREDIT` |  |  |
| `lineReference` | texte |  |  |
| `originalAmount` | nombre décimal |  |  |
| `discountAmount` | nombre décimal |  |  |
| `explanation` | texte |  |  |

<a id="m-PricingResult-Rejection"></a>
#### PricingResult.Rejection

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `sourceType` | enum : `PROMOTION`, `COUPON`, `PRICE_LIST`, `REFERRAL`, `MANUAL` |  |  |
| `sourceCode` | texte |  |  |
| `reason` | texte |  |  |

<a id="m-PricingResult-NonMonetaryGrant"></a>
#### PricingResult.NonMonetaryGrant

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `sourceType` | enum : `PROMOTION`, `COUPON`, `PRICE_LIST`, `REFERRAL`, `MANUAL` |  |  |
| `sourceCode` | texte |  |  |
| `rewardType` | enum : `PERCENTAGE_OFF`, `FIXED_AMOUNT_OFF`, `WAIVE_SETUP_FEE`, `FREE_PERIODS`, `FREE_TRIAL_DAYS`, `FREE_ENTITLEMENT`, `EXTRA_ENTITLEMENT_UNITS`, `FREE_ADDON`, `UPGRADE_TIER`, `WALLET_CREDIT` |  |  |
| `targetCode` | texte |  |  |
| `value` | nombre décimal |  |  |
| `explanation` | texte |  |  |

<a id="m-PricingResult-PriceOverride"></a>
#### PricingResult.PriceOverride

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `lineReference` | texte |  |  |
| `priceListCode` | texte |  |  |
| `priceListName` | texte |  |  |
| `catalogUnitPrice` | nombre décimal |  |  |
| `effectiveUnitPrice` | nombre décimal |  |  |
| `quantity` | entier |  |  |

<a id="m-PricingSimulationRequest-Line"></a>
#### PricingSimulationRequest.Line

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reference` | texte |  |  |
| `scope` | enum : `WHOLE_ORDER`, `LINE`, `PLAN`, `ADDON`, `PASS`, `ENTITLEMENT`, `SERVICE`, `RESOURCE`, `INVENTORY_ITEM`, `CATEGORY` | oui |  |
| `code` | texte |  |  |
| `categoryCode` | texte |  |  |
| `label` | texte |  |  |
| `quantity` | entier |  |  |
| `unitPrice` | nombre décimal | oui |  |
| `setupFee` | nombre décimal |  |  |

<a id="m-PriceableRef"></a>
#### PriceableRef

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `scope` | enum : `WHOLE_ORDER`, `LINE`, `PLAN`, `ADDON`, `PASS`, `ENTITLEMENT`, `SERVICE`, `RESOURCE`, `INVENTORY_ITEM`, `CATEGORY` |  |  |
| `code` | texte |  |  |
| `categoryCode` | texte |  |  |
| `label` | texte |  |  |
| `quantity` | entier |  |  |
| `listPrice` | nombre décimal |  |  |
| `setupFee` | nombre décimal |  |  |
| `billingCycle` | texte |  |  |
