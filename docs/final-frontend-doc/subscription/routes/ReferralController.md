# ReferralController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 17 route(s).

## Routes

<a id="r-get-sni-api-v1-referrals"></a>
### GET /sni/api/v1/referrals

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `PaginatedResponse<ReferralDtos.ReferralView>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ReferralDtos.ReferralView](#m-ReferralDtos-ReferralView), [ReferralStatus](#m-ReferralStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ReferralController.java`, méthode `referrals`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `programCode` | requête | `String` |  |  |
| `status` | requête | `ReferralStatus` |  |  |
| `referrerCode` | requête | `String` |  |  |
| `page` | requête | `int` |  | 0 |
| `size` | requête | `int` |  | 20 |

<a id="r-post-sni-api-v1-referrals"></a>
### POST /sni/api/v1/referrals

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `ReferralController.RegisterReferralRequest`
- Réponse : `Map<String, Object>`
- Modèles : [ReferralController.RegisterReferralRequest](#m-ReferralController-RegisterReferralRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ReferralController.java`, méthode `register`

<a id="r-get-sni-api-v1-referrals-links"></a>
### GET /sni/api/v1/referrals/links

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `ReferralDtos.LinkView`
- Modèles : [ReferralDtos.LinkView](#m-ReferralDtos-LinkView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ReferralController.java`, méthode `link`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `programCode` | requête | `String` | oui |  |
| `referrerType` | requête | `String` | oui |  |
| `referrerCode` | requête | `String` | oui |  |

<a id="r-post-sni-api-v1-referrals-links"></a>
### POST /sni/api/v1/referrals/links

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `ReferralDtos.LinkRequest`
- Réponse : `ReferralDtos.LinkView`
- Modèles : [ReferralDtos.LinkView](#m-ReferralDtos-LinkView), [ReferralDtos.LinkRequest](#m-ReferralDtos-LinkRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ReferralController.java`, méthode `createLink`

<a id="r-post-sni-api-v1-referrals-links-linkcode-activate"></a>
### POST /sni/api/v1/referrals/links/{linkCode}/activate

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Réponse : `ReferralDtos.LinkView`
- Modèles : [ReferralDtos.LinkView](#m-ReferralDtos-LinkView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ReferralController.java`, méthode `activateLink`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `linkCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-referrals-links-linkcode-clicks"></a>
### POST /sni/api/v1/referrals/links/{linkCode}/clicks

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ReferralController.java`, méthode `click`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `linkCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-referrals-links-linkcode-deactivate"></a>
### POST /sni/api/v1/referrals/links/{linkCode}/deactivate

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Réponse : `ReferralDtos.LinkView`
- Modèles : [ReferralDtos.LinkView](#m-ReferralDtos-LinkView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ReferralController.java`, méthode `deactivateLink`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `linkCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-referrals-programs"></a>
### GET /sni/api/v1/referrals/programs

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<ReferralDtos.ProgramView>`
- Modèles : [ReferralDtos.ProgramView](#m-ReferralDtos-ProgramView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ReferralController.java`, méthode `programs`

<a id="r-post-sni-api-v1-referrals-programs"></a>
### POST /sni/api/v1/referrals/programs

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `ReferralDtos.ProgramRequest`
- Réponse : `ReferralDtos.ProgramView`
- Modèles : [ReferralDtos.ProgramView](#m-ReferralDtos-ProgramView), [ReferralDtos.ProgramRequest](#m-ReferralDtos-ProgramRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ReferralController.java`, méthode `createProgram`

<a id="r-get-sni-api-v1-referrals-programs-code"></a>
### GET /sni/api/v1/referrals/programs/{code}

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `ReferralDtos.ProgramView`
- Modèles : [ReferralDtos.ProgramView](#m-ReferralDtos-ProgramView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ReferralController.java`, méthode `program`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-referrals-programs-code"></a>
### PUT /sni/api/v1/referrals/programs/{code}

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Corps JSON (obligatoire) : `ReferralDtos.ProgramRequest`
- Réponse : `ReferralDtos.ProgramView`
- Modèles : [ReferralDtos.ProgramView](#m-ReferralDtos-ProgramView), [ReferralDtos.ProgramRequest](#m-ReferralDtos-ProgramRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ReferralController.java`, méthode `updateProgram`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-referrals-programs-code-activate"></a>
### POST /sni/api/v1/referrals/programs/{code}/activate

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Réponse : `ReferralDtos.ProgramView`
- Modèles : [ReferralDtos.ProgramView](#m-ReferralDtos-ProgramView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ReferralController.java`, méthode `activate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-referrals-programs-code-end"></a>
### POST /sni/api/v1/referrals/programs/{code}/end

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Réponse : `ReferralDtos.ProgramView`
- Modèles : [ReferralDtos.ProgramView](#m-ReferralDtos-ProgramView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ReferralController.java`, méthode `end`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-referrals-programs-code-links"></a>
### GET /sni/api/v1/referrals/programs/{code}/links

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `PaginatedResponse<ReferralDtos.LinkView>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ReferralDtos.LinkView](#m-ReferralDtos-LinkView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ReferralController.java`, méthode `programLinks`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `page` | requête | `int` |  | 0 |
| `size` | requête | `int` |  | 20 |

<a id="r-post-sni-api-v1-referrals-programs-code-pause"></a>
### POST /sni/api/v1/referrals/programs/{code}/pause

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Réponse : `ReferralDtos.ProgramView`
- Modèles : [ReferralDtos.ProgramView](#m-ReferralDtos-ProgramView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ReferralController.java`, méthode `pause`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-referrals-referralnumber"></a>
### GET /sni/api/v1/referrals/{referralNumber}

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `ReferralDtos.ReferralView`
- Modèles : [ReferralDtos.ReferralView](#m-ReferralDtos-ReferralView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ReferralController.java`, méthode `referral`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `referralNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-referrals-referralnumber-reject"></a>
### POST /sni/api/v1/referrals/{referralNumber}/reject

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Réponse : `Map<String, String>`
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ReferralController.java`, méthode `reject`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `referralNumber` | chemin | `String` | oui |  |
| `reason` | requête | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ReferralDtos-ReferralView"></a>
#### ReferralDtos.ReferralView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `referralNumber` | texte |  |  |
| `programCode` | texte |  |  |
| `programName` | texte |  |  |
| `linkCode` | texte |  |  |
| `referrerType` | texte |  |  |
| `referrerCode` | texte |  |  |
| `refereeType` | texte |  |  |
| `refereeCode` | texte |  |  |
| `status` | enum : `PENDING`, `QUALIFIED`, `REWARDED`, `REJECTED` |  |  |
| `registeredAt` | date-heure ISO 8601 UTC |  |  |
| `qualifiedAt` | date-heure ISO 8601 UTC |  |  |
| `rejectedAt` | date-heure ISO 8601 UTC |  |  |
| `rejectionReason` | texte |  |  |
| `referrerRewardGranted` | booléen |  |  |
| `referrerRewardAmount` | nombre décimal |  |  |
| `referrerRewardedAt` | date-heure ISO 8601 UTC |  |  |
| `refereeRewardGranted` | booléen |  |  |
| `refereeRewardAmount` | nombre décimal |  |  |
| `refereeRewardedAt` | date-heure ISO 8601 UTC |  |  |
| `currency` | texte |  |  |

<a id="m-ReferralStatus"></a>
#### ReferralStatus

Énumération : `PENDING`, `QUALIFIED`, `REWARDED`, `REJECTED`

<a id="m-ReferralController-RegisterReferralRequest"></a>
#### ReferralController.RegisterReferralRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `linkCode` | texte | oui | longueur min `1` |
| `refereeType` | texte | oui | longueur min `1` |
| `refereeCode` | texte | oui | longueur min `1` |

<a id="m-ReferralDtos-LinkView"></a>
#### ReferralDtos.LinkView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `url` | texte |  |  |
| `programCode` | texte |  |  |
| `programName` | texte |  |  |
| `programOpen` | booléen |  |  |
| `referrerType` | texte |  |  |
| `referrerCode` | texte |  |  |
| `clickCount` | entier |  |  |
| `signupCount` | entier |  |  |
| `active` | booléen |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-ReferralDtos-LinkRequest"></a>
#### ReferralDtos.LinkRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `programCode` | texte | oui | longueur min `1` |
| `referrerType` | texte | oui | longueur min `1` |
| `referrerCode` | texte | oui | longueur min `1` |

<a id="m-ReferralDtos-ProgramView"></a>
#### ReferralDtos.ProgramView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `status` | enum : `DRAFT`, `ACTIVE`, `PAUSED`, `ENDED` |  |  |
| `open` | booléen |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `referrerPromotion` | [ReferralDtos.PromotionRef](#m-ReferralDtos-PromotionRef) |  |  |
| `refereePromotion` | [ReferralDtos.PromotionRef](#m-ReferralDtos-PromotionRef) |  |  |
| `qualificationRule` | enum : `SIGNUP`, `FIRST_PAYMENT`, `TENURE_REACHED` |  |  |
| `qualificationDelayDays` | entier |  |  |
| `maxReferralsPerReferrer` | entier |  |  |
| `currency` | texte |  |  |
| `links` | entier int64 ⚠ |  |  |
| `referralsByStatus` | objet { clé : entier int64 ⚠ } |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `createdBy` | texte |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-ReferralDtos-ProgramRequest"></a>
#### ReferralDtos.ProgramRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte | oui | longueur min `0`, longueur max `100` |
| `name` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `referrerPromotionCode` | texte |  |  |
| `refereePromotionCode` | texte |  |  |
| `qualificationRule` | enum : `SIGNUP`, `FIRST_PAYMENT`, `TENURE_REACHED` |  |  |
| `qualificationDelayDays` | entier |  |  |
| `maxReferralsPerReferrer` | entier |  |  |
| `currency` | texte |  |  |

<a id="m-ReferralDtos-PromotionRef"></a>
#### ReferralDtos.PromotionRef

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
