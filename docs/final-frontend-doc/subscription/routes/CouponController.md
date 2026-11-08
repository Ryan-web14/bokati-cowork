# CouponController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 5 route(s).

## Routes

<a id="r-get-sni-api-v1-coupons-available"></a>
### GET /sni/api/v1/coupons/available

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `List<CouponResponse>`
- Modèles : [CouponResponse](#m-CouponResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/coupon/controller/CouponController.java`, méthode `available`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriberType` | requête | `String` | oui |  |
| `subscriberCode` | requête | `String` | oui |  |

<a id="r-post-sni-api-v1-coupons-batches"></a>
### POST /sni/api/v1/coupons/batches

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `CreateCouponBatchRequest`
- Réponse : `Map<String, Object>`
- Modèles : [CreateCouponBatchRequest](#m-CreateCouponBatchRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/coupon/controller/CouponController.java`, méthode `createBatch`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `createdBy` | requête | `String` |  |  |

<a id="r-delete-sni-api-v1-coupons-reservations"></a>
### DELETE /sni/api/v1/coupons/reservations

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `Map<String, Integer>`
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/coupon/controller/CouponController.java`, méthode `release`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `cartReference` | requête | `String` | oui |  |
| `reason` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-coupons-code-reserve"></a>
### POST /sni/api/v1/coupons/{code}/reserve

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `Map<String, Object>`
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/coupon/controller/CouponController.java`, méthode `reserve`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `cartReference` | requête | `String` | oui |  |
| `subscriberType` | requête | `String` |  |  |
| `subscriberCode` | requête | `String` |  |  |
| `reservedBy` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-coupons-code-revoke"></a>
### POST /sni/api/v1/coupons/{code}/revoke

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `CouponResponse`
- Modèles : [CouponResponse](#m-CouponResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/coupon/controller/CouponController.java`, méthode `revoke`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `reason` | requête | `String` | oui |  |
| `revokedBy` | requête | `String` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CouponResponse"></a>
#### CouponResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `promotionCode` | texte |  |  |
| `promotionName` | texte |  |  |
| `promotionDescription` | texte |  |  |
| `couponKind` | enum : `SINGLE_USE`, `MULTI_USE`, `UNIQUE_PER_SUBSCRIBER` |  |  |
| `status` | enum : `ACTIVE`, `USED`, `EXPIRED`, `REVOKED` |  |  |
| `remainingUses` | entier |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `batchCode` | texte |  |  |

<a id="m-CreateCouponBatchRequest"></a>
#### CreateCouponBatchRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `promotionCode` | texte | oui | longueur min `1` |
| `name` | texte | oui | longueur min `1` |
| `quantity` | entier |  | min `1`, max `50000` |
| `couponKind` | enum : `SINGLE_USE`, `MULTI_USE`, `UNIQUE_PER_SUBSCRIBER` | oui |  |
| `codePrefix` | texte |  |  |
| `codeLength` | entier |  | min `6`, max `32` |
| `maxRedemptionsPerCoupon` | entier |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `channel` | texte |  |  |
