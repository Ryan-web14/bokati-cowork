# PublicReferralController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 2 route(s).

## Routes

<a id="r-get-sni-api-v1-public-referrals-code"></a>
### GET /sni/api/v1/public/referrals/{code}

- Accès : public, sans jeton
- Réponse : `ReferralDtos.PublicLinkView`
- Modèles : [ReferralDtos.PublicLinkView](#m-ReferralDtos-PublicLinkView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/PublicReferralController.java`, méthode `link`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-public-referrals-code-clicks"></a>
### POST /sni/api/v1/public/referrals/{code}/clicks

- Accès : public, sans jeton
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/PublicReferralController.java`, méthode `click`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ReferralDtos-PublicLinkView"></a>
#### ReferralDtos.PublicLinkView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `valid` | booléen |  |  |
| `reason` | texte |  |  |
| `programName` | texte |  |  |
| `programDescription` | texte |  |  |
| `refereePromotion` | [ReferralDtos.PromotionRef](#m-ReferralDtos-PromotionRef) |  |  |
| `referrerFirstName` | texte |  |  |

<a id="m-ReferralDtos-PromotionRef"></a>
#### ReferralDtos.PromotionRef

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
