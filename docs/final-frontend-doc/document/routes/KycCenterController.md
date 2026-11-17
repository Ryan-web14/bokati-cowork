# KycCenterController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [document](../README.md) · 9 route(s).

## Routes

<a id="r-post-sni-api-v1-kyc-cases-backfill"></a>
### POST /sni/api/v1/kyc/cases/backfill

- Accès : back-office : rôle d'administration et droit `KYC:MANAGE`
- Réponse : `Map<String, Integer>`
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycCenterController.java`, méthode `backfill`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `apply` | requête | `boolean` |  | false |

<a id="r-get-sni-api-v1-kyc-dashboard-center"></a>
### GET /sni/api/v1/kyc/dashboard/center

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Réponse : `KycCenterService.CenterDashboard`
- Modèles : [KycCenterService.CenterDashboard](#m-KycCenterService-CenterDashboard)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycCenterController.java`, méthode `dashboard`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-kyc-duplicates"></a>
### GET /sni/api/v1/kyc/duplicates

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Réponse : `List<KycCenterService.DuplicateGroup>`
- Modèles : [KycCenterService.DuplicateGroup](#m-KycCenterService-DuplicateGroup)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycCenterController.java`, méthode `duplicates`

<a id="r-get-sni-api-v1-kyc-reasons"></a>
### GET /sni/api/v1/kyc/reasons

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Réponse : `List<KycReasons.ReasonView>`
- Modèles : [KycReasons.ReasonView](#m-KycReasons-ReasonView)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycCenterController.java`, méthode `reasons`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `kind` | requête | `String` |  |  |
| `activeOnly` | requête | `boolean` |  | true |

<a id="r-post-sni-api-v1-kyc-reasons"></a>
### POST /sni/api/v1/kyc/reasons

- Accès : back-office : rôle d'administration et droit `KYC:MANAGE`
- Corps JSON (obligatoire) : `KycReasons.ReasonRequest`
- Réponse : `KycReasons.ReasonView`
- Modèles : [KycReasons.ReasonView](#m-KycReasons-ReasonView), [KycReasons.ReasonRequest](#m-KycReasons-ReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycCenterController.java`, méthode `createReason`

<a id="r-delete-sni-api-v1-kyc-reasons-code"></a>
### DELETE /sni/api/v1/kyc/reasons/{code}

- Accès : back-office : rôle d'administration et droit `KYC:MANAGE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycCenterController.java`, méthode `deleteReason`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-kyc-reasons-code"></a>
### PUT /sni/api/v1/kyc/reasons/{code}

- Accès : back-office : rôle d'administration et droit `KYC:MANAGE`
- Corps JSON (obligatoire) : `KycReasons.ReasonRequest`
- Réponse : `KycReasons.ReasonView`
- Modèles : [KycReasons.ReasonView](#m-KycReasons-ReasonView), [KycReasons.ReasonRequest](#m-KycReasons-ReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycCenterController.java`, méthode `updateReason`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-kyc-renewals"></a>
### GET /sni/api/v1/kyc/renewals

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Réponse : `List<KycCenterService.RenewalWeek>`
- Modèles : [KycCenterService.RenewalWeek](#m-KycCenterService-RenewalWeek)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycCenterController.java`, méthode `renewals`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `days` | requête | `int` |  | 90 |

<a id="r-post-sni-api-v1-kyc-renewals-remind"></a>
### POST /sni/api/v1/kyc/renewals/remind

- Accès : back-office : rôle d'administration et droit `KYC:MANAGE`
- Corps JSON (facultatif) : `KycCenterController.RemindRequest`
- Réponse : `KycCenterService.RemindResult`
- Modèles : [KycCenterService.RemindResult](#m-KycCenterService-RemindResult), [KycCenterController.RemindRequest](#m-KycCenterController-RemindRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycCenterController.java`, méthode `remind`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-KycCenterService-CenterDashboard"></a>
#### KycCenterService.CenterDashboard

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `from` | date AAAA-MM-JJ |  |  |
| `to` | date AAAA-MM-JJ |  |  |
| `byStatus` | objet { clé : entier int64 (texte au-delà de 2^53) } |  |  |
| `waitingReview` | [KycCenterService.AgeBuckets](#m-KycCenterService-AgeBuckets) |  |  |
| `averageProcessingHours` | nombre décimal |  |  |
| `medianProcessingHours` | nombre décimal |  |  |
| `decidedInPeriod` | entier int64 (texte au-delà de 2^53) |  |  |
| `overdue` | [KycCenterService.CaseRef](#m-KycCenterService-CaseRef)[] |  |  |
| `agents` | [KycCenterService.AgentLoad](#m-KycCenterService-AgentLoad)[] |  |  |
| `blocked` | [KycCenterService.CaseRef](#m-KycCenterService-CaseRef)[] |  |  |
| `duplicateGroups` | entier int64 (texte au-delà de 2^53) |  |  |

<a id="m-KycCenterService-DuplicateGroup"></a>
#### KycCenterService.DuplicateGroup

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentType` | texte |  |  |
| `normalizedNumber` | texte |  |  |
| `pieces` | [KycCenterService.DuplicatePiece](#m-KycCenterService-DuplicatePiece)[] |  |  |

<a id="m-KycReasons-ReasonView"></a>
#### KycReasons.ReasonView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `kind` | texte |  |  |
| `requiresText` | booléen |  |  |
| `active` | booléen |  |  |
| `sortOrder` | entier |  |  |
| `systemReason` | booléen |  |  |

<a id="m-KycReasons-ReasonRequest"></a>
#### KycReasons.ReasonRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `kind` | texte |  |  |
| `requiresText` | booléen |  |  |
| `active` | booléen |  |  |
| `sortOrder` | entier |  |  |

<a id="m-KycCenterService-RenewalWeek"></a>
#### KycCenterService.RenewalWeek

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `weekStart` | date AAAA-MM-JJ |  |  |
| `items` | [KycCenterService.RenewalItem](#m-KycCenterService-RenewalItem)[] |  |  |

<a id="m-KycCenterService-RemindResult"></a>
#### KycCenterService.RemindResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reminded` | entier |  |  |
| `skippedRecentlyReminded` | entier |  |  |

<a id="m-KycCenterController-RemindRequest"></a>
#### KycCenterController.RemindRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `days` | entier |  |  |
| `kycDocumentIds` | code identifiant (texte)[] |  |  |

<a id="m-KycCenterService-AgeBuckets"></a>
#### KycCenterService.AgeBuckets

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `underTwoDays` | entier int64 (texte au-delà de 2^53) |  |  |
| `twoToSevenDays` | entier int64 (texte au-delà de 2^53) |  |  |
| `overSevenDays` | entier int64 (texte au-delà de 2^53) |  |  |

<a id="m-KycCenterService-CaseRef"></a>
#### KycCenterService.CaseRef

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `status` | enum : `NOT_STARTED`, `IN_PROGRESS`, `SUBMITTED`, `UNDER_REVIEW`, `APPROVED`, `REJECTED`, `PENDING_CORRECTION`, `RENEWAL_REQUIRED`, `EXPIRED` |  |  |
| `ownerType` | texte |  |  |
| `ownerId` | code identifiant (texte) |  |  |
| `since` | date-heure ISO 8601 UTC |  |  |
| `detail` | texte |  |  |

<a id="m-KycCenterService-AgentLoad"></a>
#### KycCenterService.AgentLoad

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `userId` | code identifiant (texte) |  |  |
| `name` | texte |  |  |
| `assignedOpen` | entier int64 (texte au-delà de 2^53) |  |  |
| `decidedInPeriod` | entier int64 (texte au-delà de 2^53) |  |  |

<a id="m-KycCenterService-DuplicatePiece"></a>
#### KycCenterService.DuplicatePiece

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `kycDocumentId` | code identifiant (texte) |  |  |
| `kycCaseCode` | texte |  |  |
| `ownerType` | texte |  |  |
| `ownerId` | code identifiant (texte) |  |  |
| `documentNumber` | texte |  |  |
| `status` | enum : `PENDING`, `VERIFIED`, `REJECTED`, `EXPIRED` |  |  |

<a id="m-KycCenterService-RenewalItem"></a>
#### KycCenterService.RenewalItem

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `kycDocumentId` | code identifiant (texte) |  |  |
| `kycCaseCode` | texte |  |  |
| `documentType` | texte |  |  |
| `documentNumber` | texte |  |  |
| `ownerType` | texte |  |  |
| `ownerId` | code identifiant (texte) |  |  |
| `expiryDate` | date AAAA-MM-JJ |  |  |
| `daysLeft` | entier int64 (texte au-delà de 2^53) |  |  |
| `lastRemindedAt` | date-heure ISO 8601 UTC |  |  |
