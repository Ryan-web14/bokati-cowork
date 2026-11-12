# ClientReferralController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-client-referrals"></a>
### GET /sni/api/v1/client/referrals

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<ReferralDtos.MyReferralView>`
- Modèles : [ReferralDtos.MyReferralView](#m-ReferralDtos-MyReferralView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ClientReferralController.java`, méthode `referrals`

<a id="r-get-sni-api-v1-client-referrals-links"></a>
### GET /sni/api/v1/client/referrals/links

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<ReferralDtos.LinkView>`
- Modèles : [ReferralDtos.LinkView](#m-ReferralDtos-LinkView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ClientReferralController.java`, méthode `links`

<a id="r-post-sni-api-v1-client-referrals-links"></a>
### POST /sni/api/v1/client/referrals/links

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `ReferralDtos.ClientLinkRequest`
- Réponse : `ReferralDtos.LinkView`
- Modèles : [ReferralDtos.LinkView](#m-ReferralDtos-LinkView), [ReferralDtos.ClientLinkRequest](#m-ReferralDtos-ClientLinkRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ClientReferralController.java`, méthode `link`

<a id="r-get-sni-api-v1-client-referrals-programs"></a>
### GET /sni/api/v1/client/referrals/programs

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<ReferralDtos.OpenProgramView>`
- Modèles : [ReferralDtos.OpenProgramView](#m-ReferralDtos-OpenProgramView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ClientReferralController.java`, méthode `programs`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ReferralDtos-MyReferralView"></a>
#### ReferralDtos.MyReferralView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `referralNumber` | texte |  |  |
| `programName` | texte |  |  |
| `friend` | texte |  |  |
| `status` | enum : `PENDING`, `QUALIFIED`, `REWARDED`, `REJECTED` |  |  |
| `registeredAt` | date-heure ISO 8601 UTC |  |  |
| `qualifiedAt` | date-heure ISO 8601 UTC |  |  |
| `rewardGranted` | booléen |  |  |
| `rewardAmount` | nombre décimal |  |  |
| `currency` | texte |  |  |

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

<a id="m-ReferralDtos-ClientLinkRequest"></a>
#### ReferralDtos.ClientLinkRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `programCode` | texte | oui | longueur min `1` |

<a id="m-ReferralDtos-OpenProgramView"></a>
#### ReferralDtos.OpenProgramView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `referrerPromotion` | [ReferralDtos.PromotionRef](#m-ReferralDtos-PromotionRef) |  |  |
| `refereePromotion` | [ReferralDtos.PromotionRef](#m-ReferralDtos-PromotionRef) |  |  |
| `qualificationRule` | enum : `SIGNUP`, `FIRST_PAYMENT`, `TENURE_REACHED` |  |  |
| `qualificationDelayDays` | entier |  |  |
| `maxReferralsPerReferrer` | entier |  |  |
| `myLinkCode` | texte |  |  |
| `myLinkUrl` | texte |  |  |

<a id="m-ReferralDtos-PromotionRef"></a>
#### ReferralDtos.PromotionRef

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
