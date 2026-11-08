# ClientCompanyPassController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [portal](../README.md) · 6 route(s).

## Routes

<a id="r-get-sni-api-v1-client-companies-customerid-passes"></a>
### GET /sni/api/v1/client/companies/{customerId}/passes

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<PassSharingDtos.PassSummary>`
- Modèles : [PassSharingDtos.PassSummary](#m-PassSharingDtos-PassSummary)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyPassController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-companies-customerid-passes-purchase"></a>
### POST /sni/api/v1/client/companies/{customerId}/passes/purchase

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `ClientPurchasePassRequest`
- Réponse : `PassResponse`
- Modèles : [PassResponse](#m-PassResponse), [ClientPurchasePassRequest](#m-ClientPurchasePassRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyPassController.java`, méthode `purchase`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-companies-customerid-passes-passnumber-beneficiaries"></a>
### GET /sni/api/v1/client/companies/{customerId}/passes/{passNumber}/beneficiaries

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `PassSharingDtos.PassView`
- Modèles : [PassSharingDtos.PassView](#m-PassSharingDtos-PassView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyPassController.java`, méthode `view`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `passNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-companies-customerid-passes-passnumber-beneficiaries"></a>
### POST /sni/api/v1/client/companies/{customerId}/passes/{passNumber}/beneficiaries

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `PassSharingDtos.AddBeneficiaryRequest`
- Réponse : `PassSharingDtos.PassView`
- Modèles : [PassSharingDtos.PassView](#m-PassSharingDtos-PassView), [PassSharingDtos.AddBeneficiaryRequest](#m-PassSharingDtos-AddBeneficiaryRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyPassController.java`, méthode `add`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `passNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-companies-customerid-passes-passnumber-beneficiaries-accessid-revoke"></a>
### POST /sni/api/v1/client/companies/{customerId}/passes/{passNumber}/beneficiaries/{accessId}/revoke

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (facultatif) : `PassSharingDtos.RevokeBeneficiaryRequest`
- Réponse : `PassSharingDtos.RevokeResult`
- Modèles : [PassSharingDtos.RevokeResult](#m-PassSharingDtos-RevokeResult), [PassSharingDtos.RevokeBeneficiaryRequest](#m-PassSharingDtos-RevokeBeneficiaryRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyPassController.java`, méthode `revoke`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `passNumber` | chemin | `String` | oui |  |
| `accessId` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-client-me-company-passes"></a>
### GET /sni/api/v1/client/me/company-passes

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<PassSharingDtos.MyPassAccess>`
- Modèles : [PassSharingDtos.MyPassAccess](#m-PassSharingDtos-MyPassAccess)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanyPassController.java`, méthode `myAccesses`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-PassSharingDtos-PassSummary"></a>
#### PassSharingDtos.PassSummary

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `passNumber` | texte |  |  |
| `name` | texte |  |  |
| `passType` | texte |  |  |
| `status` | texte |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `maxUses` | entier |  |  |
| `usedCount` | entier |  |  |
| `beneficiaries` | entier int64 ⚠ |  |  |

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

<a id="m-PassSharingDtos-PassView"></a>
#### PassSharingDtos.PassView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `companyCustomerCode` | texte |  |  |
| `pass` | [PassSharingDtos.PassSummary](#m-PassSharingDtos-PassSummary) |  |  |
| `entitlements` | [PassSharingDtos.PassEntitlement](#m-PassSharingDtos-PassEntitlement)[] |  |  |
| `beneficiaries` | [PassSharingDtos.BeneficiaryView](#m-PassSharingDtos-BeneficiaryView)[] |  |  |

<a id="m-PassSharingDtos-AddBeneficiaryRequest"></a>
#### PassSharingDtos.AddBeneficiaryRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `memberId` | texte | oui | longueur min `1` |
| `maxUses` | entier |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |

<a id="m-PassSharingDtos-RevokeResult"></a>
#### PassSharingDtos.RevokeResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `memberId` | texte |  |  |
| `futureBookings` | texte[] |  |  |
| `warning` | texte |  |  |

<a id="m-PassSharingDtos-RevokeBeneficiaryRequest"></a>
#### PassSharingDtos.RevokeBeneficiaryRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  | longueur min `0`, longueur max `255` |

<a id="m-PassSharingDtos-MyPassAccess"></a>
#### PassSharingDtos.MyPassAccess

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `companyCustomerCode` | texte |  |  |
| `companyName` | texte |  |  |
| `passNumber` | texte |  |  |
| `passName` | texte |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `maxUses` | entier |  |  |
| `usedCount` | entier |  |  |
| `entitlements` | [PassSharingDtos.PassEntitlement](#m-PassSharingDtos-PassEntitlement)[] |  |  |

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

<a id="m-PassSharingDtos-PassEntitlement"></a>
#### PassSharingDtos.PassEntitlement

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `entitlementCode` | texte |  |  |
| `name` | texte |  |  |
| `unit` | texte |  |  |
| `unlimited` | booléen |  |  |
| `granted` | nombre décimal |  |  |
| `remaining` | nombre décimal |  |  |

<a id="m-PassSharingDtos-BeneficiaryView"></a>
#### PassSharingDtos.BeneficiaryView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `memberId` | texte |  |  |
| `memberName` | texte |  |  |
| `open` | booléen |  |  |
| `maxUses` | entier |  |  |
| `usedCount` | entier |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `addedAt` | date-heure ISO 8601 UTC |  |  |
| `addedBy` | texte |  |  |
| `revokedAt` | date-heure ISO 8601 UTC |  |  |
| `revokedReason` | texte |  |  |
| `consumption` | [PassSharingDtos.BeneficiaryConsumption](#m-PassSharingDtos-BeneficiaryConsumption)[] |  |  |

<a id="m-PassSharingDtos-BeneficiaryConsumption"></a>
#### PassSharingDtos.BeneficiaryConsumption

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `entitlementCode` | texte |  |  |
| `consumed` | nombre décimal |  |  |
