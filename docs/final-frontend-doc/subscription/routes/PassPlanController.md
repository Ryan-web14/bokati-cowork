# PassPlanController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 15 route(s).

## Routes

<a id="r-get-sni-api-v1-pass-plans"></a>
### GET /sni/api/v1/pass-plans

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('PASS_PLAN:READ','PASS_PLAN_READ')`
- Réponse : `PaginatedResponse<PassPlanDtos.PassPlanResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [PassPlanDtos.PassPlanResponse](#m-PassPlanDtos-PassPlanResponse), [PassType](#m-PassType), [PlanStatus](#m-PlanStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassPlanController.java`, méthode `searchPlans`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `passType` | requête | `PassType` |  |  |
| `status` | requête | `PlanStatus` |  |  |
| `searchText` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-post-sni-api-v1-pass-plans"></a>
### POST /sni/api/v1/pass-plans

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('PASS_PLAN:WRITE','PASS_PLAN_WRITE')`
- Corps JSON (obligatoire) : `PassPlanDtos.CreatePassPlanRequest`
- Réponse : `PassPlanDtos.PassPlanResponse`
- Modèles : [PassPlanDtos.PassPlanResponse](#m-PassPlanDtos-PassPlanResponse), [PassPlanDtos.CreatePassPlanRequest](#m-PassPlanDtos-CreatePassPlanRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassPlanController.java`, méthode `createPlan`

<a id="r-get-sni-api-v1-pass-plans-versions-versioncode-entitlements"></a>
### GET /sni/api/v1/pass-plans/versions/{versionCode}/entitlements

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('PASS_PLAN:READ','PASS_PLAN_READ')`
- Réponse : `List<PassPlanDtos.PassPlanEntitlementResponse>`
- Modèles : [PassPlanDtos.PassPlanEntitlementResponse](#m-PassPlanDtos-PassPlanEntitlementResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassPlanController.java`, méthode `listEntitlements`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `versionCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-pass-plans-versions-versioncode-entitlements"></a>
### POST /sni/api/v1/pass-plans/versions/{versionCode}/entitlements

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('PASS_PLAN:WRITE','PASS_PLAN_WRITE')`
- Corps JSON (obligatoire) : `PassPlanDtos.AddPassPlanEntitlementRequest`
- Réponse : `PassPlanDtos.PassPlanEntitlementResponse`
- Modèles : [PassPlanDtos.PassPlanEntitlementResponse](#m-PassPlanDtos-PassPlanEntitlementResponse), [PassPlanDtos.AddPassPlanEntitlementRequest](#m-PassPlanDtos-AddPassPlanEntitlementRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassPlanController.java`, méthode `addEntitlement`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `versionCode` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-pass-plans-versions-versioncode-entitlements-entitlementcode"></a>
### DELETE /sni/api/v1/pass-plans/versions/{versionCode}/entitlements/{entitlementCode}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('PASS_PLAN:WRITE','PASS_PLAN_WRITE')`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassPlanController.java`, méthode `removeEntitlement`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `versionCode` | chemin | `String` | oui |  |
| `entitlementCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-pass-plans-versions-versioncode-prices"></a>
### GET /sni/api/v1/pass-plans/versions/{versionCode}/prices

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('PASS_PLAN:READ','PASS_PLAN_READ')`
- Réponse : `List<PassPlanDtos.PassPlanPriceResponse>`
- Modèles : [PassPlanDtos.PassPlanPriceResponse](#m-PassPlanDtos-PassPlanPriceResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassPlanController.java`, méthode `listPrices`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `versionCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-pass-plans-versions-versioncode-prices"></a>
### POST /sni/api/v1/pass-plans/versions/{versionCode}/prices

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('PASS_PLAN:WRITE','PASS_PLAN_WRITE')`
- Corps JSON (obligatoire) : `PassPlanDtos.SetPassPlanPriceRequest`
- Réponse : `PassPlanDtos.PassPlanPriceResponse`
- Modèles : [PassPlanDtos.PassPlanPriceResponse](#m-PassPlanDtos-PassPlanPriceResponse), [PassPlanDtos.SetPassPlanPriceRequest](#m-PassPlanDtos-SetPassPlanPriceRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassPlanController.java`, méthode `setPrice`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `versionCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-pass-plans-plancode"></a>
### GET /sni/api/v1/pass-plans/{planCode}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('PASS_PLAN:READ','PASS_PLAN_READ')`
- Réponse : `PassPlanDtos.PassPlanResponse`
- Modèles : [PassPlanDtos.PassPlanResponse](#m-PassPlanDtos-PassPlanResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassPlanController.java`, méthode `getPlan`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `planCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-pass-plans-plancode"></a>
### PATCH /sni/api/v1/pass-plans/{planCode}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('PASS_PLAN:WRITE','PASS_PLAN_WRITE')`
- Corps JSON (obligatoire) : `PassPlanDtos.UpdatePassPlanRequest`
- Réponse : `PassPlanDtos.PassPlanResponse`
- Modèles : [PassPlanDtos.PassPlanResponse](#m-PassPlanDtos-PassPlanResponse), [PassPlanDtos.UpdatePassPlanRequest](#m-PassPlanDtos-UpdatePassPlanRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassPlanController.java`, méthode `updatePlan`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `planCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-pass-plans-plancode-archive"></a>
### PATCH /sni/api/v1/pass-plans/{planCode}/archive

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('PASS_PLAN:WRITE','PASS_PLAN_WRITE')`
- Réponse : `PassPlanDtos.PassPlanResponse`
- Modèles : [PassPlanDtos.PassPlanResponse](#m-PassPlanDtos-PassPlanResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassPlanController.java`, méthode `archivePlan`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `planCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-pass-plans-plancode-publish"></a>
### PATCH /sni/api/v1/pass-plans/{planCode}/publish

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('PASS_PLAN:WRITE','PASS_PLAN_WRITE')`
- Réponse : `PassPlanDtos.PassPlanResponse`
- Modèles : [PassPlanDtos.PassPlanResponse](#m-PassPlanDtos-PassPlanResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassPlanController.java`, méthode `publishPlan`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `planCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-pass-plans-plancode-purchase"></a>
### POST /sni/api/v1/pass-plans/{planCode}/purchase

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('PASS:WRITE','PASS_WRITE')`
- Corps JSON (obligatoire) : `CreatePassPurchaseRequest`
- Réponse : `PassResponse`
- Modèles : [PassResponse](#m-PassResponse), [CreatePassPurchaseRequest](#m-CreatePassPurchaseRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassPlanController.java`, méthode `purchase`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `planCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-pass-plans-plancode-versions"></a>
### GET /sni/api/v1/pass-plans/{planCode}/versions

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('PASS_PLAN:READ','PASS_PLAN_READ')`
- Réponse : `List<PassPlanDtos.PassPlanVersionResponse>`
- Modèles : [PassPlanDtos.PassPlanVersionResponse](#m-PassPlanDtos-PassPlanVersionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassPlanController.java`, méthode `listVersions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `planCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-pass-plans-plancode-versions"></a>
### POST /sni/api/v1/pass-plans/{planCode}/versions

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('PASS_PLAN:WRITE','PASS_PLAN_WRITE')`
- Corps JSON (obligatoire) : `PassPlanDtos.CreatePassPlanVersionRequest`
- Réponse : `PassPlanDtos.PassPlanVersionResponse`
- Modèles : [PassPlanDtos.PassPlanVersionResponse](#m-PassPlanDtos-PassPlanVersionResponse), [PassPlanDtos.CreatePassPlanVersionRequest](#m-PassPlanDtos-CreatePassPlanVersionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassPlanController.java`, méthode `createVersion`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `planCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-pass-plans-plancode-versions-versioncode-publish"></a>
### PATCH /sni/api/v1/pass-plans/{planCode}/versions/{versionCode}/publish

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('PASS_PLAN:WRITE','PASS_PLAN_WRITE')`
- Réponse : `PassPlanDtos.PassPlanVersionResponse`
- Modèles : [PassPlanDtos.PassPlanVersionResponse](#m-PassPlanDtos-PassPlanVersionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassPlanController.java`, méthode `publishVersion`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `planCode` | chemin | `String` | oui |  |
| `versionCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-PassPlanDtos-PassPlanResponse"></a>
#### PassPlanDtos.PassPlanResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | texte |  |  |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `passType` | enum : `DAY_PASS`, `TIME_PACK`, `VISITOR_PASS`, `MEETING_ROOM_PACK`, `PROMOTIONAL_PASS`, `SUBSCRIPTION_PASS`, `COMPANY_SHARED_PASS`, `CUSTOM` |  |  |
| `targetAudience` | enum : `INDIVIDUAL`, `COMPANY`, `BOTH` |  |  |
| `status` | enum : `DRAFT`, `ACTIVE`, `ARCHIVED` |  |  |
| `visible` | booléen |  |  |
| `sortOrder` | entier |  |  |
| `requiredKycLevel` | entier |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-PassType"></a>
#### PassType

Énumération : `DAY_PASS`, `TIME_PACK`, `VISITOR_PASS`, `MEETING_ROOM_PACK`, `PROMOTIONAL_PASS`, `SUBSCRIPTION_PASS`, `COMPANY_SHARED_PASS`, `CUSTOM`

<a id="m-PlanStatus"></a>
#### PlanStatus

Énumération : `DRAFT`, `ACTIVE`, `ARCHIVED`

<a id="m-PassPlanDtos-CreatePassPlanRequest"></a>
#### PassPlanDtos.CreatePassPlanRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `passType` | enum : `DAY_PASS`, `TIME_PACK`, `VISITOR_PASS`, `MEETING_ROOM_PACK`, `PROMOTIONAL_PASS`, `SUBSCRIPTION_PASS`, `COMPANY_SHARED_PASS`, `CUSTOM` | oui |  |
| `targetAudience` | enum : `INDIVIDUAL`, `COMPANY`, `BOTH` | oui |  |
| `visible` | booléen |  |  |
| `sortOrder` | entier |  |  |
| `requiredKycLevel` | entier |  |  |

<a id="m-PassPlanDtos-PassPlanEntitlementResponse"></a>
#### PassPlanDtos.PassPlanEntitlementResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | texte |  |  |
| `versionCode` | texte |  |  |
| `entitlementCode` | texte |  |  |
| `entitlementName` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `unlimited` | booléen |  |  |
| `rolloverAllowed` | booléen |  |  |
| `rolloverLimit` | nombre décimal |  |  |
| `validForDays` | entier |  |  |
| `priority` | entier |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-PassPlanDtos-AddPassPlanEntitlementRequest"></a>
#### PassPlanDtos.AddPassPlanEntitlementRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `entitlementCode` | texte | oui | longueur min `1` |
| `quantity` | nombre décimal |  |  |
| `unlimited` | booléen |  |  |
| `rolloverAllowed` | booléen |  |  |
| `rolloverLimit` | nombre décimal |  |  |
| `validForDays` | entier |  |  |
| `priority` | entier |  |  |

<a id="m-PassPlanDtos-PassPlanPriceResponse"></a>
#### PassPlanDtos.PassPlanPriceResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | texte |  |  |
| `versionCode` | texte |  |  |
| `currency` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `setupFee` | nombre décimal |  |  |
| `depositAmount` | nombre décimal |  |  |
| `taxIncluded` | booléen |  |  |
| `taxCode` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-PassPlanDtos-SetPassPlanPriceRequest"></a>
#### PassPlanDtos.SetPassPlanPriceRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `currency` | texte | oui | longueur min `1` |
| `amount` | nombre décimal | oui |  |
| `setupFee` | nombre décimal |  |  |
| `depositAmount` | nombre décimal |  |  |
| `taxIncluded` | booléen |  |  |
| `taxCode` | texte |  |  |

<a id="m-PassPlanDtos-UpdatePassPlanRequest"></a>
#### PassPlanDtos.UpdatePassPlanRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `visible` | booléen |  |  |
| `sortOrder` | entier |  |  |
| `requiredKycLevel` | entier |  |  |

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

<a id="m-CreatePassPurchaseRequest"></a>
#### CreatePassPurchaseRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ownerType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` | oui |  |
| `ownerCode` | texte | oui | longueur min `1` |
| `planVersionId` | texte |  |  |
| `currency` | texte |  |  |
| `validFrom` | texte |  |  |
| `autoRenew` | booléen |  |  |
| `metadataJson` | texte |  |  |
| `idempotencyKey` | texte |  |  |

<a id="m-PassPlanDtos-PassPlanVersionResponse"></a>
#### PassPlanDtos.PassPlanVersionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | texte |  |  |
| `versionCode` | texte |  |  |
| `planCode` | texte |  |  |
| `versionNumber` | entier |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `status` | enum : `DRAFT`, `ACTIVE`, `ARCHIVED` |  |  |
| `duration` | entier |  |  |
| `durationUnit` | enum : `DAY`, `WEEK`, `MONTH`, `YEAR` |  |  |
| `maxUses` | entier |  |  |
| `autoRenewable` | booléen |  |  |
| `requiredKycLevel` | entier |  |  |
| `effectiveFrom` | date AAAA-MM-JJ |  |  |
| `effectiveTo` | date AAAA-MM-JJ |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-PassPlanDtos-CreatePassPlanVersionRequest"></a>
#### PassPlanDtos.CreatePassPlanVersionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `duration` | entier | oui |  |
| `durationUnit` | enum : `DAY`, `WEEK`, `MONTH`, `YEAR` | oui |  |
| `maxUses` | entier |  |  |
| `autoRenewable` | booléen |  |  |
| `requiredKycLevel` | entier |  |  |
| `effectiveFrom` | date AAAA-MM-JJ |  |  |
| `effectiveTo` | date AAAA-MM-JJ |  |  |
| `termsJson` | texte |  |  |

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
