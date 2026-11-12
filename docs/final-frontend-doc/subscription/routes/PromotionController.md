# PromotionController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-promotions"></a>
### GET /sni/api/v1/promotions

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `PaginatedResponse<PromotionResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [PromotionResponse](#m-PromotionResponse), [PromotionStatus](#m-PromotionStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/controller/PromotionController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | requête | `String` |  |  |
| `name` | requête | `String` |  |  |
| `status` | requête | `PromotionStatus` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-post-sni-api-v1-promotions"></a>
### POST /sni/api/v1/promotions

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `CreatePromotionRequest`
- Réponse : `PromotionResponse`
- Modèles : [PromotionResponse](#m-PromotionResponse), [CreatePromotionRequest](#m-CreatePromotionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/controller/PromotionController.java`, méthode `create`

<a id="r-patch-sni-api-v1-promotions-code-activate"></a>
### PATCH /sni/api/v1/promotions/{code}/activate

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Réponse : `PromotionResponse`
- Modèles : [PromotionResponse](#m-PromotionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/controller/PromotionController.java`, méthode `activate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-promotions-code-redeem"></a>
### POST /sni/api/v1/promotions/{code}/redeem

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `RedeemPromotionRequest`
- Réponse : `CouponRedemptionResponse`
- Modèles : [CouponRedemptionResponse](#m-CouponRedemptionResponse), [RedeemPromotionRequest](#m-RedeemPromotionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/controller/PromotionController.java`, méthode `redeem`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-PromotionResponse"></a>
#### PromotionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `discountType` | enum : `PERCENTAGE`, `FIXED_AMOUNT`, `FREE_TRIAL_DAYS`, `FREE_ENTITLEMENT`, `WAIVE_SETUP_FEE` |  |  |
| `discountValue` | nombre décimal |  |  |
| `startsAt` | date-heure ISO 8601 UTC |  |  |
| `endsAt` | date-heure ISO 8601 UTC |  |  |
| `maxRedemptions` | entier |  |  |
| `redemptionCount` | entier |  |  |
| `status` | enum : `DRAFT`, `ACTIVE`, `PAUSED`, `EXPIRED`, `ARCHIVED` |  |  |
| `metadataJson` | texte |  |  |

<a id="m-PromotionStatus"></a>
#### PromotionStatus

Énumération : `DRAFT`, `ACTIVE`, `PAUSED`, `EXPIRED`, `ARCHIVED`

<a id="m-CreatePromotionRequest"></a>
#### CreatePromotionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `discountType` | enum : `PERCENTAGE`, `FIXED_AMOUNT`, `FREE_TRIAL_DAYS`, `FREE_ENTITLEMENT`, `WAIVE_SETUP_FEE` | oui |  |
| `discountValue` | nombre décimal | oui | min `0.0` |
| `startsAt` | texte | oui | longueur min `1` |
| `endsAt` | texte |  |  |
| `maxRedemptions` | entier |  |  |
| `metadataJson` | texte |  |  |

<a id="m-CouponRedemptionResponse"></a>
#### CouponRedemptionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `promotionCode` | texte |  |  |
| `subscriberType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `subscriberCode` | texte |  |  |
| `subscriptionNumber` | texte |  |  |
| `redeemedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-RedeemPromotionRequest"></a>
#### RedeemPromotionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subscriberType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` | oui |  |
| `subscriberCode` | texte | oui | longueur min `1` |
| `subscriptionNumber` | texte |  |  |
