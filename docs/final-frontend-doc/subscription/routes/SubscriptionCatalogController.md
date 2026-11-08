# SubscriptionCatalogController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 14 route(s).

## Routes

<a id="r-get-sni-api-v1-subscriptions-entitlement-definitions"></a>
### GET /sni/api/v1/subscriptions/entitlement-definitions

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `PaginatedResponse<EntitlementDefinitionResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [EntitlementDefinitionResponse](#m-EntitlementDefinitionResponse), [EntitlementType](#m-EntitlementType), [EntitlementUnit](#m-EntitlementUnit), [ConsumptionMode](#m-ConsumptionMode), [EntitlementResetPolicy](#m-EntitlementResetPolicy)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionCatalogController.java`, méthode `listEntitlementDefinitions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` |  |  |
| `code` | requête | `String` |  |  |
| `name` | requête | `String` |  |  |
| `entitlementType` | requête | `EntitlementType` |  |  |
| `unit` | requête | `EntitlementUnit` |  |  |
| `consumptionMode` | requête | `ConsumptionMode` |  |  |
| `resetPolicy` | requête | `EntitlementResetPolicy` |  |  |
| `resourceTypeCode` | requête | `String` |  |  |
| `resourceGroupCode` | requête | `String` |  |  |
| `stackable` | requête | `Boolean` |  |  |
| `transferable` | requête | `Boolean` |  |  |
| `active` | requête | `Boolean` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-post-sni-api-v1-subscriptions-entitlement-definitions"></a>
### POST /sni/api/v1/subscriptions/entitlement-definitions

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE`
- Corps JSON (obligatoire) : `CreateEntitlementDefinitionRequest`
- Réponse : `EntitlementDefinitionResponse`
- Modèles : [EntitlementDefinitionResponse](#m-EntitlementDefinitionResponse), [CreateEntitlementDefinitionRequest](#m-CreateEntitlementDefinitionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionCatalogController.java`, méthode `createEntitlementDefinition`

<a id="r-get-sni-api-v1-subscriptions-entitlement-definitions-search-basic"></a>
### GET /sni/api/v1/subscriptions/entitlement-definitions/search/basic

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `List<EntitlementDefinitionResponse>`
- Modèles : [EntitlementDefinitionResponse](#m-EntitlementDefinitionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionCatalogController.java`, méthode `basicSearchEntitlementDefinitions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-subscriptions-entitlement-definitions-code"></a>
### GET /sni/api/v1/subscriptions/entitlement-definitions/{code}

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `EntitlementDefinitionResponse`
- Modèles : [EntitlementDefinitionResponse](#m-EntitlementDefinitionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionCatalogController.java`, méthode `getEntitlementDefinition`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-subscriptions-entitlement-definitions-code"></a>
### PUT /sni/api/v1/subscriptions/entitlement-definitions/{code}

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:UPDATE`
- Corps JSON (obligatoire) : `CreateEntitlementDefinitionRequest`
- Réponse : `EntitlementDefinitionResponse`
- Modèles : [EntitlementDefinitionResponse](#m-EntitlementDefinitionResponse), [CreateEntitlementDefinitionRequest](#m-CreateEntitlementDefinitionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionCatalogController.java`, méthode `updateEntitlementDefinition`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-subscriptions-plans"></a>
### GET /sni/api/v1/subscriptions/plans

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `PaginatedResponse<PlanResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [PlanResponse](#m-PlanResponse), [PlanType](#m-PlanType), [TargetAudience](#m-TargetAudience), [PlanStatus](#m-PlanStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionCatalogController.java`, méthode `listPlans`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | requête | `String` |  |  |
| `name` | requête | `String` |  |  |
| `planType` | requête | `PlanType` |  |  |
| `targetAudience` | requête | `TargetAudience` |  |  |
| `status` | requête | `PlanStatus` |  |  |
| `visible` | requête | `Boolean` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-post-sni-api-v1-subscriptions-plans"></a>
### POST /sni/api/v1/subscriptions/plans

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE`
- Corps JSON (obligatoire) : `CreatePlanRequest`
- Réponse : `PlanResponse`
- Modèles : [PlanResponse](#m-PlanResponse), [CreatePlanRequest](#m-CreatePlanRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionCatalogController.java`, méthode `createPlan`

<a id="r-delete-sni-api-v1-subscriptions-plans-plancode"></a>
### DELETE /sni/api/v1/subscriptions/plans/{planCode}

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:UPDATE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionCatalogController.java`, méthode `deletePlan`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `planCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-subscriptions-plans-plancode"></a>
### GET /sni/api/v1/subscriptions/plans/{planCode}

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `PlanResponse`
- Modèles : [PlanResponse](#m-PlanResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionCatalogController.java`, méthode `getPlan`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `planCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-subscriptions-plans-plancode"></a>
### PUT /sni/api/v1/subscriptions/plans/{planCode}

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:UPDATE`
- Corps JSON (obligatoire) : `UpdatePlanRequest`
- Réponse : `PlanResponse`
- Modèles : [PlanResponse](#m-PlanResponse), [UpdatePlanRequest](#m-UpdatePlanRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionCatalogController.java`, méthode `updatePlan`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `planCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-subscriptions-plans-plancode-versions"></a>
### GET /sni/api/v1/subscriptions/plans/{planCode}/versions

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `List<PlanVersionResponse>`
- Modèles : [PlanVersionResponse](#m-PlanVersionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionCatalogController.java`, méthode `listVersions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `planCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-plans-plancode-versions"></a>
### POST /sni/api/v1/subscriptions/plans/{planCode}/versions

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE`
- Corps JSON (obligatoire) : `CreatePlanVersionRequest`
- Réponse : `PlanVersionResponse`
- Modèles : [PlanVersionResponse](#m-PlanVersionResponse), [CreatePlanVersionRequest](#m-CreatePlanVersionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionCatalogController.java`, méthode `createVersion`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `planCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-subscriptions-plans-plancode-versions-versionid"></a>
### GET /sni/api/v1/subscriptions/plans/{planCode}/versions/{versionId}

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `PlanVersionResponse`
- Modèles : [PlanVersionResponse](#m-PlanVersionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionCatalogController.java`, méthode `getVersion`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `planCode` | chemin | `String` | oui |  |
| `versionId` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-subscriptions-plans-plancode-versions-versionid-publish"></a>
### PATCH /sni/api/v1/subscriptions/plans/{planCode}/versions/{versionId}/publish

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:UPDATE`
- Réponse : `PlanVersionResponse`
- Modèles : [PlanVersionResponse](#m-PlanVersionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionCatalogController.java`, méthode `publishVersion`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `planCode` | chemin | `String` | oui |  |
| `versionId` | chemin | `Long` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-EntitlementDefinitionResponse"></a>
#### EntitlementDefinitionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `entitlementType` | enum : `ACCESS`, `CREDIT`, `TIME`, `DISCOUNT`, `QUOTA`, `FEATURE_FLAG`, `SERVICE_ALLOWANCE` |  |  |
| `unit` | enum : `DAY`, `HOUR`, `CREDIT`, `BOOKING`, `VISIT`, `MEMBER`, `PERCENT`, `AMOUNT`, `BOOLEAN` |  |  |
| `consumptionMode` | enum : `CHECK_ONLY`, `RESERVABLE`, `CONSUMABLE`, `RESERVE_THEN_CONSUME` |  |  |
| `resetPolicy` | enum : `NEVER`, `DAILY`, `WEEKLY`, `MONTHLY`, `PER_BILLING_CYCLE` |  |  |
| `stackable` | booléen |  |  |
| `transferable` | booléen |  |  |
| `resourceTypeCode` | texte |  |  |
| `resourceGroupCode` | texte |  |  |
| `metadataJson` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-EntitlementType"></a>
#### EntitlementType

Énumération : `ACCESS`, `CREDIT`, `TIME`, `DISCOUNT`, `QUOTA`, `FEATURE_FLAG`, `SERVICE_ALLOWANCE`

<a id="m-EntitlementUnit"></a>
#### EntitlementUnit

Énumération : `DAY`, `HOUR`, `CREDIT`, `BOOKING`, `VISIT`, `MEMBER`, `PERCENT`, `AMOUNT`, `BOOLEAN`

<a id="m-ConsumptionMode"></a>
#### ConsumptionMode

Énumération : `CHECK_ONLY`, `RESERVABLE`, `CONSUMABLE`, `RESERVE_THEN_CONSUME`

<a id="m-EntitlementResetPolicy"></a>
#### EntitlementResetPolicy

Énumération : `NEVER`, `DAILY`, `WEEKLY`, `MONTHLY`, `PER_BILLING_CYCLE`

<a id="m-CreateEntitlementDefinitionRequest"></a>
#### CreateEntitlementDefinitionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `entitlementType` | enum : `ACCESS`, `CREDIT`, `TIME`, `DISCOUNT`, `QUOTA`, `FEATURE_FLAG`, `SERVICE_ALLOWANCE` | oui |  |
| `unit` | enum : `DAY`, `HOUR`, `CREDIT`, `BOOKING`, `VISIT`, `MEMBER`, `PERCENT`, `AMOUNT`, `BOOLEAN` | oui |  |
| `consumptionMode` | enum : `CHECK_ONLY`, `RESERVABLE`, `CONSUMABLE`, `RESERVE_THEN_CONSUME` | oui |  |
| `resetPolicy` | enum : `NEVER`, `DAILY`, `WEEKLY`, `MONTHLY`, `PER_BILLING_CYCLE` | oui |  |
| `stackable` | booléen |  |  |
| `transferable` | booléen |  |  |
| `resourceTypeCode` | texte |  |  |
| `resourceGroupCode` | texte |  |  |
| `metadataJson` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-PlanResponse"></a>
#### PlanResponse

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
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `activeVersion` | [PlanVersionResponse](#m-PlanVersionResponse) |  |  |
| `versions` | [PlanVersionResponse](#m-PlanVersionResponse)[] |  |  |

<a id="m-PlanType"></a>
#### PlanType

Énumération : `MEMBERSHIP`, `COWORKING_ACCESS`, `DEDICATED_DESK`, `PRIVATE_OFFICE`, `MEETING_ROOM_PACK`, `VIRTUAL_OFFICE`, `COMPANY_PLAN`, `CUSTOM`

<a id="m-TargetAudience"></a>
#### TargetAudience

Énumération : `INDIVIDUAL`, `COMPANY`, `BOTH`

<a id="m-PlanStatus"></a>
#### PlanStatus

Énumération : `DRAFT`, `ACTIVE`, `ARCHIVED`

<a id="m-CreatePlanRequest"></a>
#### CreatePlanRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `planType` | enum : `MEMBERSHIP`, `COWORKING_ACCESS`, `DEDICATED_DESK`, `PRIVATE_OFFICE`, `MEETING_ROOM_PACK`, `VIRTUAL_OFFICE`, `COMPANY_PLAN`, `CUSTOM` | oui |  |
| `targetAudience` | enum : `INDIVIDUAL`, `COMPANY`, `BOTH` | oui |  |
| `visible` | booléen |  |  |
| `sortOrder` | entier |  |  |

<a id="m-UpdatePlanRequest"></a>
#### UpdatePlanRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `planType` | enum : `MEMBERSHIP`, `COWORKING_ACCESS`, `DEDICATED_DESK`, `PRIVATE_OFFICE`, `MEETING_ROOM_PACK`, `VIRTUAL_OFFICE`, `COMPANY_PLAN`, `CUSTOM` |  |  |
| `targetAudience` | enum : `INDIVIDUAL`, `COMPANY`, `BOTH` |  |  |
| `visible` | booléen |  |  |
| `sortOrder` | entier |  |  |

<a id="m-PlanVersionResponse"></a>
#### PlanVersionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | texte |  |  |
| `versionNumber` | entier |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `status` | enum : `DRAFT`, `ACTIVE`, `ARCHIVED` |  |  |
| `effectiveFrom` | date AAAA-MM-JJ |  |  |
| `effectiveTo` | date AAAA-MM-JJ |  |  |
| `termsJson` | texte |  |  |
| `prices` | [PlanPriceResponse](#m-PlanPriceResponse)[] |  |  |
| `benefits` | [PlanBenefitResponse](#m-PlanBenefitResponse)[] |  |  |
| `entitlements` | [PlanEntitlementResponse](#m-PlanEntitlementResponse)[] |  |  |

<a id="m-CreatePlanVersionRequest"></a>
#### CreatePlanVersionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `effectiveFrom` | date AAAA-MM-JJ |  |  |
| `effectiveTo` | date AAAA-MM-JJ |  |  |
| `termsJson` | texte |  |  |
| `prices` | [PlanPriceRequest](#m-PlanPriceRequest)[] |  |  |
| `benefits` | [PlanBenefitRequest](#m-PlanBenefitRequest)[] |  |  |
| `entitlements` | [PlanEntitlementRequest](#m-PlanEntitlementRequest)[] |  |  |

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

<a id="m-PlanPriceRequest"></a>
#### PlanPriceRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `billingCycle` | enum : `ONE_TIME`, `DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `YEARLY` | oui |  |
| `currency` | texte | oui | longueur min `1` |
| `amount` | nombre décimal | oui | min `0.0` |
| `setupFee` | nombre décimal |  |  |
| `depositAmount` | nombre décimal |  |  |
| `taxIncluded` | booléen |  |  |
| `commitmentMonths` | entier |  |  |

<a id="m-PlanBenefitRequest"></a>
#### PlanBenefitRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `title` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `icon` | texte |  |  |
| `category` | enum : `ACCESS`, `BOOKING`, `COMMUNITY`, `BUSINESS_SERVICE`, `SERVICE`, `FINANCIAL`, `SUPPORT`, `DISCOUNT`, `INVENTORY`, `OTHER`, `FLEXIBILITY`, `SECURITY`, `COMFORT`, `CUSTOM` |  |  |
| `displayOrder` | entier |  |  |
| `highlighted` | booléen |  |  |
| `included` | booléen |  |  |
| `metadataJson` | texte |  |  |

<a id="m-PlanEntitlementRequest"></a>
#### PlanEntitlementRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `entitlementCode` | texte | oui | longueur min `1` |
| `quantity` | nombre décimal |  |  |
| `unlimited` | booléen |  |  |
| `rolloverAllowed` | booléen |  |  |
| `rolloverLimit` | nombre décimal |  |  |
| `validForDays` | entier |  |  |
| `priority` | entier |  |  |
| `restrictionsJson` | texte |  |  |
