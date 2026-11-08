# ReferralController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 4 route(s).

## Routes

<a id="r-post-sni-api-v1-referrals"></a>
### POST /sni/api/v1/referrals

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `ReferralController.RegisterReferralRequest`
- Réponse : `Map<String, Object>`
- Modèles : [ReferralController.RegisterReferralRequest](#m-ReferralController-RegisterReferralRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ReferralController.java`, méthode `register`

<a id="r-get-sni-api-v1-referrals-links"></a>
### GET /sni/api/v1/referrals/links

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `Map<String, Object>`
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ReferralController.java`, méthode `link`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `programCode` | requête | `String` | oui |  |
| `referrerType` | requête | `String` | oui |  |
| `referrerCode` | requête | `String` | oui |  |

<a id="r-post-sni-api-v1-referrals-links-linkcode-clicks"></a>
### POST /sni/api/v1/referrals/links/{linkCode}/clicks

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ReferralController.java`, méthode `click`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `linkCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-referrals-referralnumber-reject"></a>
### POST /sni/api/v1/referrals/{referralNumber}/reject

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `Map<String, String>`
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/controller/ReferralController.java`, méthode `reject`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `referralNumber` | chemin | `String` | oui |  |
| `reason` | requête | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ReferralController-RegisterReferralRequest"></a>
#### ReferralController.RegisterReferralRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `linkCode` | texte | oui | longueur min `1` |
| `refereeType` | texte | oui | longueur min `1` |
| `refereeCode` | texte | oui | longueur min `1` |
