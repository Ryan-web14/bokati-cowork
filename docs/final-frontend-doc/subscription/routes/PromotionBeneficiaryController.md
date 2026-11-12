# PromotionBeneficiaryController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 5 route(s).

## Routes

<a id="r-delete-sni-api-v1-promotions-promotioncode-beneficiaries"></a>
### DELETE /sni/api/v1/promotions/{promotionCode}/beneficiaries

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/audience/controller/PromotionBeneficiaryController.java`, méthode `revoke`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `promotionCode` | chemin | `String` | oui |  |
| `subscriberType` | requête | `String` | oui |  |
| `subscriberCode` | requête | `String` | oui |  |
| `reason` | requête | `String` | oui |  |
| `revokedBy` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-promotions-promotioncode-beneficiaries"></a>
### POST /sni/api/v1/promotions/{promotionCode}/beneficiaries

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `PromotionBeneficiaryController.AddBeneficiaryRequest`
- Réponse : `Map<String, Object>`
- Modèles : [PromotionBeneficiaryController.AddBeneficiaryRequest](#m-PromotionBeneficiaryController-AddBeneficiaryRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/audience/controller/PromotionBeneficiaryController.java`, méthode `add`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `promotionCode` | chemin | `String` | oui |  |
| `addedBy` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-promotions-promotioncode-beneficiaries-import"></a>
### POST /sni/api/v1/promotions/{promotionCode}/beneficiaries/import

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `PromotionBeneficiaryController.ImportBeneficiariesRequest`
- Réponse : `PromotionBeneficiaryService.ImportReport`
- Modèles : [PromotionBeneficiaryService.ImportReport](#m-PromotionBeneficiaryService-ImportReport), [PromotionBeneficiaryController.ImportBeneficiariesRequest](#m-PromotionBeneficiaryController-ImportBeneficiariesRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/audience/controller/PromotionBeneficiaryController.java`, méthode `importAll`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `promotionCode` | chemin | `String` | oui |  |
| `addedBy` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-promotions-promotioncode-beneficiaries-notified"></a>
### POST /sni/api/v1/promotions/{promotionCode}/beneficiaries/notified

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/audience/controller/PromotionBeneficiaryController.java`, méthode `markNotified`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `promotionCode` | chemin | `String` | oui |  |
| `subscriberType` | requête | `String` | oui |  |
| `subscriberCode` | requête | `String` | oui |  |
| `channel` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-promotions-promotioncode-beneficiaries-usage"></a>
### GET /sni/api/v1/promotions/{promotionCode}/beneficiaries/usage

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `PromotionBeneficiaryService.Usage`
- Modèles : [PromotionBeneficiaryService.Usage](#m-PromotionBeneficiaryService-Usage)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/audience/controller/PromotionBeneficiaryController.java`, méthode `usage`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `promotionCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-PromotionBeneficiaryController-AddBeneficiaryRequest"></a>
#### PromotionBeneficiaryController.AddBeneficiaryRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subscriberType` | texte | oui | longueur min `1` |
| `subscriberCode` | texte | oui | longueur min `1` |
| `reason` | texte |  |  |

<a id="m-PromotionBeneficiaryService-ImportReport"></a>
#### PromotionBeneficiaryService.ImportReport

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `promotionCode` | texte |  |  |
| `submitted` | entier |  |  |
| `accepted` | entier |  |  |
| `rejected` | entier |  |  |
| `lines` | [PromotionBeneficiaryService.ImportLine](#m-PromotionBeneficiaryService-ImportLine)[] |  |  |

<a id="m-PromotionBeneficiaryController-ImportBeneficiariesRequest"></a>
#### PromotionBeneficiaryController.ImportBeneficiariesRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `beneficiaries` | [PromotionBeneficiaryController.AddBeneficiaryRequest](#m-PromotionBeneficiaryController-AddBeneficiaryRequest)[] | oui | éléments min `1` |
| `reason` | texte |  |  |

<a id="m-PromotionBeneficiaryService-Usage"></a>
#### PromotionBeneficiaryService.Usage

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `promotionCode` | texte |  |  |
| `active` | entier int64 ⚠ |  |  |
| `redeemed` | entier int64 ⚠ |  |  |

<a id="m-PromotionBeneficiaryService-ImportLine"></a>
#### PromotionBeneficiaryService.ImportLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `line` | entier |  |  |
| `subscriberType` | texte |  |  |
| `subscriberCode` | texte |  |  |
| `outcome` | texte |  |  |
| `accepted` | booléen |  |  |
