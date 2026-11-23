# PostingController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [accounting](../README.md) · 17 route(s).

## Routes

<a id="r-post-sni-api-v1-accounting-posting-drafts-validate"></a>
### POST /sni/api/v1/accounting/posting/drafts/validate

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_POSTING:REPLAY`
- Réponse : `PostingDtos.BatchResult`
- Modèles : [PostingDtos.BatchResult](#m-PostingDtos-BatchResult)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PostingController.java`, méthode `validateDrafts`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `journalCode` | requête | `String` |  |  |
| `to` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-accounting-posting-events"></a>
### GET /sni/api/v1/accounting/posting/events

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_POSTING:READ`
- Réponse : `List<PostingDtos.Event>`
- Modèles : [PostingDtos.Event](#m-PostingDtos-Event), [PostingEventStatus](#m-PostingEventStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PostingController.java`, méthode `events`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `PostingEventStatus` |  |  |
| `eventType` | requête | `String` |  |  |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |
| `limit` | requête | `int` |  | 100 |

<a id="r-post-sni-api-v1-accounting-posting-events"></a>
### POST /sni/api/v1/accounting/posting/events

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_POSTING:REPLAY`
- Corps JSON (obligatoire) : `PostingDtos.SubmitEvent`
- Réponse : `PostingDtos.Event`
- Modèles : [PostingDtos.Event](#m-PostingDtos-Event), [PostingDtos.SubmitEvent](#m-PostingDtos-SubmitEvent)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PostingController.java`, méthode `submit`

<a id="r-post-sni-api-v1-accounting-posting-events-repost"></a>
### POST /sni/api/v1/accounting/posting/events/repost

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_POSTING:REPLAY`
- Corps JSON (obligatoire) : `PostingDtos.RepostBatchRequest`
- Réponse : `PostingDtos.RepostBatch`
- Modèles : [PostingDtos.RepostBatch](#m-PostingDtos-RepostBatch), [PostingDtos.RepostBatchRequest](#m-PostingDtos-RepostBatchRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PostingController.java`, méthode `repostBatch`

<a id="r-get-sni-api-v1-accounting-posting-events-summary"></a>
### GET /sni/api/v1/accounting/posting/events/summary

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_POSTING:READ`
- Réponse : `PostingDtos.EventSummary`
- Modèles : [PostingDtos.EventSummary](#m-PostingDtos-EventSummary)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PostingController.java`, méthode `summary`

<a id="r-get-sni-api-v1-accounting-posting-events-id"></a>
### GET /sni/api/v1/accounting/posting/events/{id}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_POSTING:READ`
- Réponse : `PostingDtos.Event`
- Modèles : [PostingDtos.Event](#m-PostingDtos-Event)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PostingController.java`, méthode `event`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` (code identifiant ou nombre) | oui |  |

<a id="r-patch-sni-api-v1-accounting-posting-events-id-ignore"></a>
### PATCH /sni/api/v1/accounting/posting/events/{id}/ignore

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_POSTING:REPLAY`
- Corps JSON (obligatoire) : `PostingDtos.Reason`
- Réponse : `PostingDtos.Event`
- Modèles : [PostingDtos.Event](#m-PostingDtos-Event), [PostingDtos.Reason](#m-PostingDtos-Reason)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PostingController.java`, méthode `ignore`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` (code identifiant ou nombre) | oui |  |

<a id="r-patch-sni-api-v1-accounting-posting-events-id-regenerate"></a>
### PATCH /sni/api/v1/accounting/posting/events/{id}/regenerate

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_POSTING:REPLAY`
- Réponse : `PostingDtos.Event`
- Modèles : [PostingDtos.Event](#m-PostingDtos-Event)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PostingController.java`, méthode `regenerate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` (code identifiant ou nombre) | oui |  |

<a id="r-patch-sni-api-v1-accounting-posting-events-id-repost"></a>
### PATCH /sni/api/v1/accounting/posting/events/{id}/repost

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_POSTING:REPLAY`
- Corps JSON (obligatoire) : `PostingDtos.RepostRequest`
- Réponse : `PostingDtos.RepostResult`
- Modèles : [PostingDtos.RepostResult](#m-PostingDtos-RepostResult), [PostingDtos.RepostRequest](#m-PostingDtos-RepostRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PostingController.java`, méthode `repost`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` (code identifiant ou nombre) | oui |  |

<a id="r-post-sni-api-v1-accounting-posting-preview"></a>
### POST /sni/api/v1/accounting/posting/preview

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_POSTING:READ`
- Corps JSON (obligatoire) : `PostingDtos.PreviewRequest`
- Réponse : `PostingDtos.Preview`
- Modèles : [PostingDtos.Preview](#m-PostingDtos-Preview), [PostingDtos.PreviewRequest](#m-PostingDtos-PreviewRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PostingController.java`, méthode `preview`

<a id="r-get-sni-api-v1-accounting-posting-resolvers"></a>
### GET /sni/api/v1/accounting/posting/resolvers

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_POSTING:READ`
- Réponse : `List<PostingDtos.Resolver>`
- Modèles : [PostingDtos.Resolver](#m-PostingDtos-Resolver)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PostingController.java`, méthode `resolvers`

<a id="r-get-sni-api-v1-accounting-posting-rules"></a>
### GET /sni/api/v1/accounting/posting/rules

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_POSTING:READ`
- Réponse : `List<PostingDtos.Rule>`
- Modèles : [PostingDtos.Rule](#m-PostingDtos-Rule)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PostingController.java`, méthode `rules`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `eventType` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-accounting-posting-rules"></a>
### POST /sni/api/v1/accounting/posting/rules

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_POSTING:MANAGE`
- Corps JSON (obligatoire) : `PostingDtos.RuleRequest`
- Réponse : `PostingDtos.Rule`
- Modèles : [PostingDtos.Rule](#m-PostingDtos-Rule), [PostingDtos.RuleRequest](#m-PostingDtos-RuleRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PostingController.java`, méthode `save`

<a id="r-get-sni-api-v1-accounting-posting-rules-code-versions"></a>
### GET /sni/api/v1/accounting/posting/rules/{code}/versions

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_POSTING:READ`
- Réponse : `List<PostingDtos.Rule>`
- Modèles : [PostingDtos.Rule](#m-PostingDtos-Rule)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PostingController.java`, méthode `versions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-accounting-posting-rules-id-activate"></a>
### PATCH /sni/api/v1/accounting/posting/rules/{id}/activate

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_POSTING:MANAGE`
- Réponse : `PostingDtos.Rule`
- Modèles : [PostingDtos.Rule](#m-PostingDtos-Rule)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PostingController.java`, méthode `activate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` (code identifiant ou nombre) | oui |  |

<a id="r-patch-sni-api-v1-accounting-posting-rules-id-retire"></a>
### PATCH /sni/api/v1/accounting/posting/rules/{id}/retire

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_POSTING:MANAGE`
- Réponse : `PostingDtos.Rule`
- Modèles : [PostingDtos.Rule](#m-PostingDtos-Rule)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PostingController.java`, méthode `retire`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` (code identifiant ou nombre) | oui |  |

<a id="r-post-sni-api-v1-accounting-posting-run"></a>
### POST /sni/api/v1/accounting/posting/run

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_POSTING:REPLAY`
- Réponse : `PostingDtos.BatchResult`
- Modèles : [PostingDtos.BatchResult](#m-PostingDtos-BatchResult)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/PostingController.java`, méthode `run`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-PostingDtos-BatchResult"></a>
#### PostingDtos.BatchResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `processed` | entier |  |  |
| `succeeded` | entier |  |  |
| `failed` | entier |  |  |
| `errors` | texte[] |  |  |

<a id="m-PostingDtos-Event"></a>
#### PostingDtos.Event

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | code identifiant (texte) |  |  |
| `eventType` | texte |  |  |
| `reference` | texte |  |  |
| `family` | texte |  |  |
| `eventDate` | date AAAA-MM-JJ |  |  |
| `payload` | texte |  |  |
| `status` | enum : `PENDING`, `POSTED`, `UNBALANCED`, `FAILED`, `IGNORED`, `REVERSED` |  |  |
| `replay` | booléen |  |  |
| `failureReason` | texte |  |  |
| `ruleCode` | texte |  |  |
| `ruleVersion` | entier |  |  |
| `entryNumber` | texte |  |  |
| `provisionalNumber` | texte |  |  |
| `shiftedNote` | texte |  |  |
| `attempts` | entier |  |  |
| `lastAttemptAt` | date-heure ISO 8601 UTC |  |  |
| `postedAt` | date-heure ISO 8601 UTC |  |  |
| `ignoredBy` | texte |  |  |
| `ignoredReason` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-PostingEventStatus"></a>
#### PostingEventStatus

Énumération : `PENDING`, `POSTED`, `UNBALANCED`, `FAILED`, `IGNORED`, `REVERSED`

<a id="m-PostingDtos-SubmitEvent"></a>
#### PostingDtos.SubmitEvent

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `eventType` | texte | oui | longueur min `1` |
| `reference` | texte | oui | longueur min `1` |
| `family` | texte |  |  |
| `eventDate` | date AAAA-MM-JJ |  |  |
| `payload` | objet libre |  |  |
| `replay` | booléen |  |  |

<a id="m-PostingDtos-RepostBatch"></a>
#### PostingDtos.RepostBatch

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `events` | [PostingDtos.RepostPreview](#m-PostingDtos-RepostPreview)[] |  |  |
| `result` | [PostingDtos.BatchResult](#m-PostingDtos-BatchResult) |  |  |

<a id="m-PostingDtos-RepostBatchRequest"></a>
#### PostingDtos.RepostBatchRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ruleCode` | texte |  |  |
| `eventType` | texte |  |  |
| `from` | date AAAA-MM-JJ |  |  |
| `to` | date AAAA-MM-JJ |  |  |
| `dryRun` | booléen |  |  |
| `reason` | texte |  |  |

<a id="m-PostingDtos-EventSummary"></a>
#### PostingDtos.EventSummary

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `counts` | objet { clé : entier int64 (texte au-delà de 2^53) } |  |  |
| `accountingEnabled` | booléen |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `disabledFamilies` | texte[] |  |  |

<a id="m-PostingDtos-Reason"></a>
#### PostingDtos.Reason

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  |  |

<a id="m-PostingDtos-RepostResult"></a>
#### PostingDtos.RepostResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `event` | [PostingDtos.Event](#m-PostingDtos-Event) |  |  |
| `reversal` | texte |  |  |
| `awaitingSecondValidation` | booléen |  |  |
| `newEntry` | texte |  |  |
| `message` | texte |  |  |

<a id="m-PostingDtos-RepostRequest"></a>
#### PostingDtos.RepostRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  |  |
| `date` | date AAAA-MM-JJ |  |  |

<a id="m-PostingDtos-Preview"></a>
#### PostingDtos.Preview

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ruleCode` | texte |  |  |
| `ruleVersion` | entier |  |  |
| `matches` | booléen |  |  |
| `journalCode` | texte |  |  |
| `journalExists` | booléen |  |  |
| `label` | texte |  |  |
| `lines` | [PostingDtos.PreviewLine](#m-PostingDtos-PreviewLine)[] |  |  |
| `totalDebit` | nombre décimal |  |  |
| `totalCredit` | nombre décimal |  |  |
| `balanced` | booléen |  |  |
| `warnings` | texte[] |  |  |
| `error` | texte |  |  |

<a id="m-PostingDtos-PreviewRequest"></a>
#### PostingDtos.PreviewRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ruleId` | code identifiant (texte) |  |  |
| `rule` | [PostingDtos.RuleRequest](#m-PostingDtos-RuleRequest) |  |  |
| `eventId` | code identifiant (texte) |  |  |
| `payload` | objet libre |  |  |

<a id="m-PostingDtos-Resolver"></a>
#### PostingDtos.Resolver

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte |  |  |
| `description` | texte |  |  |

<a id="m-PostingDtos-Rule"></a>
#### PostingDtos.Rule

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | code identifiant (texte) |  |  |
| `code` | texte |  |  |
| `version` | entier |  |  |
| `eventType` | texte |  |  |
| `label` | texte |  |  |
| `description` | texte |  |  |
| `family` | texte |  |  |
| `priority` | entier |  |  |
| `conditions` | texte |  |  |
| `journal` | texte |  |  |
| `entryLabel` | texte |  |  |
| `lines` | texte |  |  |
| `noEntry` | booléen |  |  |
| `samplePayload` | texte |  |  |
| `effectiveFrom` | date AAAA-MM-JJ |  |  |
| `status` | enum : `DRAFT`, `ACTIVE`, `RETIRED` |  |  |
| `activatedBy` | texte |  |  |
| `activatedAt` | date-heure ISO 8601 UTC |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-PostingDtos-RuleRequest"></a>
#### PostingDtos.RuleRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte | oui | longueur min `1` |
| `eventType` | texte | oui | longueur min `1` |
| `label` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `family` | texte | oui | longueur min `1` |
| `priority` | entier |  |  |
| `conditions` | objet libre |  |  |
| `journal` | texte |  |  |
| `entryLabel` | texte |  |  |
| `lines` | objet libre |  |  |
| `noEntry` | booléen |  |  |
| `samplePayload` | objet libre |  |  |
| `effectiveFrom` | date AAAA-MM-JJ |  |  |

<a id="m-PostingDtos-RepostPreview"></a>
#### PostingDtos.RepostPreview

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `eventId` | code identifiant (texte) |  |  |
| `eventType` | texte |  |  |
| `reference` | texte |  |  |
| `eventDate` | date AAAA-MM-JJ |  |  |
| `entryNumber` | texte |  |  |
| `ruleCode` | texte |  |  |
| `ruleVersion` | entier |  |  |
| `newRuleVersion` | entier |  |  |
| `changed` | booléen |  |  |
| `before` | [PostingDtos.PreviewLine](#m-PostingDtos-PreviewLine)[] |  |  |
| `after` | [PostingDtos.PreviewLine](#m-PostingDtos-PreviewLine)[] |  |  |
| `error` | texte |  |  |

<a id="m-PostingDtos-PreviewLine"></a>
#### PostingDtos.PreviewLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `accountNumber` | texte |  |  |
| `accountLabel` | texte |  |  |
| `accountExists` | booléen |  |  |
| `thirdPartyCode` | texte |  |  |
| `label` | texte |  |  |
| `debit` | nombre décimal |  |  |
| `credit` | nombre décimal |  |  |
| `reference` | texte |  |  |
