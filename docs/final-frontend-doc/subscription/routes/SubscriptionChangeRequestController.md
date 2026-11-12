# SubscriptionChangeRequestController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-subscriptions-changes"></a>
### GET /sni/api/v1/subscriptions/changes

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `PaginatedResponse<SubscriptionChangeResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [SubscriptionChangeResponse](#m-SubscriptionChangeResponse), [SubscriptionChangeType](#m-SubscriptionChangeType), [SubscriptionChangeStatus](#m-SubscriptionChangeStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/change/controller/SubscriptionChangeRequestController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | requête | `String` |  |  |
| `changeType` | requête | `SubscriptionChangeType` |  |  |
| `status` | requête | `SubscriptionChangeStatus` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-patch-sni-api-v1-subscriptions-changes-changenumber-apply"></a>
### PATCH /sni/api/v1/subscriptions/changes/{changeNumber}/apply

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:UPDATE`
- Réponse : `SubscriptionChangeResponse`
- Modèles : [SubscriptionChangeResponse](#m-SubscriptionChangeResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/change/controller/SubscriptionChangeRequestController.java`, méthode `apply`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `changeNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-subscriptions-changes-changenumber-approve"></a>
### PATCH /sni/api/v1/subscriptions/changes/{changeNumber}/approve

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:UPDATE`
- Corps JSON (facultatif) : `Map<String, String>`
- Réponse : `SubscriptionChangeResponse`
- Modèles : [SubscriptionChangeResponse](#m-SubscriptionChangeResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/change/controller/SubscriptionChangeRequestController.java`, méthode `approve`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `changeNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-subscriptionnumber-changes"></a>
### POST /sni/api/v1/subscriptions/{subscriptionNumber}/changes

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE`
- Corps JSON (obligatoire) : `CreateSubscriptionChangeRequest`
- Réponse : `SubscriptionChangeResponse`
- Modèles : [SubscriptionChangeResponse](#m-SubscriptionChangeResponse), [CreateSubscriptionChangeRequest](#m-CreateSubscriptionChangeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/change/controller/SubscriptionChangeRequestController.java`, méthode `request`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-SubscriptionChangeResponse"></a>
#### SubscriptionChangeResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `changeNumber` | texte |  |  |
| `subscriptionNumber` | texte |  |  |
| `changeType` | enum : `UPGRADE`, `DOWNGRADE`, `ADD_ADDON`, `REMOVE_ADDON`, `QUANTITY_CHANGE` |  |  |
| `currentPlanVersionId` | entier int64 ⚠ |  |  |
| `targetPlanVersionId` | entier int64 ⚠ |  |  |
| `effectivePolicy` | enum : `IMMEDIATE`, `NEXT_BILLING_PERIOD`, `CUSTOM_DATE` |  |  |
| `effectiveDate` | date AAAA-MM-JJ |  |  |
| `prorationAmount` | nombre décimal |  |  |
| `prorationPolicy` | enum : `DAILY`, `MONTH_STARTED`, `NONE` |  |  |
| `prorationCredit` | booléen |  |  |
| `prorationBillableNumber` | texte |  |  |
| `status` | enum : `REQUESTED`, `APPROVED`, `APPLIED`, `REJECTED`, `CANCELLED` |  |  |
| `reason` | texte |  |  |
| `requestedBy` | texte |  |  |
| `approvedBy` | texte |  |  |
| `appliedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-SubscriptionChangeType"></a>
#### SubscriptionChangeType

Énumération : `UPGRADE`, `DOWNGRADE`, `ADD_ADDON`, `REMOVE_ADDON`, `QUANTITY_CHANGE`

<a id="m-SubscriptionChangeStatus"></a>
#### SubscriptionChangeStatus

Énumération : `REQUESTED`, `APPROVED`, `APPLIED`, `REJECTED`, `CANCELLED`

<a id="m-CreateSubscriptionChangeRequest"></a>
#### CreateSubscriptionChangeRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `changeType` | enum : `UPGRADE`, `DOWNGRADE`, `ADD_ADDON`, `REMOVE_ADDON`, `QUANTITY_CHANGE` | oui |  |
| `targetPlanVersionId` | entier int64 ⚠ |  |  |
| `effectivePolicy` | enum : `IMMEDIATE`, `NEXT_BILLING_PERIOD`, `CUSTOM_DATE` | oui |  |
| `effectiveDate` | date AAAA-MM-JJ |  |  |
| `prorationAmount` | nombre décimal |  |  |
| `reason` | texte |  |  |
| `requestedBy` | texte |  |  |
