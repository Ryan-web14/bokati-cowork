# CouponController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 11 route(s).

## Routes

<a id="r-get-sni-api-v1-coupons-available"></a>
### GET /sni/api/v1/coupons/available

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<CouponResponse>`
- Modèles : [CouponResponse](#m-CouponResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/coupon/controller/CouponController.java`, méthode `available`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriberType` | requête | `String` | oui |  |
| `subscriberCode` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-coupons-batches"></a>
### GET /sni/api/v1/coupons/batches

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `PaginatedResponse<CouponBatchView>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [CouponBatchView](#m-CouponBatchView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/coupon/controller/CouponController.java`, méthode `batches`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `promotionCode` | requête | `String` |  |  |
| `page` | requête | `int` |  | 0 |
| `size` | requête | `int` |  | 20 |

<a id="r-post-sni-api-v1-coupons-batches"></a>
### POST /sni/api/v1/coupons/batches

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `CreateCouponBatchRequest`
- Réponse : `Map<String, Object>`
- Modèles : [CreateCouponBatchRequest](#m-CreateCouponBatchRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/coupon/controller/CouponController.java`, méthode `createBatch`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `createdBy` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-coupons-batches-batchcode"></a>
### GET /sni/api/v1/coupons/batches/{batchCode}

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `CouponBatchView`
- Modèles : [CouponBatchView](#m-CouponBatchView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/coupon/controller/CouponController.java`, méthode `batch`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `batchCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-coupons-batches-batchcode-coupons"></a>
### GET /sni/api/v1/coupons/batches/{batchCode}/coupons

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `PaginatedResponse<CouponLineView>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [CouponLineView](#m-CouponLineView), [CouponStatus](#m-CouponStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/coupon/controller/CouponController.java`, méthode `batchCoupons`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `batchCode` | chemin | `String` | oui |  |
| `status` | requête | `CouponStatus` |  |  |
| `page` | requête | `int` |  | 0 |
| `size` | requête | `int` |  | 50 |

<a id="r-get-sni-api-v1-coupons-batches-batchcode-export"></a>
### GET /sni/api/v1/coupons/batches/{batchCode}/export

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `String`
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/coupon/controller/CouponController.java`, méthode `exportBatch`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `batchCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-coupons-batches-batchcode-revoke"></a>
### POST /sni/api/v1/coupons/batches/{batchCode}/revoke

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Réponse : `CouponBatchRevocation`
- Modèles : [CouponBatchRevocation](#m-CouponBatchRevocation)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/coupon/controller/CouponController.java`, méthode `revokeBatch`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `batchCode` | chemin | `String` | oui |  |
| `reason` | requête | `String` | oui |  |
| `revokedBy` | requête | `String` |  |  |

<a id="r-delete-sni-api-v1-coupons-reservations"></a>
### DELETE /sni/api/v1/coupons/reservations

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Réponse : `Map<String, Integer>`
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/coupon/controller/CouponController.java`, méthode `release`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `cartReference` | requête | `String` | oui |  |
| `reason` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-coupons-code"></a>
### GET /sni/api/v1/coupons/{code}

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `CouponResponse`
- Modèles : [CouponResponse](#m-CouponResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/coupon/controller/CouponController.java`, méthode `coupon`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-coupons-code-reserve"></a>
### POST /sni/api/v1/coupons/{code}/reserve

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
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

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
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

<a id="m-CouponBatchView"></a>
#### CouponBatchView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `batchCode` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `promotionCode` | texte |  |  |
| `promotionName` | texte |  |  |
| `couponKind` | enum : `SINGLE_USE`, `MULTI_USE`, `UNIQUE_PER_SUBSCRIBER` |  |  |
| `codePrefix` | texte |  |  |
| `codeLength` | entier |  |  |
| `requestedQuantity` | entier |  |  |
| `generatedQuantity` | entier |  |  |
| `maxRedemptionsPerCoupon` | entier |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `channel` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `createdBy` | texte |  |  |
| `totalCoupons` | entier int64 ⚠ |  |  |
| `countsByStatus` | objet { clé : entier int64 ⚠ } |  |  |
| `totalRedemptions` | entier int64 ⚠ |  |  |
| `totalReserved` | entier int64 ⚠ |  |  |

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

<a id="m-CouponLineView"></a>
#### CouponLineView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `status` | enum : `ACTIVE`, `USED`, `EXPIRED`, `REVOKED` |  |  |
| `couponKind` | enum : `SINGLE_USE`, `MULTI_USE`, `UNIQUE_PER_SUBSCRIBER` |  |  |
| `assignedToType` | texte |  |  |
| `assignedToCode` | texte |  |  |
| `maxRedemptions` | entier |  |  |
| `redemptionCount` | entier |  |  |
| `reservedCount` | entier |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `issuedAt` | date-heure ISO 8601 UTC |  |  |
| `revokedAt` | date-heure ISO 8601 UTC |  |  |
| `revokedReason` | texte |  |  |

<a id="m-CouponStatus"></a>
#### CouponStatus

Énumération : `ACTIVE`, `USED`, `EXPIRED`, `REVOKED`

<a id="m-CouponBatchRevocation"></a>
#### CouponBatchRevocation

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `batchCode` | texte |  |  |
| `total` | entier |  |  |
| `revoked` | entier |  |  |
| `alreadyRevoked` | entier |  |  |
| `exhausted` | entier |  |  |
| `expired` | entier |  |  |
