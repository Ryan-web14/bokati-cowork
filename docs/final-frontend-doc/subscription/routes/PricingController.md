# PricingController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 1 route(s).

## Routes

<a id="r-post-sni-api-v1-pricing-simulate"></a>
### POST /sni/api/v1/pricing/simulate

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `PricingSimulationRequest`
- Réponse : `PricingSimulationResponse`
- Modèles : [PricingSimulationResponse](#m-PricingSimulationResponse), [PricingSimulationRequest](#m-PricingSimulationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/controller/PricingController.java`, méthode `simulate`

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
