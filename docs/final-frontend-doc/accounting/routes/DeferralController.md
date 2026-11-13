# DeferralController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [accounting](../README.md) · 14 route(s).

## Routes

<a id="r-get-sni-api-v1-accounting-accruals"></a>
### GET /sni/api/v1/accounting/accruals

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<AccAccrual>`
- Modèles : [AccAccrual](#m-AccAccrual)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/DeferralController.java`, méthode `accruals`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `period` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-accounting-accruals"></a>
### POST /sni/api/v1/accounting/accruals

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `DeferralService.AccrualRequest`
- Réponse : `AccAccrual`
- Modèles : [AccAccrual](#m-AccAccrual), [DeferralService.AccrualRequest](#m-DeferralService-AccrualRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/DeferralController.java`, méthode `propose`

<a id="r-post-sni-api-v1-accounting-accruals-id-match"></a>
### POST /sni/api/v1/accounting/accruals/{id}/match

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `DeferralController.MatchRequest`
- Réponse : `AccAccrual`
- Modèles : [AccAccrual](#m-AccAccrual), [DeferralController.MatchRequest](#m-DeferralController-MatchRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/DeferralController.java`, méthode `match`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-accounting-accruals-id-post"></a>
### POST /sni/api/v1/accounting/accruals/{id}/post

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Réponse : `AccAccrual`
- Modèles : [AccAccrual](#m-AccAccrual)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/DeferralController.java`, méthode `post`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-accounting-deferral-runs"></a>
### POST /sni/api/v1/accounting/deferral-runs

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Réponse : `DeferralService.RunResult`
- Modèles : [DeferralService.RunResult](#m-DeferralService-RunResult)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/DeferralController.java`, méthode `run`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `period` | requête | `String` | oui |  |
| `kind` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-accounting-deferral-runs-preview"></a>
### POST /sni/api/v1/accounting/deferral-runs/preview

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Réponse : `DeferralService.RunResult`
- Modèles : [DeferralService.RunResult](#m-DeferralService-RunResult)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/DeferralController.java`, méthode `preview`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `period` | requête | `String` | oui |  |
| `kind` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-accounting-deferrals"></a>
### GET /sni/api/v1/accounting/deferrals

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<DeferralService.DetailLine>`
- Modèles : [DeferralService.DetailLine](#m-DeferralService-DetailLine)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/DeferralController.java`, méthode `detail`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `kind` | requête | `String` |  |  |
| `date` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-accounting-deferrals-closing-check"></a>
### GET /sni/api/v1/accounting/deferrals/closing-check

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<DeferralService.Finding>`
- Modèles : [DeferralService.Finding](#m-DeferralService-Finding)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/DeferralController.java`, méthode `closingCheck`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | requête | `int` | oui |  |

<a id="r-get-sni-api-v1-accounting-deferrals-controls"></a>
### GET /sni/api/v1/accounting/deferrals/controls

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<DeferralService.Finding>`
- Modèles : [DeferralService.Finding](#m-DeferralService-Finding)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/DeferralController.java`, méthode `controls`

<a id="r-get-sni-api-v1-accounting-deferrals-reports-report"></a>
### GET /sni/api/v1/accounting/deferrals/reports/{report}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `non typé, voir le code`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/DeferralController.java`, méthode `report`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `report` | chemin | `String` | oui |  |
| `year` | requête | `Integer` |  |  |
| `date` | requête | `LocalDate` |  |  |
| `format` | requête | `String` |  | json |

<a id="r-get-sni-api-v1-accounting-deferrals-schedules-id"></a>
### GET /sni/api/v1/accounting/deferrals/schedules/{id}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `DeferralService.ScheduleView`
- Modèles : [DeferralService.ScheduleView](#m-DeferralService-ScheduleView)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/DeferralController.java`, méthode `schedule`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-accounting-deferrals-schedules-id-events"></a>
### POST /sni/api/v1/accounting/deferrals/schedules/{id}/events

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `DeferralService.EventRequest`
- Réponse : `AccDeferralSchedule`
- Modèles : [AccDeferralSchedule](#m-AccDeferralSchedule), [DeferralService.EventRequest](#m-DeferralService-EventRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/DeferralController.java`, méthode `event`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-accounting-deferrals-schedules-id-usage"></a>
### POST /sni/api/v1/accounting/deferrals/schedules/{id}/usage

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `DeferralService.UsageRequest`
- Réponse : `AccDeferralSchedule`
- Modèles : [AccDeferralSchedule](#m-AccDeferralSchedule), [DeferralService.UsageRequest](#m-DeferralService-UsageRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/DeferralController.java`, méthode `usage`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-billing-documents-number-lines-lineid-service-period"></a>
### PATCH /sni/api/v1/billing/documents/{number}/lines/{lineId}/service-period

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Corps JSON (obligatoire) : `DeferralService.PeriodRequest`
- Réponse : `BillingDocumentLine`
- Modèles : [BillingDocumentLine](#m-BillingDocumentLine), [DeferralService.PeriodRequest](#m-DeferralService-PeriodRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/DeferralController.java`, méthode `servicePeriod`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |
| `lineId` | chemin | `Long` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-AccAccrual"></a>
#### AccAccrual

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `kind` | texte |  |  |
| `sourceType` | texte |  |  |
| `reference` | texte |  |  |
| `thirdParty` | texte |  |  |
| `label` | texte |  |  |
| `accountNumber` | texte |  |  |
| `sectionCode` | texte |  |  |
| `periodCode` | texte |  |  |
| `estimatedAmount` | nombre décimal |  |  |
| `vatAmount` | nombre décimal |  |  |
| `status` | texte |  |  |
| `entryNumber` | texte |  |  |
| `reversalEntryNumber` | texte |  |  |
| `actualReference` | texte |  |  |
| `actualAmount` | nombre décimal |  |  |
| `gap` | nombre décimal |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-DeferralService-AccrualRequest"></a>
#### DeferralService.AccrualRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `kind` | texte |  |  |
| `reference` | texte |  |  |
| `thirdParty` | texte |  |  |
| `label` | texte |  |  |
| `accountNumber` | texte |  |  |
| `sectionCode` | texte |  |  |
| `period` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `vatAmount` | nombre décimal |  |  |
| `sourceType` | texte |  |  |

<a id="m-DeferralController-MatchRequest"></a>
#### DeferralController.MatchRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `actualReference` | texte |  |  |
| `actualAmount` | nombre décimal |  |  |

<a id="m-DeferralService-RunResult"></a>
#### DeferralService.RunResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `period` | texte |  |  |
| `kind` | texte |  |  |
| `simulated` | booléen |  |  |
| `totalNeed` | nombre décimal |  |  |
| `totalPrevious` | nombre décimal |  |  |
| `difference` | nombre décimal |  |  |
| `entryNumber` | texte |  |  |
| `lines` | [DeferralService.RunLine](#m-DeferralService-RunLine)[] |  |  |
| `warnings` | texte[] |  |  |

<a id="m-DeferralService-DetailLine"></a>
#### DeferralService.DetailLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `scheduleId` | entier int64 ⚠ |  |  |
| `kind` | texte |  |  |
| `documentNumber` | texte |  |  |
| `thirdParty` | texte |  |  |
| `label` | texte |  |  |
| `activity` | texte |  |  |
| `account` | texte |  |  |
| `start` | date AAAA-MM-JJ |  |  |
| `end` | date AAAA-MM-JJ |  |  |
| `method` | texte |  |  |
| `status` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `recognized` | nombre décimal |  |  |
| `remaining` | nombre décimal |  |  |

<a id="m-DeferralService-Finding"></a>
#### DeferralService.Finding

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `severity` | texte |  |  |
| `subject` | texte |  |  |
| `message` | texte |  |  |

<a id="m-DeferralService-ScheduleView"></a>
#### DeferralService.ScheduleView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `schedule` | [AccDeferralSchedule](#m-AccDeferralSchedule) |  |  |
| `events` | [AccDeferralScheduleEvent](#m-AccDeferralScheduleEvent)[] |  |  |
| `usage` | [AccUsageConsumption](#m-AccUsageConsumption)[] |  |  |
| `monthlyNeed` | objet { clé : nombre décimal } |  |  |

<a id="m-AccDeferralSchedule"></a>
#### AccDeferralSchedule

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `companyCode` | texte |  |  |
| `kind` | texte |  |  |
| `sourceType` | texte |  |  |
| `sourceLineId` | entier int64 ⚠ |  |  |
| `documentNumber` | texte |  |  |
| `thirdParty` | texte |  |  |
| `label` | texte |  |  |
| `accountNumber` | texte |  |  |
| `sectionCode` | texte |  |  |
| `activity` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `endDate` | date AAAA-MM-JJ |  |  |
| `method` | texte |  |  |
| `totalUnits` | nombre décimal |  |  |
| `recognitionSource` | texte |  |  |
| `version` | entier |  |  |
| `status` | texte |  |  |
| `endedOn` | date AAAA-MM-JJ |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `sourceRef` | texte |  |  |

<a id="m-DeferralService-EventRequest"></a>
#### DeferralService.EventRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | texte |  |  |
| `date` | date AAAA-MM-JJ |  |  |
| `days` | entier |  |  |
| `newDate` | date AAAA-MM-JJ |  |  |
| `reason` | texte |  |  |

<a id="m-DeferralService-UsageRequest"></a>
#### DeferralService.UsageRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `units` | nombre décimal |  |  |
| `sourceRef` | texte |  |  |
| `totalUnits` | nombre décimal |  |  |

<a id="m-BillingDocumentLine"></a>
#### BillingDocumentLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `document` | [BillingDocument](#m-BillingDocument) |  |  |
| `lineOrder` | entier |  |  |
| `lineType` | enum : `PRODUCT`, `SERVICE`, `SUBSCRIPTION`, `PASS`, `ADDON`, `BOOKING`, `OVERAGE`, `PENALTY`, `WALLET`, `ADJUSTMENT` |  |  |
| `itemCode` | texte |  |  |
| `category` | texte |  |  |
| `description` | texte |  |  |
| `detailedDescription` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `unitPrice` | nombre décimal |  |  |
| `discountRate` | nombre décimal |  |  |
| `discountAmount` | nombre décimal |  |  |
| `taxable` | booléen |  |  |
| `taxIncluded` | booléen |  |  |
| `vatRate` | nombre décimal |  |  |
| `additionalCentRate` | nombre décimal |  |  |
| `subtotalAmount` | nombre décimal |  |  |
| `taxableAmount` | nombre décimal |  |  |
| `vatAmount` | nombre décimal |  |  |
| `additionalCentAmount` | nombre décimal |  |  |
| `taxAmount` | nombre décimal |  |  |
| `totalAmount` | nombre décimal |  |  |
| `unit` | texte |  |  |
| `externalReference` | texte |  |  |
| `notes` | texte |  |  |
| `optional` | booléen |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `serviceStart` | date AAAA-MM-JJ |  |  |
| `serviceEnd` | date AAAA-MM-JJ |  |  |
| `recognitionMethod` | texte |  |  |
| `recognitionSource` | texte |  |  |
| `originalLineId` | entier int64 ⚠ |  |  |

<a id="m-DeferralService-PeriodRequest"></a>
#### DeferralService.PeriodRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `serviceStart` | date AAAA-MM-JJ |  |  |
| `serviceEnd` | date AAAA-MM-JJ |  |  |
| `method` | texte |  |  |
| `reason` | texte |  |  |

<a id="m-DeferralService-RunLine"></a>
#### DeferralService.RunLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `scheduleId` | entier int64 ⚠ |  |  |
| `documentNumber` | texte |  |  |
| `label` | texte |  |  |
| `account` | texte |  |  |
| `section` | texte |  |  |
| `start` | date AAAA-MM-JJ |  |  |
| `end` | date AAAA-MM-JJ |  |  |
| `amount` | nombre décimal |  |  |
| `need` | nombre décimal |  |  |
| `previous` | nombre décimal |  |  |
| `difference` | nombre décimal |  |  |

<a id="m-AccDeferralScheduleEvent"></a>
#### AccDeferralScheduleEvent

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `scheduleId` | entier int64 ⚠ |  |  |
| `type` | texte |  |  |
| `eventDate` | date AAAA-MM-JJ |  |  |
| `beforeValue` | texte |  |  |
| `afterValue` | texte |  |  |
| `reason` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-AccUsageConsumption"></a>
#### AccUsageConsumption

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `scheduleId` | entier int64 ⚠ |  |  |
| `usageDate` | date AAAA-MM-JJ |  |  |
| `units` | nombre décimal |  |  |
| `sourceRef` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-BillingDocument"></a>
#### BillingDocument

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `documentNumber` | texte |  |  |
| `documentType` | enum : `QUOTE`, `PROFORMA_INVOICE`, `INVOICE`, `CREDIT_NOTE`, `CORRECTIVE_INVOICE`, `DEBIT_NOTE` |  |  |
| `status` | enum : `DRAFT`, `ISSUED`, `VALIDATED`, `SENT`, `VIEWED`, `NEGOTIATION`, `ACCEPTED`, `DEPOSIT_REQUESTED`, `DEPOSIT_PAID`, `REJECTED`, `EXPIRED`, `CONVERTED`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`, `CANCELLED`, `REFUNDED`, `APPLIED`, `WRITTEN_OFF` |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `customerName` | texte |  |  |
| `customerEmail` | texte |  |  |
| `customerPhone` | texte |  |  |
| `billingAddressJson` | texte |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `title` | texte |  |  |
| `description` | texte |  |  |
| `terms` | texte |  |  |
| `currency` | texte |  |  |
| `subtotalAmount` | nombre décimal |  |  |
| `discountAmount` | nombre décimal |  |  |
| `taxableAmount` | nombre décimal |  |  |
| `vatAmount` | nombre décimal |  |  |
| `additionalCentAmount` | nombre décimal |  |  |
| `taxAmount` | nombre décimal |  |  |
| `totalAmount` | nombre décimal |  |  |
| `paidAmount` | nombre décimal |  |  |
| `balanceDue` | nombre décimal |  |  |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `paymentReminderSentAt` | date-heure ISO 8601 UTC |  |  |
| `paymentReminderCount` | entier |  |  |
| `issuedAt` | date-heure ISO 8601 UTC |  |  |
| `sentAt` | date-heure ISO 8601 UTC |  |  |
| `paidAt` | date-heure ISO 8601 UTC |  |  |
| `cancelledAt` | date-heure ISO 8601 UTC |  |  |
| `archivedAt` | date-heure ISO 8601 UTC |  |  |
| `discountOverrideReason` | texte |  |  |
| `discountOverrideBy` | texte |  |  |
| `discountOverrideAt` | date-heure ISO 8601 UTC |  |  |
| `pdfStorageProvider` | texte |  |  |
| `pdfStoragePath` | texte |  |  |
| `pdfSha256` | texte |  |  |
| `pdfSealedAt` | date-heure ISO 8601 UTC |  |  |
| `customerReference` | texte |  |  |
| `poNumber` | texte |  |  |
| `projectCode` | texte |  |  |
| `salespersonCode` | texte |  |  |
| `deliveryAddressJson` | texte |  |  |
| `language` | texte |  |  |
| `exchangeRate` | nombre décimal |  |  |
| `paymentReference` | texte |  |  |
| `paymentInstructions` | texte |  |  |
| `bankDetailsJson` | texte |  |  |
| `metadataJson` | texte |  |  |
| `internalNotes` | texte |  |  |
| `locked` | booléen |  |  |
| `validatedAt` | date-heure ISO 8601 UTC |  |  |
| `fiscalDate` | date AAAA-MM-JJ |  |  |
| `fiscalNumber` | texte |  |  |
| `previousHash` | texte |  |  |
| `currentHash` | texte |  |  |
| `hashAlgorithm` | texte |  |  |
| `fiscalSignature` | texte |  |  |
| `signatureAlgorithm` | texte |  |  |
| `signedAt` | date-heure ISO 8601 UTC |  |  |
| `sellerName` | texte |  |  |
| `sellerNiu` | texte |  |  |
| `sellerAddressJson` | texte |  |  |
| `sellerPhone` | texte |  |  |
| `sellerEmail` | texte |  |  |
| `customerNiu` | texte |  |  |
| `customerCategory` | texte |  |  |
| `customerIdentifiersJson` | texte |  |  |
| `customerVatExemptionNote` | texte |  |  |
| `companyCustomerId` | entier int64 ⚠ |  |  |
| `companyCustomerCode` | texte |  |  |
| `companyName` | texte |  |  |
| `companyIdentifiersJson` | texte |  |  |
| `usageAnnexJson` | texte |  |  |
| `companyLinkSource` | texte |  |  |
| `companyLinkedAt` | date-heure ISO 8601 UTC |  |  |
| `companyLinkedBy` | texte |  |  |
| `originalDocumentNumber` | texte |  |  |
| `originalDocumentType` | texte |  |  |
| `creditNoteReason` | texte |  |  |
| `deletedAt` | date-heure ISO 8601 UTC |  |  |
| `deletedBy` | texte |  |  |
| `deleteReason` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `createdBy` | texte |  |  |
| `createdByName` | texte |  |  |
| `createdByTitle` | texte |  |  |
| `versionAuthor` | texte |  |  |
| `versionAuthorName` | texte |  |  |
| `versionAuthorTitle` | texte |  |  |
| `versionReason` | texte |  |  |
| `versionOrigin` | texte |  |  |
| `replacedByDocumentNumber` | texte |  |  |
| `depositForDocumentNumber` | texte |  |  |
| `dealCode` | texte |  |  |
| `convertedToDocumentNumber` | texte |  |  |
| `recipientCorrectedAt` | date-heure ISO 8601 UTC |  |  |
