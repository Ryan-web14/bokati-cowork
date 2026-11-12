# SubscriptionTimelineController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 2 route(s).

## Routes

<a id="r-get-sni-api-v1-subscription-timeline"></a>
### GET /sni/api/v1/subscription-timeline

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `PaginatedResponse<SubscriptionTimelineEventResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [SubscriptionTimelineEventResponse](#m-SubscriptionTimelineEventResponse), [SubscriberType](#m-SubscriberType), [SubscriptionTimelineEventType](#m-SubscriptionTimelineEventType)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/timeline/controller/SubscriptionTimelineController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | requête | `String` |  |  |
| `ownerType` | requête | `SubscriberType` |  |  |
| `ownerCode` | requête | `String` |  |  |
| `eventType` | requête | `SubscriptionTimelineEventType` |  |  |
| `occurredFrom` | requête | `Instant` |  |  |
| `occurredTo` | requête | `Instant` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=occurredAt,DESC |

<a id="r-post-sni-api-v1-subscription-timeline"></a>
### POST /sni/api/v1/subscription-timeline

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE`
- Corps JSON (obligatoire) : `CreateTimelineEventRequest`
- Réponse : `SubscriptionTimelineEventResponse`
- Modèles : [SubscriptionTimelineEventResponse](#m-SubscriptionTimelineEventResponse), [CreateTimelineEventRequest](#m-CreateTimelineEventRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/timeline/controller/SubscriptionTimelineController.java`, méthode `create`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-SubscriptionTimelineEventResponse"></a>
#### SubscriptionTimelineEventResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `eventNumber` | texte |  |  |
| `subscriptionNumber` | texte |  |  |
| `ownerType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `ownerCode` | texte |  |  |
| `eventType` | enum : `SUBSCRIPTION_CREATED`, `SUBSCRIPTION_ACTIVATED`, `SUBSCRIPTION_RENEWED`, `SUBSCRIPTION_SUSPENDED`, `SUBSCRIPTION_CANCELLED`, `SUBSCRIPTION_EXPIRED`, `STATUS_CHANGED`, `PLAN_CHANGED`, `ENTITLEMENTS_GRANTED`, `BILLING_SCHEDULED`, `ADDON_ADDED`, `ADDON_CANCELLED`, `ADDON_EXPIRED`, `PROMOTION_REDEEMED`, `PASS_ISSUED`, `PASS_CANCELLED`, `PASS_EXPIRED`, `USAGE_RECORDED`, `OVERAGE_BILLED`, `ROLLOVER_APPLIED`, `PAUSE_STARTED`, `PAUSE_ENDED`, `CONTRACT_BOUND`, `WALLET_CREDIT_APPLIED`, `FRAUD_SIGNAL_RAISED`, `NOTIFICATION_QUEUED`, `NOTIFICATION_DISPATCHED` |  |  |
| `sourceType` | texte |  |  |
| `sourceId` | texte |  |  |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `payloadJson` | texte |  |  |
| `occurredAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-SubscriberType"></a>
#### SubscriberType

Énumération : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY`

<a id="m-SubscriptionTimelineEventType"></a>
#### SubscriptionTimelineEventType

Énumération : `SUBSCRIPTION_CREATED`, `SUBSCRIPTION_ACTIVATED`, `SUBSCRIPTION_RENEWED`, `SUBSCRIPTION_SUSPENDED`, `SUBSCRIPTION_CANCELLED`, `SUBSCRIPTION_EXPIRED`, `STATUS_CHANGED`, `PLAN_CHANGED`, `ENTITLEMENTS_GRANTED`, `BILLING_SCHEDULED`, `ADDON_ADDED`, `ADDON_CANCELLED`, `ADDON_EXPIRED`, `PROMOTION_REDEEMED`, `PASS_ISSUED`, `PASS_CANCELLED`, `PASS_EXPIRED`, `USAGE_RECORDED`, `OVERAGE_BILLED`, `ROLLOVER_APPLIED`, `PAUSE_STARTED`, `PAUSE_ENDED`, `CONTRACT_BOUND`, `WALLET_CREDIT_APPLIED`, `FRAUD_SIGNAL_RAISED`, `NOTIFICATION_QUEUED`, `NOTIFICATION_DISPATCHED`

<a id="m-CreateTimelineEventRequest"></a>
#### CreateTimelineEventRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subscriptionNumber` | texte |  |  |
| `ownerType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `ownerCode` | texte |  |  |
| `eventType` | enum : `SUBSCRIPTION_CREATED`, `SUBSCRIPTION_ACTIVATED`, `SUBSCRIPTION_RENEWED`, `SUBSCRIPTION_SUSPENDED`, `SUBSCRIPTION_CANCELLED`, `SUBSCRIPTION_EXPIRED`, `STATUS_CHANGED`, `PLAN_CHANGED`, `ENTITLEMENTS_GRANTED`, `BILLING_SCHEDULED`, `ADDON_ADDED`, `ADDON_CANCELLED`, `ADDON_EXPIRED`, `PROMOTION_REDEEMED`, `PASS_ISSUED`, `PASS_CANCELLED`, `PASS_EXPIRED`, `USAGE_RECORDED`, `OVERAGE_BILLED`, `ROLLOVER_APPLIED`, `PAUSE_STARTED`, `PAUSE_ENDED`, `CONTRACT_BOUND`, `WALLET_CREDIT_APPLIED`, `FRAUD_SIGNAL_RAISED`, `NOTIFICATION_QUEUED`, `NOTIFICATION_DISPATCHED` | oui |  |
| `sourceType` | texte |  |  |
| `sourceId` | texte |  |  |
| `title` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `payloadJson` | texte |  |  |
| `occurredAt` | date-heure ISO 8601 UTC |  |  |
