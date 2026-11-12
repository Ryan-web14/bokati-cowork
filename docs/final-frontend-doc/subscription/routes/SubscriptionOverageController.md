# SubscriptionOverageController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 3 route(s).

## Routes

<a id="r-get-sni-api-v1-subscription-overage-charges"></a>
### GET /sni/api/v1/subscription-overage/charges

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `List<OverageChargeResponse>`
- Modèles : [OverageChargeResponse](#m-OverageChargeResponse), [SubscriberType](#m-SubscriberType)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/overage/controller/SubscriptionOverageController.java`, méthode `listCharges`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | requête | `String` |  |  |
| `ownerType` | requête | `SubscriberType` |  |  |
| `ownerCode` | requête | `String` |  |  |
| `entitlementCode` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-subscription-overage-policies"></a>
### GET /sni/api/v1/subscription-overage/policies

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `List<OveragePolicyResponse>`
- Modèles : [OveragePolicyResponse](#m-OveragePolicyResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/overage/controller/SubscriptionOverageController.java`, méthode `listPolicies`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `planVersionId` | requête | `Long` |  |  |
| `entitlementCode` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-subscription-overage-policies"></a>
### POST /sni/api/v1/subscription-overage/policies

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE`
- Corps JSON (obligatoire) : `CreateOveragePolicyRequest`
- Réponse : `OveragePolicyResponse`
- Modèles : [OveragePolicyResponse](#m-OveragePolicyResponse), [CreateOveragePolicyRequest](#m-CreateOveragePolicyRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/overage/controller/SubscriptionOverageController.java`, méthode `createPolicy`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-OverageChargeResponse"></a>
#### OverageChargeResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `chargeNumber` | texte |  |  |
| `subscriptionNumber` | texte |  |  |
| `usageNumber` | texte |  |  |
| `billableNumber` | texte |  |  |
| `ownerType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `ownerCode` | texte |  |  |
| `entitlementCode` | texte |  |  |
| `overageQuantity` | nombre décimal |  |  |
| `unitPrice` | nombre décimal |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-SubscriberType"></a>
#### SubscriberType

Énumération : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY`

<a id="m-OveragePolicyResponse"></a>
#### OveragePolicyResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `planVersionId` | entier int64 ⚠ |  |  |
| `planCode` | texte |  |  |
| `entitlementCode` | texte |  |  |
| `mode` | enum : `BLOCK`, `BILLABLE`, `ALLOW_UNBILLED` |  |  |
| `unitPrice` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `freeQuantity` | nombre décimal |  |  |
| `metadataJson` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-CreateOveragePolicyRequest"></a>
#### CreateOveragePolicyRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `planVersionId` | entier int64 ⚠ | oui |  |
| `entitlementCode` | texte | oui | longueur min `1` |
| `mode` | enum : `BLOCK`, `BILLABLE`, `ALLOW_UNBILLED` | oui |  |
| `unitPrice` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `freeQuantity` | nombre décimal |  |  |
| `metadataJson` | texte |  |  |
| `active` | booléen |  |  |
