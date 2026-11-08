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
- Réponse : `List<PassEvent>`
- Modèles : [PassEvent](#m-PassEvent)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassController.java`, méthode `events`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `passNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-passes-passnumber-history"></a>
### GET /sni/api/v1/passes/{passNumber}/history

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `List<PassStatusHistory>`
- Modèles : [PassStatusHistory](#m-PassStatusHistory)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassController.java`, méthode `history`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `passNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-passes-passnumber-renewal"></a>
### GET /sni/api/v1/passes/{passNumber}/renewal

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:ACTIVATE`
- Réponse : `PassRenewalSchedule`
- Modèles : [PassRenewalSchedule](#m-PassRenewalSchedule)
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

<a id="m-PassEvent"></a>
#### PassEvent

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `pass` | [Pass](#m-Pass) |  |  |
| `eventType` | texte |  |  |
| `payloadJson` | texte |  |  |
| `occurredAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-PassStatusHistory"></a>
#### PassStatusHistory

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `pass` | [Pass](#m-Pass) |  |  |
| `fromStatus` | texte |  |  |
| `toStatus` | texte |  |  |
| `reason` | texte |  |  |
| `changedBy` | texte |  |  |
| `changedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-PassRenewalSchedule"></a>
#### PassRenewalSchedule

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `pass` | [Pass](#m-Pass) |  |  |
| `duration` | entier |  |  |
| `durationUnit` | enum : `DAY`, `WEEK`, `MONTH`, `YEAR` |  |  |
| `nextRenewalDate` | date-heure ISO 8601 UTC |  |  |
| `currentPeriodStart` | date-heure ISO 8601 UTC |  |  |
| `currentPeriodEnd` | date-heure ISO 8601 UTC |  |  |
| `status` | enum : `ACTIVE`, `PAUSED`, `PAST_DUE`, `CANCELLED`, `COMPLETED` |  |  |
| `retryCount` | entier |  |  |
| `lastAttemptAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

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

<a id="m-Pass"></a>
#### Pass

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `passNumber` | texte |  |  |
| `idempotencyKey` | texte |  |  |
| `passType` | enum : `DAY_PASS`, `TIME_PACK`, `VISITOR_PASS`, `MEETING_ROOM_PACK`, `PROMOTIONAL_PASS`, `SUBSCRIPTION_PASS`, `COMPANY_SHARED_PASS`, `CUSTOM` |  |  |
| `ownerType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `ownerCode` | texte |  |  |
| `subscription` | [Subscription](#m-Subscription) |  |  |
| `planVersion` | [PlanVersion](#m-PlanVersion) |  |  |
| `passVersion` | [PassPlanVersion](#m-PassPlanVersion) |  |  |
| `status` | enum : `DRAFT`, `PENDING_ACTIVATION`, `ACTIVE`, `PARTIALLY_USED`, `CONSUMED`, `PAST_DUE`, `EXPIRED`, `CANCELLED`, `SUSPENDED` |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `transferable` | booléen |  |  |
| `shareable` | booléen |  |  |
| `maxUses` | entier |  |  |
| `usedCount` | entier |  |  |
| `metadataJson` | texte |  |  |
| `currency` | texte |  |  |
| `subtotalAmount` | nombre décimal |  |  |
| `taxAmount` | nombre décimal |  |  |
| `totalAmount` | nombre décimal |  |  |
| `autoRenew` | booléen |  |  |
| `nextRenewalDate` | date-heure ISO 8601 UTC |  |  |
| `renewalCount` | entier |  |  |
| `cancelledAt` | date-heure ISO 8601 UTC |  |  |
| `cancellationReason` | texte |  |  |
| `contractCode` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-Subscription"></a>
#### Subscription

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `subscriptionNumber` | texte |  |  |
| `subscriberType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `subscriberCode` | texte |  |  |
| `maxSeats` | entier |  |  |
| `seatCeiling` | entier |  |  |
| `member` | [Member](#m-Member) |  |  |
| `customer` | [Customer](#m-Customer) |  |  |
| `businessEntity` | [BusinessEntity](#m-BusinessEntity) |  |  |
| `planVersion` | [PlanVersion](#m-PlanVersion) |  |  |
| `status` | enum : `DRAFT`, `PENDING_ACTIVATION`, `PENDING_DOCUMENTS`, `TRIALING`, `ACTIVE`, `GRACE_PERIOD`, `PAST_DUE`, `PAUSED`, `SUSPENDED`, `PENDING_TERMINATION`, `CANCELLED`, `EXPIRED` |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `currentPeriodStart` | date AAAA-MM-JJ |  |  |
| `currentPeriodEnd` | date AAAA-MM-JJ |  |  |
| `nextBillingDate` | date AAAA-MM-JJ |  |  |
| `endNoticeStage` | entier |  |  |
| `pendingRenewalInvoice` | texte |  |  |
| `pendingRenewalStart` | date AAAA-MM-JJ |  |  |
| `pendingRenewalEnd` | date AAAA-MM-JJ |  |  |
| `trialStart` | date AAAA-MM-JJ |  |  |
| `trialEnd` | date AAAA-MM-JJ |  |  |
| `autoRenew` | booléen |  |  |
| `billingCycle` | enum : `ONE_TIME`, `DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `YEARLY` |  |  |
| `currency` | texte |  |  |
| `subtotalAmount` | nombre décimal |  |  |
| `taxAmount` | nombre décimal |  |  |
| `totalAmount` | nombre décimal |  |  |
| `cancelAtPeriodEnd` | booléen |  |  |
| `cancelledAt` | date-heure ISO 8601 UTC |  |  |
| `cancellationReason` | texte |  |  |
| `suspendedAt` | date-heure ISO 8601 UTC |  |  |
| `suspensionReason` | texte |  |  |
| `pausedAt` | date-heure ISO 8601 UTC |  |  |
| `pauseUntil` | date AAAA-MM-JJ |  |  |
| `metadataJson` | texte |  |  |
| `contractCode` | texte |  |  |
| `kycCompliant` | booléen |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-PlanVersion"></a>
#### PlanVersion

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `plan` | [SubscriptionPlan](#m-SubscriptionPlan) |  |  |
| `versionNumber` | entier |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `status` | enum : `DRAFT`, `ACTIVE`, `ARCHIVED` |  |  |
| `effectiveFrom` | date AAAA-MM-JJ |  |  |
| `effectiveTo` | date AAAA-MM-JJ |  |  |
| `termsJson` | texte |  |  |
| `scope` | enum : `CATALOGUE`, `SUBSCRIPTION` |  |  |
| `ownerSubscriptionId` | entier int64 ⚠ |  |  |
| `derivedFromVersionId` | entier int64 ⚠ |  |  |
| `floorPrice` | nombre décimal |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-PassPlanVersion"></a>
#### PassPlanVersion

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `plan` | [PassPlan](#m-PassPlan) |  |  |
| `versionNumber` | entier |  |  |
| `versionCode` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `status` | enum : `DRAFT`, `ACTIVE`, `ARCHIVED` |  |  |
| `duration` | entier |  |  |
| `durationUnit` | enum : `DAY`, `WEEK`, `MONTH`, `YEAR` |  |  |
| `maxUses` | entier |  |  |
| `autoRenewable` | booléen |  |  |
| `requiredKycLevel` | entier |  |  |
| `transferable` | booléen |  |  |
| `shareable` | booléen |  |  |
| `effectiveFrom` | date AAAA-MM-JJ |  |  |
| `effectiveTo` | date AAAA-MM-JJ |  |  |
| `termsJson` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-Member"></a>
#### Member

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `memberId` | texte |  |  |
| `customer` | [Customer](#m-Customer) |  |  |
| `user` | [Users](#m-Users) |  |  |
| `firstname` | texte |  |  |
| `lastname` | texte |  |  |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `whatsappPhone` | texte |  |  |
| `status` | enum : `ACTIVE`, `PENDING`, `UNDER_REVIEW`, `PENDING_CORRECTION`, `REJECTED`, `INACTIVE`, `SUSPENDED`, `ARCHIVED` |  |  |
| `portalAccess` | booléen |  |  |
| `portalActivatedAt` | date-heure ISO 8601 UTC |  |  |
| `kycGracePeriodEndAt` | date-heure ISO 8601 UTC |  |  |
| `avatarUrl` | texte |  |  |
| `createByAdmin` | booléen |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `deleted` | booléen |  |  |
| `displayName` | texte |  |  |

<a id="m-Customer"></a>
#### Customer

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `customerId` | texte |  |  |
| `type` | enum : `PERSON`, `COMPANY` |  |  |
| `firstname` | texte |  |  |
| `lastname` | texte |  |  |
| `companyName` | texte |  |  |
| `email` | texte |  |  |
| `billingEmail` | texte |  |  |
| `phone` | texte |  |  |
| `whatsappPhone` | texte |  |  |
| `address` | [Address](#m-Address) |  |  |
| `status` | enum : `ACTIVE`, `PENDING`, `INACTIVE`, `SUSPENDED`, `ARCHIVED` |  |  |
| `businessEntity` | [BusinessEntity](#m-BusinessEntity) |  |  |
| `paymentTermsDays` | entier |  |  |
| `creditLimit` | nombre décimal |  |  |
| `billingAddress` | [Address](#m-Address) |  |  |
| `website` | texte |  |  |
| `defaultInvoiceLinkMode` | enum : `AUTOMATIC`, `ON_CHOICE` |  |  |
| `blockedReason` | texte |  |  |
| `blockedBy` | texte |  |  |
| `blockedAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `deleted` | booléen |  |  |
| `member` | booléen |  |  |

<a id="m-BusinessEntity"></a>
#### BusinessEntity

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `legalForm` | texte | oui | longueur min `1` |
| `niuNumber` | texte |  | longueur min `0`, longueur max `18` |
| `rccmNumber` | texte |  |  |
| `organizationType` | enum : `SOCIETE`, `ETABLISSEMENT`, `ENTREPRENEUR_INDIVIDUEL`, `PROFESSION_LIBERALE`, `ASSOCIATION`, `ONG`, `INSTITUTION_PUBLIQUE` |  |  |
| `legalRepresentativeName` | texte |  |  |
| `legalRepresentativeTitle` | texte |  |  |
| `vatRegime` | enum : `SUBJECT`, `EXEMPT` |  |  |
| `vatExemptionNote` | texte |  |  |
| `taxId` | texte |  |  |
| `activity` | texte |  |  |
| `address` | [Address](#m-Address) |  |  |
| `phone` | texte |  |  |
| `email` | string (email) |  |  |
| `baseCurrency` | [Currency](#m-Currency) |  |  |
| `status` | enum : `ACTIVE`, `INACTIVE`, `DELETED`, `BLOCKED` |  |  |
| `deleted` | booléen |  |  |
| `operator` | booléen |  |  |
| `created_by` | texte |  |  |
| `updated_by` | texte |  |  |
| `createdAt` | date-heure locale sans fuseau |  |  |
| `updatedAt` | date-heure locale sans fuseau |  |  |

<a id="m-SubscriptionPlan"></a>
#### SubscriptionPlan

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `planType` | enum : `MEMBERSHIP`, `COWORKING_ACCESS`, `DEDICATED_DESK`, `PRIVATE_OFFICE`, `MEETING_ROOM_PACK`, `VIRTUAL_OFFICE`, `COMPANY_PLAN`, `CUSTOM` |  |  |
| `targetAudience` | enum : `INDIVIDUAL`, `COMPANY`, `BOTH` |  |  |
| `status` | enum : `DRAFT`, `ACTIVE`, `ARCHIVED` |  |  |
| `visible` | booléen |  |  |
| `sortOrder` | entier |  |  |
| `requiredKycLevel` | entier |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `deleted` | booléen |  |  |

<a id="m-PassPlan"></a>
#### PassPlan

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `passType` | enum : `DAY_PASS`, `TIME_PACK`, `VISITOR_PASS`, `MEETING_ROOM_PACK`, `PROMOTIONAL_PASS`, `SUBSCRIPTION_PASS`, `COMPANY_SHARED_PASS`, `CUSTOM` |  |  |
| `targetAudience` | enum : `INDIVIDUAL`, `COMPANY`, `BOTH` |  |  |
| `status` | enum : `DRAFT`, `ACTIVE`, `ARCHIVED` |  |  |
| `visible` | booléen |  |  |
| `sortOrder` | entier |  |  |
| `requiredKycLevel` | entier |  |  |
| `deleted` | booléen |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-Users"></a>
#### Users

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `userId` | texte |  |  |
| `email` | texte |  |  |
| `firstname` | texte |  |  |
| `lastname` | texte |  |  |
| `passwordHash` | texte |  |  |
| `lastLogin` | date-heure locale sans fuseau |  |  |
| `createdAt` | date-heure locale sans fuseau |  |  |
| `updatedAt` | date-heure locale sans fuseau |  |  |
| `deleted` | booléen |  |  |
| `isAccountExpired` | booléen |  |  |
| `isAccountLocked` | booléen |  |  |
| `isAccountEnabled` | booléen |  |  |
| `failedLoginAttempts` | entier |  |  |
| `pendingEmail` | texte |  |  |
| `emailVerifiedAt` | date-heure ISO 8601 UTC |  |  |
| `emailVerifiedBy` | texte |  |  |
| `roleUsers` | [RoleUser](#m-RoleUser)[] |  |  |

<a id="m-Address"></a>
#### Address

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `streetNumber` | texte |  |  |
| `streetName` | texte |  |  |
| `district` | texte |  |  |
| `city` | texte |  |  |
| `country` | [Country](#m-Country) |  |  |
| `createdAt` | date-heure locale sans fuseau |  |  |
| `updatedAt` | date-heure locale sans fuseau |  |  |

<a id="m-Currency"></a>
#### Currency

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `currencyCode` | texte |  | longueur min `3`, longueur max `3` |
| `currencyName` | texte |  |  |
| `createdAt` | date-heure locale sans fuseau |  |  |
| `updatedAt` | date-heure locale sans fuseau |  |  |

<a id="m-RoleUser"></a>
#### RoleUser

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | [RoleUserId](#m-RoleUserId) |  |  |
| `role` | [Role](#m-Role) |  |  |
| `users` | [Users](#m-Users) |  |  |
| `assignedBy` | texte |  |  |
| `assignedAt` | date-heure locale sans fuseau |  |  |
| `updatedAt` | date-heure locale sans fuseau |  |  |

<a id="m-Country"></a>
#### Country

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `name` | texte |  |  |
| `countryCode` | texte |  | longueur min `2`, longueur max `3` |
| `defaultCurrency` | [Currency](#m-Currency) |  |  |
| `phoneCode` | texte |  |  |
| `isOhadaMember` | booléen |  |  |
| `deleted` | booléen |  |  |
| `createdAt` | date-heure locale sans fuseau |  |  |
| `updatedAt` | date-heure locale sans fuseau |  |  |

<a id="m-RoleUserId"></a>
#### RoleUserId

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `userId` | entier int64 ⚠ |  |  |
| `roleId` | entier int64 ⚠ |  |  |

<a id="m-Role"></a>
#### Role

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `name` | texte | oui | longueur min `0`, longueur max `250`, motif `^[A-Z_]+$` |
| `displayName` | texte |  |  |
| `description` | texte |  |  |
| `isSystemRole` | booléen |  |  |
| `version` | entier |  |  |
| `createdAt` | date-heure locale sans fuseau |  |  |
| `updatedAt` | date-heure locale sans fuseau |  |  |
| `isActive` | booléen |  |  |
| `rolePermissions` | [RolePermission](#m-RolePermission)[] |  |  |

<a id="m-RolePermission"></a>
#### RolePermission

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `role` | [Role](#m-Role) |  |  |
| `permission` | [Permission](#m-Permission) |  |  |
| `created_by` | texte |  |  |
| `isActive` | booléen |  |  |
| `id` | [RolePermissionId](#m-RolePermissionId) |  |  |

<a id="m-Permission"></a>
#### Permission

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `name` | texte | oui | longueur min `0`, longueur max `100`, motif `^[A-Z_]+$` |
| `displayName` | texte | oui | longueur min `0`, longueur max `100` |
| `module` | texte | oui | longueur min `0`, longueur max `50` |
| `action` | texte | oui | longueur min `0`, longueur max `60` |
| `isSystemPermission` | booléen |  |  |
| `isActive` | booléen |  |  |
| `fullPermissionName` | texte |  |  |

<a id="m-RolePermissionId"></a>
#### RolePermissionId

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `roleId` | entier int64 ⚠ |  |  |
| `permissionId` | entier int64 ⚠ |  |  |
