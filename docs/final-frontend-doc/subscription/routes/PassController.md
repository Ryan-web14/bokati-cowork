# PassController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 8 route(s).

## Routes

<a id="r-get-sni-api-v1-passes"></a>
### GET /sni/api/v1/passes

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `PaginatedResponse<PassResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [PassResponse](#m-PassResponse), [SubscriberType](#m-SubscriberType), [PassType](#m-PassType), [PassStatus](#m-PassStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ownerType` | requête | `SubscriberType` |  |  |
| `ownerCode` | requête | `String` |  |  |
| `passType` | requête | `PassType` |  |  |
| `status` | requête | `PassStatus` |  |  |
| `expiringBefore` | requête | `Instant` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-post-sni-api-v1-passes"></a>
### POST /sni/api/v1/passes

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE`
- Corps JSON (obligatoire) : `CreatePassRequest`
- Réponse : `PassResponse`
- Modèles : [PassResponse](#m-PassResponse), [CreatePassRequest](#m-CreatePassRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassController.java`, méthode `create`

<a id="r-get-sni-api-v1-passes-passnumber"></a>
### GET /sni/api/v1/passes/{passNumber}

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `PassResponse`
- Modèles : [PassResponse](#m-PassResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `passNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-passes-passnumber-activate"></a>
### POST /sni/api/v1/passes/{passNumber}/activate

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:ACTIVATE`
- Réponse : `PassResponse`
- Modèles : [PassResponse](#m-PassResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassController.java`, méthode `activate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `passNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-passes-passnumber-cancel"></a>
### PATCH /sni/api/v1/passes/{passNumber}/cancel

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CANCEL`
- Corps JSON (facultatif) : `Map<String, String>`
- Réponse : `PassResponse`
- Modèles : [PassResponse](#m-PassResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `passNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-passes-passnumber-events"></a>
### GET /sni/api/v1/passes/{passNumber}/events

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `List<PassController.PassEventView>`
- Modèles : [PassController.PassEventView](#m-PassController-PassEventView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassController.java`, méthode `events`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `passNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-passes-passnumber-history"></a>
### GET /sni/api/v1/passes/{passNumber}/history

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `List<PassController.PassStatusChangeView>`
- Modèles : [PassController.PassStatusChangeView](#m-PassController-PassStatusChangeView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassController.java`, méthode `history`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `passNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-passes-passnumber-renewal"></a>
### GET /sni/api/v1/passes/{passNumber}/renewal

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:ACTIVATE`
- Réponse : `PassController.PassRenewalView`
- Modèles : [PassController.PassRenewalView](#m-PassController-PassRenewalView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassController.java`, méthode `renewal`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `passNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-PassResponse"></a>
#### PassResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `passNumber` | texte |  |  |
| `passType` | enum : `DAY_PASS`, `TIME_PACK`, `VISITOR_PASS`, `MEETING_ROOM_PACK`, `PROMOTIONAL_PASS`, `SUBSCRIPTION_PASS`, `COMPANY_SHARED_PASS`, `CUSTOM` |  |  |
| `ownerType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `ownerCode` | texte |  |  |
| `subscriptionNumber` | texte |  |  |
| `status` | enum : `DRAFT`, `PENDING_ACTIVATION`, `ACTIVE`, `PARTIALLY_USED`, `CONSUMED`, `PAST_DUE`, `EXPIRED`, `CANCELLED`, `SUSPENDED` |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `transferable` | booléen |  |  |
| `shareable` | booléen |  |  |
| `maxUses` | entier |  |  |
| `usedCount` | entier |  |  |
| `contractCode` | texte |  |  |
| `planCode` | texte |  |  |
| `planName` | texte |  |  |
| `planVersion` | entier |  |  |
| `currency` | texte |  |  |
| `subtotalAmount` | nombre décimal |  |  |
| `taxAmount` | nombre décimal |  |  |
| `totalAmount` | nombre décimal |  |  |
| `autoRenew` | booléen |  |  |
| `nextRenewalDate` | date-heure ISO 8601 UTC |  |  |
| `renewalCount` | entier |  |  |
| `entitlements` | [EntitlementGrantResponse](#m-EntitlementGrantResponse)[] |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-SubscriberType"></a>
#### SubscriberType

Énumération : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY`

<a id="m-PassType"></a>
#### PassType

Énumération : `DAY_PASS`, `TIME_PACK`, `VISITOR_PASS`, `MEETING_ROOM_PACK`, `PROMOTIONAL_PASS`, `SUBSCRIPTION_PASS`, `COMPANY_SHARED_PASS`, `CUSTOM`

<a id="m-PassStatus"></a>
#### PassStatus

Énumération : `DRAFT`, `PENDING_ACTIVATION`, `ACTIVE`, `PARTIALLY_USED`, `CONSUMED`, `PAST_DUE`, `EXPIRED`, `CANCELLED`, `SUSPENDED`

<a id="m-CreatePassRequest"></a>
#### CreatePassRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `passType` | enum : `DAY_PASS`, `TIME_PACK`, `VISITOR_PASS`, `MEETING_ROOM_PACK`, `PROMOTIONAL_PASS`, `SUBSCRIPTION_PASS`, `COMPANY_SHARED_PASS`, `CUSTOM` | oui |  |
| `ownerType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` | oui |  |
| `ownerCode` | texte | oui | longueur min `1` |
| `subscriptionNumber` | texte |  |  |
| `planVersionId` | entier int64 ⚠ |  |  |
| `name` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `validFrom` | texte | oui |  |
| `validUntil` | texte |  |  |
| `transferable` | booléen |  |  |
| `shareable` | booléen |  |  |
| `maxUses` | entier |  |  |
| `metadataJson` | texte |  |  |
| `idempotencyKey` | texte |  |  |
| `entitlements` | [PassEntitlementRequest](#m-PassEntitlementRequest)[] |  |  |

<a id="m-PassController-PassEventView"></a>
#### PassController.PassEventView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `passNumber` | texte |  |  |
| `eventType` | texte |  |  |
| `payloadJson` | texte |  |  |
| `occurredAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-PassController-PassStatusChangeView"></a>
#### PassController.PassStatusChangeView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `passNumber` | texte |  |  |
| `fromStatus` | texte |  |  |
| `toStatus` | texte |  |  |
| `reason` | texte |  |  |
| `changedBy` | texte |  |  |
| `changedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-PassController-PassRenewalView"></a>
#### PassController.PassRenewalView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `passNumber` | texte |  |  |
| `duration` | entier |  |  |
| `durationUnit` | texte |  |  |
| `nextRenewalDate` | date-heure ISO 8601 UTC |  |  |
| `currentPeriodStart` | date-heure ISO 8601 UTC |  |  |
| `currentPeriodEnd` | date-heure ISO 8601 UTC |  |  |
| `status` | texte |  |  |
| `retryCount` | entier |  |  |
| `lastAttemptAt` | date-heure ISO 8601 UTC |  |  |

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

<a id="m-PassEntitlementRequest"></a>
#### PassEntitlementRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `entitlementCode` | texte | oui | longueur min `1` |
| `quantity` | nombre décimal |  |  |
| `unlimited` | booléen |  |  |
| `validFrom` | texte |  |  |
| `validUntil` | texte |  |  |
