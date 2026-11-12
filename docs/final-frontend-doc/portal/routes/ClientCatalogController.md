# ClientCatalogController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [portal](../README.md) · 4 route(s).

## Routes

<a id="r-post-sni-api-v1-client-catalog-passes-purchase"></a>
### POST /sni/api/v1/client/catalog/passes/purchase

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `ClientPurchasePassRequest`
- Réponse : `PassResponse`
- Modèles : [PassResponse](#m-PassResponse), [ClientPurchasePassRequest](#m-ClientPurchasePassRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/subscription/controller/ClientCatalogController.java`, méthode `purchasePass`

<a id="r-get-sni-api-v1-client-catalog-plans"></a>
### GET /sni/api/v1/client/catalog/plans

- Accès : public, sans jeton
- Réponse : `PaginatedResponse<ClientPlanSummaryResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ClientPlanSummaryResponse](#m-ClientPlanSummaryResponse), [PlanType](#m-PlanType)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/subscription/controller/ClientCatalogController.java`, méthode `listAvailablePlans`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `planType` | requête | `PlanType` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=sortOrder,ASC |

<a id="r-get-sni-api-v1-client-catalog-plans-plancode"></a>
### GET /sni/api/v1/client/catalog/plans/{planCode}

- Accès : public, sans jeton
- Réponse : `ClientPlanDetailResponse`
- Modèles : [ClientPlanDetailResponse](#m-ClientPlanDetailResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/subscription/controller/ClientCatalogController.java`, méthode `getPlanDetail`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `planCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-catalog-subscribe"></a>
### POST /sni/api/v1/client/catalog/subscribe

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `ClientSubscribeRequest`
- Réponse : `SubscriptionResponse`
- Modèles : [SubscriptionResponse](#m-SubscriptionResponse), [ClientSubscribeRequest](#m-ClientSubscribeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/subscription/controller/ClientCatalogController.java`, méthode `subscribe`

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

<a id="m-ClientPurchasePassRequest"></a>
#### ClientPurchasePassRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `planCode` | texte | oui | longueur min `1` |
| `passType` | enum : `DAY_PASS`, `TIME_PACK`, `VISITOR_PASS`, `MEETING_ROOM_PACK`, `PROMOTIONAL_PASS`, `SUBSCRIPTION_PASS`, `COMPANY_SHARED_PASS`, `CUSTOM` | oui |  |
| `name` | texte | oui | longueur min `1` |
| `idempotencyKey` | texte |  |  |

<a id="m-ClientPlanSummaryResponse"></a>
#### ClientPlanSummaryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `planType` | enum : `MEMBERSHIP`, `COWORKING_ACCESS`, `DEDICATED_DESK`, `PRIVATE_OFFICE`, `MEETING_ROOM_PACK`, `VIRTUAL_OFFICE`, `COMPANY_PLAN`, `CUSTOM` |  |  |
| `targetAudience` | enum : `INDIVIDUAL`, `COMPANY`, `BOTH` |  |  |
| `sortOrder` | entier |  |  |
| `startingPrice` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `highlightedBenefits` | [ClientPlanSummaryResponse.HighlightedBenefit](#m-ClientPlanSummaryResponse-HighlightedBenefit)[] |  |  |

<a id="m-PlanType"></a>
#### PlanType

Énumération : `MEMBERSHIP`, `COWORKING_ACCESS`, `DEDICATED_DESK`, `PRIVATE_OFFICE`, `MEETING_ROOM_PACK`, `VIRTUAL_OFFICE`, `COMPANY_PLAN`, `CUSTOM`

<a id="m-ClientPlanDetailResponse"></a>
#### ClientPlanDetailResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `planType` | enum : `MEMBERSHIP`, `COWORKING_ACCESS`, `DEDICATED_DESK`, `PRIVATE_OFFICE`, `MEETING_ROOM_PACK`, `VIRTUAL_OFFICE`, `COMPANY_PLAN`, `CUSTOM` |  |  |
| `targetAudience` | enum : `INDIVIDUAL`, `COMPANY`, `BOTH` |  |  |
| `sortOrder` | entier |  |  |
| `requiredKycLevel` | entier |  |  |
| `termsJson` | texte |  |  |
| `prices` | [PlanPriceResponse](#m-PlanPriceResponse)[] |  |  |
| `benefits` | [PlanBenefitResponse](#m-PlanBenefitResponse)[] |  |  |
| `entitlements` | [PlanEntitlementResponse](#m-PlanEntitlementResponse)[] |  |  |

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

<a id="m-ClientSubscribeRequest"></a>
#### ClientSubscribeRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `planCode` | texte | oui | longueur min `1` |
| `billingCycle` | enum : `ONE_TIME`, `DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `YEARLY` | oui |  |
| `autoRenew` | booléen |  |  |
| `couponCodes` | texte[] |  |  |

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

<a id="m-ClientPlanSummaryResponse-HighlightedBenefit"></a>
#### ClientPlanSummaryResponse.HighlightedBenefit

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `icon` | texte |  |  |

<a id="m-PlanPriceResponse"></a>
#### PlanPriceResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `billingCycle` | enum : `ONE_TIME`, `DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `YEARLY` |  |  |
| `currency` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `setupFee` | nombre décimal |  |  |
| `depositAmount` | nombre décimal |  |  |
| `taxIncluded` | booléen |  |  |
| `trialDays` | entier |  |  |
| `commitmentMonths` | entier |  |  |
| `taxCode` | texte |  |  |

<a id="m-PlanBenefitResponse"></a>
#### PlanBenefitResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `icon` | texte |  |  |
| `category` | enum : `ACCESS`, `BOOKING`, `COMMUNITY`, `BUSINESS_SERVICE`, `SERVICE`, `FINANCIAL`, `SUPPORT`, `DISCOUNT`, `INVENTORY`, `OTHER`, `FLEXIBILITY`, `SECURITY`, `COMFORT`, `CUSTOM` |  |  |
| `displayOrder` | entier |  |  |
| `highlighted` | booléen |  |  |
| `included` | booléen |  |  |
| `metadataJson` | texte |  |  |

<a id="m-PlanEntitlementResponse"></a>
#### PlanEntitlementResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `entitlementCode` | texte |  |  |
| `entitlementName` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `unlimited` | booléen |  |  |
| `rolloverAllowed` | booléen |  |  |
| `rolloverLimit` | nombre décimal |  |  |
| `validForDays` | entier |  |  |
| `priority` | entier |  |  |
| `restrictionsJson` | texte |  |  |
