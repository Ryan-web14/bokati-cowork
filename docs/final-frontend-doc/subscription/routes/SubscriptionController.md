# SubscriptionController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 13 route(s).

## Routes

<a id="r-get-sni-api-v1-subscriptions"></a>
### GET /sni/api/v1/subscriptions

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `PaginatedResponse<SubscriptionResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [SubscriptionResponse](#m-SubscriptionResponse), [SubscriberType](#m-SubscriberType), [SubscriptionStatus](#m-SubscriptionStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriberType` | requête | `SubscriberType` |  |  |
| `subscriberCode` | requête | `String` |  |  |
| `planCode` | requête | `String` |  |  |
| `status` | requête | `SubscriptionStatus` |  |  |
| `nextBillingBefore` | requête | `LocalDate` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-post-sni-api-v1-subscriptions"></a>
### POST /sni/api/v1/subscriptions

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE`
- Corps JSON (obligatoire) : `CreateSubscriptionRequest`
- Réponse : `SubscriptionResponse`
- Modèles : [SubscriptionResponse](#m-SubscriptionResponse), [CreateSubscriptionRequest](#m-CreateSubscriptionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionController.java`, méthode `create`

<a id="r-get-sni-api-v1-subscriptions-current"></a>
### GET /sni/api/v1/subscriptions/current

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `SubscriptionResponse`
- Modèles : [SubscriptionResponse](#m-SubscriptionResponse), [SubscriberType](#m-SubscriberType)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionController.java`, méthode `current`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriberType` | requête | `SubscriberType` | oui |  |
| `subscriberCode` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-subscriptions-subscriptionnumber"></a>
### GET /sni/api/v1/subscriptions/{subscriptionNumber}

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `SubscriptionResponse`
- Modèles : [SubscriptionResponse](#m-SubscriptionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-subscriptions-subscriptionnumber-activate"></a>
### PATCH /sni/api/v1/subscriptions/{subscriptionNumber}/activate

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:ACTIVATE`
- Corps JSON (facultatif) : `SubscriptionStatusChangeRequest`
- Réponse : `SubscriptionResponse`
- Modèles : [SubscriptionResponse](#m-SubscriptionResponse), [SubscriptionStatusChangeRequest](#m-SubscriptionStatusChangeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionController.java`, méthode `activate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-subscriptions-subscriptionnumber-billing-schedule"></a>
### GET /sni/api/v1/subscriptions/{subscriptionNumber}/billing-schedule

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `BillingScheduleResponse`
- Modèles : [BillingScheduleResponse](#m-BillingScheduleResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionController.java`, méthode `billingSchedule`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-subscriptions-subscriptionnumber-cancel"></a>
### PATCH /sni/api/v1/subscriptions/{subscriptionNumber}/cancel

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CANCEL`
- Corps JSON (facultatif) : `SubscriptionStatusChangeRequest`
- Réponse : `SubscriptionResponse`
- Modèles : [SubscriptionResponse](#m-SubscriptionResponse), [SubscriptionStatusChangeRequest](#m-SubscriptionStatusChangeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-subscriptions-subscriptionnumber-entitlements"></a>
### GET /sni/api/v1/subscriptions/{subscriptionNumber}/entitlements

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `List<EntitlementGrantResponse>`
- Modèles : [EntitlementGrantResponse](#m-EntitlementGrantResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionController.java`, méthode `entitlements`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-subscriptions-subscriptionnumber-history"></a>
### GET /sni/api/v1/subscriptions/{subscriptionNumber}/history

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `List<SubscriptionHistoryResponse>`
- Modèles : [SubscriptionHistoryResponse](#m-SubscriptionHistoryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionController.java`, méthode `history`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-subscriptions-subscriptionnumber-pause"></a>
### PATCH /sni/api/v1/subscriptions/{subscriptionNumber}/pause

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CANCEL`
- Corps JSON (obligatoire) : `PauseSubscriptionRequest`
- Réponse : `SubscriptionResponse`
- Modèles : [SubscriptionResponse](#m-SubscriptionResponse), [PauseSubscriptionRequest](#m-PauseSubscriptionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionController.java`, méthode `pause`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-subscriptions-subscriptionnumber-renew"></a>
### PATCH /sni/api/v1/subscriptions/{subscriptionNumber}/renew

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:ACTIVATE`
- Réponse : `SubscriptionResponse`
- Modèles : [SubscriptionResponse](#m-SubscriptionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionController.java`, méthode `renew`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-subscriptions-subscriptionnumber-resume"></a>
### PATCH /sni/api/v1/subscriptions/{subscriptionNumber}/resume

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:ACTIVATE`
- Corps JSON (facultatif) : `SubscriptionStatusChangeRequest`
- Réponse : `SubscriptionResponse`
- Modèles : [SubscriptionResponse](#m-SubscriptionResponse), [SubscriptionStatusChangeRequest](#m-SubscriptionStatusChangeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionController.java`, méthode `resume`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-subscriptions-subscriptionnumber-suspend"></a>
### PATCH /sni/api/v1/subscriptions/{subscriptionNumber}/suspend

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CANCEL`
- Corps JSON (facultatif) : `SubscriptionStatusChangeRequest`
- Réponse : `SubscriptionResponse`
- Modèles : [SubscriptionResponse](#m-SubscriptionResponse), [SubscriptionStatusChangeRequest](#m-SubscriptionStatusChangeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionController.java`, méthode `suspend`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-SubscriptionResponse"></a>
#### SubscriptionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subscriptionNumber` | texte |  |  |
| `subscriberType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `subscriberCode` | texte |  |  |
| `subscriberName` | texte |  |  |
| `planCode` | texte |  |  |
| `planName` | texte |  |  |
| `planVersion` | entier |  |  |
| `status` | enum : `DRAFT`, `PENDING_ACTIVATION`, `PENDING_DOCUMENTS`, `TRIALING`, `ACTIVE`, `GRACE_PERIOD`, `PAST_DUE`, `PAUSED`, `SUSPENDED`, `PENDING_TERMINATION`, `CANCELLED`, `EXPIRED` |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `currentPeriodStart` | date AAAA-MM-JJ |  |  |
| `currentPeriodEnd` | date AAAA-MM-JJ |  |  |
| `nextBillingDate` | date AAAA-MM-JJ |  |  |
| `autoRenew` | booléen |  |  |
| `billingCycle` | enum : `ONE_TIME`, `DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `YEARLY` |  |  |
| `currency` | texte |  |  |
| `subtotalAmount` | nombre décimal |  |  |
| `taxAmount` | nombre décimal |  |  |
| `totalAmount` | nombre décimal |  |  |
| `cancelAtPeriodEnd` | booléen |  |  |
| `pausedAt` | date-heure ISO 8601 UTC |  |  |
| `pauseUntil` | date AAAA-MM-JJ |  |  |
| `contractCode` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `pendingRenewalInvoice` | texte |  |  |
| `pendingRenewalStart` | date AAAA-MM-JJ |  |  |
| `pendingRenewalEnd` | date AAAA-MM-JJ |  |  |

<a id="m-SubscriberType"></a>
#### SubscriberType

Énumération : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY`

<a id="m-SubscriptionStatus"></a>
#### SubscriptionStatus

Énumération : `DRAFT`, `PENDING_ACTIVATION`, `PENDING_DOCUMENTS`, `TRIALING`, `ACTIVE`, `GRACE_PERIOD`, `PAST_DUE`, `PAUSED`, `SUSPENDED`, `PENDING_TERMINATION`, `CANCELLED`, `EXPIRED`

<a id="m-CreateSubscriptionRequest"></a>
#### CreateSubscriptionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `planCode` | texte | oui | longueur min `1` |
| `planVersionId` | texte |  |  |
| `billingCycle` | enum : `ONE_TIME`, `DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `YEARLY` |  |  |
| `subscriberType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` | oui |  |
| `subscriberCode` | texte | oui | longueur min `1` |
| `startDate` | date AAAA-MM-JJ | oui |  |
| `autoRenew` | booléen |  |  |
| `metadataJson` | texte |  |  |
| `autoActivate` | booléen |  |  |
| `couponCodes` | texte[] |  |  |

<a id="m-SubscriptionStatusChangeRequest"></a>
#### SubscriptionStatusChangeRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  |  |
| `changedBy` | texte |  |  |
| `cancelAtPeriodEnd` | booléen |  |  |

<a id="m-BillingScheduleResponse"></a>
#### BillingScheduleResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subscriptionNumber` | texte |  |  |
| `billingCycle` | enum : `ONE_TIME`, `DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `YEARLY` |  |  |
| `nextBillingDate` | date AAAA-MM-JJ |  |  |
| `currentPeriodStart` | date AAAA-MM-JJ |  |  |
| `currentPeriodEnd` | date AAAA-MM-JJ |  |  |
| `status` | enum : `ACTIVE`, `PAUSED`, `PAST_DUE`, `CANCELLED`, `COMPLETED` |  |  |
| `retryCount` | entier |  |  |
| `lastAttemptAt` | date-heure ISO 8601 UTC |  |  |
| `currency` | texte |  |  |
| `subtotalAmount` | nombre décimal |  |  |
| `taxAmount` | nombre décimal |  |  |
| `totalAmount` | nombre décimal |  |  |
| `recurringAmount` | nombre décimal |  |  |

<a id="m-EntitlementGrantResponse"></a>
#### EntitlementGrantResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `grantNumber` | texte |  |  |
| `entitlementCode` | texte |  |  |
| `entitlementName` | texte |  |  |
| `unit` | enum : `DAY`, `HOUR`, `CREDIT`, `BOOKING`, `VISIT`, `MEMBER`, `PERCENT`, `AMOUNT`, `BOOLEAN` |  |  |
| `ownerType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `ownerCode` | texte |  |  |
| `quantityGranted` | nombre décimal |  |  |
| `quantityRemaining` | nombre décimal |  |  |
| `unlimited` | booléen |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `status` | enum : `ACTIVE`, `RESERVED`, `DEPLETED`, `EXPIRED`, `CANCELLED`, `SUSPENDED` |  |  |
| `sourceType` | texte |  |  |
| `sourceId` | texte |  |  |

<a id="m-SubscriptionHistoryResponse"></a>
#### SubscriptionHistoryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `fromStatus` | enum : `DRAFT`, `PENDING_ACTIVATION`, `PENDING_DOCUMENTS`, `TRIALING`, `ACTIVE`, `GRACE_PERIOD`, `PAST_DUE`, `PAUSED`, `SUSPENDED`, `PENDING_TERMINATION`, `CANCELLED`, `EXPIRED` |  |  |
| `toStatus` | enum : `DRAFT`, `PENDING_ACTIVATION`, `PENDING_DOCUMENTS`, `TRIALING`, `ACTIVE`, `GRACE_PERIOD`, `PAST_DUE`, `PAUSED`, `SUSPENDED`, `PENDING_TERMINATION`, `CANCELLED`, `EXPIRED` |  |  |
| `reason` | texte |  |  |
| `changedBy` | texte |  |  |
| `changedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-PauseSubscriptionRequest"></a>
#### PauseSubscriptionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `days` | entier |  | min `1` |
| `resumeDate` | date AAAA-MM-JJ |  |  |
| `reason` | texte |  |  |
| `changedBy` | texte |  |  |
