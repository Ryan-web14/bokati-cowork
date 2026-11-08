# UsageRecordController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 2 route(s).

## Routes

<a id="r-get-sni-api-v1-usage-records"></a>
### GET /sni/api/v1/usage-records

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `PaginatedResponse<UsageRecordResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [UsageRecordResponse](#m-UsageRecordResponse), [SubscriberType](#m-SubscriberType), [UsageRecordStatus](#m-UsageRecordStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/usage/controller/UsageRecordController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ownerType` | requête | `SubscriberType` |  |  |
| `ownerCode` | requête | `String` |  |  |
| `entitlementCode` | requête | `String` |  |  |
| `referenceType` | requête | `String` |  |  |
| `referenceId` | requête | `String` |  |  |
| `status` | requête | `UsageRecordStatus` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=occurredAt,DESC |

<a id="r-post-sni-api-v1-usage-records"></a>
### POST /sni/api/v1/usage-records

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE`
- Corps JSON (obligatoire) : `CreateUsageRecordRequest`
- Réponse : `UsageRecordResponse`
- Modèles : [UsageRecordResponse](#m-UsageRecordResponse), [CreateUsageRecordRequest](#m-CreateUsageRecordRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/usage/controller/UsageRecordController.java`, méthode `record`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-UsageRecordResponse"></a>
#### UsageRecordResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `usageNumber` | texte |  |  |
| `ownerType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `ownerCode` | texte |  |  |
| `entitlementCode` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `unit` | enum : `DAY`, `HOUR`, `CREDIT`, `BOOKING`, `VISIT`, `MEMBER`, `PERCENT`, `AMOUNT`, `BOOLEAN` |  |  |
| `referenceType` | texte |  |  |
| `referenceId` | texte |  |  |
| `billable` | booléen |  |  |
| `billableNumber` | texte |  |  |
| `status` | enum : `RECORDED`, `BILLED`, `CANCELLED` |  |  |
| `occurredAt` | date-heure ISO 8601 UTC |  |  |
| `metadataJson` | texte |  |  |

<a id="m-SubscriberType"></a>
#### SubscriberType

Énumération : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY`

<a id="m-UsageRecordStatus"></a>
#### UsageRecordStatus

Énumération : `RECORDED`, `BILLED`, `CANCELLED`

<a id="m-CreateUsageRecordRequest"></a>
#### CreateUsageRecordRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ownerType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` | oui |  |
| `ownerCode` | texte | oui | longueur min `1` |
| `entitlementCode` | texte | oui | longueur min `1` |
| `quantity` | nombre décimal | oui |  |
| `unit` | enum : `DAY`, `HOUR`, `CREDIT`, `BOOKING`, `VISIT`, `MEMBER`, `PERCENT`, `AMOUNT`, `BOOLEAN` | oui |  |
| `referenceType` | texte | oui | longueur min `1` |
| `referenceId` | texte | oui | longueur min `1` |
| `consumeEntitlement` | booléen |  |  |
| `billable` | booléen |  |  |
| `billableAmount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `occurredAt` | date-heure ISO 8601 UTC |  |  |
| `metadataJson` | texte |  |  |
| `beneficiaryType` | texte |  |  |
| `beneficiaryCode` | texte |  |  |
| `scopeType` | texte |  |  |
| `scopeNumber` | texte |  |  |
