# SubscriptionNotificationController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-subscription-notifications"></a>
### GET /sni/api/v1/subscription-notifications

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `PaginatedResponse<SubscriptionNotificationResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [SubscriptionNotificationResponse](#m-SubscriptionNotificationResponse), [SubscriberType](#m-SubscriberType), [SubscriptionNotificationType](#m-SubscriptionNotificationType), [SubscriptionNotificationStatus](#m-SubscriptionNotificationStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/notification/controller/SubscriptionNotificationController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | requête | `String` |  |  |
| `ownerType` | requête | `SubscriberType` |  |  |
| `ownerCode` | requête | `String` |  |  |
| `notificationType` | requête | `SubscriptionNotificationType` |  |  |
| `status` | requête | `SubscriptionNotificationStatus` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=scheduledAt,DESC |

<a id="r-post-sni-api-v1-subscription-notifications"></a>
### POST /sni/api/v1/subscription-notifications

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE`
- Corps JSON (obligatoire) : `CreateSubscriptionNotificationRequest`
- Réponse : `SubscriptionNotificationResponse`
- Modèles : [SubscriptionNotificationResponse](#m-SubscriptionNotificationResponse), [CreateSubscriptionNotificationRequest](#m-CreateSubscriptionNotificationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/notification/controller/SubscriptionNotificationController.java`, méthode `queue`

<a id="r-post-sni-api-v1-subscription-notifications-dispatch-due"></a>
### POST /sni/api/v1/subscription-notifications/dispatch-due

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE`
- Réponse : `Integer`
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/notification/controller/SubscriptionNotificationController.java`, méthode `dispatchDue`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `limit` | requête | `int` |  | 100 |

<a id="r-patch-sni-api-v1-subscription-notifications-notificationnumber-cancel"></a>
### PATCH /sni/api/v1/subscription-notifications/{notificationNumber}/cancel

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CANCEL`
- Réponse : `Integer`
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/notification/controller/SubscriptionNotificationController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `notificationNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-SubscriptionNotificationResponse"></a>
#### SubscriptionNotificationResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `notificationNumber` | texte |  |  |
| `subscriptionNumber` | texte |  |  |
| `ownerType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `ownerCode` | texte |  |  |
| `notificationType` | enum : `RENEWAL_UPCOMING`, `PAYMENT_DUE`, `OVERAGE_WARNING`, `OVERAGE_BILLED`, `ROLLOVER_APPLIED`, `PAUSE_STARTED`, `PAUSE_ENDING`, `CONTRACT_EXPIRING`, `FRAUD_REVIEW`, `WALLET_CREDIT_APPLIED`, `ENTITLEMENT_LOW_BALANCE`, `ENTITLEMENT_EXHAUSTED`, `SUBSCRIPTION_CANCELLED`, `PASS_EXPIRING`, `PASS_LOW_BALANCE`, `PASS_UNUSED` |  |  |
| `channel` | enum : `EMAIL`, `SMS`, `WHATSAPP`, `IN_APP`, `WEBHOOK` |  |  |
| `recipient` | texte |  |  |
| `subject` | texte |  |  |
| `body` | texte |  |  |
| `payloadJson` | texte |  |  |
| `scheduledAt` | date-heure ISO 8601 UTC |  |  |
| `dispatchedAt` | date-heure ISO 8601 UTC |  |  |
| `status` | enum : `PENDING`, `DISPATCHED`, `FAILED`, `CANCELLED` |  |  |
| `attemptCount` | entier |  |  |
| `errorMessage` | texte |  |  |

<a id="m-SubscriberType"></a>
#### SubscriberType

Énumération : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY`

<a id="m-SubscriptionNotificationType"></a>
#### SubscriptionNotificationType

Énumération : `RENEWAL_UPCOMING`, `PAYMENT_DUE`, `OVERAGE_WARNING`, `OVERAGE_BILLED`, `ROLLOVER_APPLIED`, `PAUSE_STARTED`, `PAUSE_ENDING`, `CONTRACT_EXPIRING`, `FRAUD_REVIEW`, `WALLET_CREDIT_APPLIED`, `ENTITLEMENT_LOW_BALANCE`, `ENTITLEMENT_EXHAUSTED`, `SUBSCRIPTION_CANCELLED`, `PASS_EXPIRING`, `PASS_LOW_BALANCE`, `PASS_UNUSED`

<a id="m-SubscriptionNotificationStatus"></a>
#### SubscriptionNotificationStatus

Énumération : `PENDING`, `DISPATCHED`, `FAILED`, `CANCELLED`

<a id="m-CreateSubscriptionNotificationRequest"></a>
#### CreateSubscriptionNotificationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subscriptionNumber` | texte |  |  |
| `ownerType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `ownerCode` | texte |  |  |
| `notificationType` | enum : `RENEWAL_UPCOMING`, `PAYMENT_DUE`, `OVERAGE_WARNING`, `OVERAGE_BILLED`, `ROLLOVER_APPLIED`, `PAUSE_STARTED`, `PAUSE_ENDING`, `CONTRACT_EXPIRING`, `FRAUD_REVIEW`, `WALLET_CREDIT_APPLIED`, `ENTITLEMENT_LOW_BALANCE`, `ENTITLEMENT_EXHAUSTED`, `SUBSCRIPTION_CANCELLED`, `PASS_EXPIRING`, `PASS_LOW_BALANCE`, `PASS_UNUSED` | oui |  |
| `channel` | enum : `EMAIL`, `SMS`, `WHATSAPP`, `IN_APP`, `WEBHOOK` | oui |  |
| `recipient` | texte | oui | longueur min `1` |
| `subject` | texte |  |  |
| `body` | texte |  |  |
| `payloadJson` | texte |  |  |
| `scheduledAt` | date-heure ISO 8601 UTC |  |  |
