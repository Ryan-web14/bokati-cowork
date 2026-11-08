# ComplianceController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 24 route(s).

## Routes

<a id="r-get-sni-api-v1-wallets-compliance-audit-verify"></a>
### GET /sni/api/v1/wallets/compliance/audit/verify

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `ComplianceAuditService.ChainReport`
- Modèles : [ComplianceAuditService.ChainReport](#m-ComplianceAuditService-ChainReport)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/compliance/controller/ComplianceController.java`, méthode `verifyAudit`

<a id="r-get-sni-api-v1-wallets-compliance-audit-subjecttype-subjectcode"></a>
### GET /sni/api/v1/wallets/compliance/audit/{subjectType}/{subjectCode}

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `List<ComplianceAuditEntry>`
- Modèles : [ComplianceAuditEntry](#m-ComplianceAuditEntry)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/compliance/controller/ComplianceController.java`, méthode `trail`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subjectType` | chemin | `String` | oui |  |
| `subjectCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-wallets-compliance-cases"></a>
### GET /sni/api/v1/wallets/compliance/cases

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `PaginatedResponse<ComplianceDtos.CaseView>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ComplianceDtos.CaseView](#m-ComplianceDtos-CaseView)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/compliance/controller/ComplianceController.java`, méthode `cases`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `scope` | requête | `String` |  | open |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-get-sni-api-v1-wallets-compliance-cases-casenumber"></a>
### GET /sni/api/v1/wallets/compliance/cases/{caseNumber}

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `ComplianceDtos.CaseView`
- Modèles : [ComplianceDtos.CaseView](#m-ComplianceDtos-CaseView)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/compliance/controller/ComplianceController.java`, méthode `getCase`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `caseNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-wallets-compliance-cases-casenumber-assign"></a>
### POST /sni/api/v1/wallets/compliance/cases/{caseNumber}/assign

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `ComplianceDtos.AssignRequest`
- Réponse : `ComplianceDtos.CaseView`
- Modèles : [ComplianceDtos.CaseView](#m-ComplianceDtos-CaseView), [ComplianceDtos.AssignRequest](#m-ComplianceDtos-AssignRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/compliance/controller/ComplianceController.java`, méthode `assign`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `caseNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-wallets-compliance-cases-casenumber-close"></a>
### POST /sni/api/v1/wallets/compliance/cases/{caseNumber}/close

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `ComplianceDtos.CloseRequest`
- Réponse : `ComplianceDtos.CaseView`
- Modèles : [ComplianceDtos.CaseView](#m-ComplianceDtos-CaseView), [ComplianceDtos.CloseRequest](#m-ComplianceDtos-CloseRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/compliance/controller/ComplianceController.java`, méthode `close`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `caseNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-wallets-compliance-cases-casenumber-escalate"></a>
### POST /sni/api/v1/wallets/compliance/cases/{caseNumber}/escalate

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `ComplianceDtos.EscalateRequest`
- Réponse : `ComplianceDtos.CaseView`
- Modèles : [ComplianceDtos.CaseView](#m-ComplianceDtos-CaseView), [ComplianceDtos.EscalateRequest](#m-ComplianceDtos-EscalateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/compliance/controller/ComplianceController.java`, méthode `escalate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `caseNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-wallets-compliance-cases-casenumber-export"></a>
### GET /sni/api/v1/wallets/compliance/cases/{caseNumber}/export

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Renvoie : `application/json`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/payment/compliance/controller/ComplianceController.java`, méthode `export`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `caseNumber` | chemin | `String` | oui |  |
| `activityDays` | requête | `int` |  | 90 |

<a id="r-get-sni-api-v1-wallets-compliance-cases-casenumber-notes"></a>
### GET /sni/api/v1/wallets/compliance/cases/{caseNumber}/notes

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `List<ComplianceDtos.NoteView>`
- Modèles : [ComplianceDtos.NoteView](#m-ComplianceDtos-NoteView)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/compliance/controller/ComplianceController.java`, méthode `notes`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `caseNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-wallets-compliance-cases-casenumber-notes"></a>
### POST /sni/api/v1/wallets/compliance/cases/{caseNumber}/notes

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `ComplianceDtos.NoteRequest`
- Réponse : `ComplianceDtos.NoteView`
- Modèles : [ComplianceDtos.NoteView](#m-ComplianceDtos-NoteView), [ComplianceDtos.NoteRequest](#m-ComplianceDtos-NoteRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/compliance/controller/ComplianceController.java`, méthode `addNote`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `caseNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-wallets-compliance-dashboard"></a>
### GET /sni/api/v1/wallets/compliance/dashboard

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `ComplianceDashboardService.Dashboard`
- Modèles : [ComplianceDashboardService.Dashboard](#m-ComplianceDashboardService-Dashboard)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/compliance/controller/ComplianceController.java`, méthode `dashboard`

<a id="r-get-sni-api-v1-wallets-compliance-freezes"></a>
### GET /sni/api/v1/wallets/compliance/freezes

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `PaginatedResponse<ComplianceDtos.FreezeView>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ComplianceDtos.FreezeView](#m-ComplianceDtos-FreezeView)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/compliance/controller/ComplianceController.java`, méthode `freezes`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-post-sni-api-v1-wallets-compliance-freezes-freezenumber-lift"></a>
### POST /sni/api/v1/wallets/compliance/freezes/{freezeNumber}/lift

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `ComplianceDtos.LiftRequest`
- Réponse : `ComplianceDtos.FreezeView`
- Modèles : [ComplianceDtos.FreezeView](#m-ComplianceDtos-FreezeView), [ComplianceDtos.LiftRequest](#m-ComplianceDtos-LiftRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/compliance/controller/ComplianceController.java`, méthode `lift`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `freezeNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-wallets-compliance-rules"></a>
### GET /sni/api/v1/wallets/compliance/rules

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `List<ComplianceDtos.RuleView>`
- Modèles : [ComplianceDtos.RuleView](#m-ComplianceDtos-RuleView)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/compliance/controller/ComplianceController.java`, méthode `rules`

<a id="r-put-sni-api-v1-wallets-compliance-rules-rulecode"></a>
### PUT /sni/api/v1/wallets/compliance/rules/{ruleCode}

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `ComplianceDtos.RuleUpdateRequest`
- Réponse : `ComplianceDtos.RuleView`
- Modèles : [ComplianceDtos.RuleView](#m-ComplianceDtos-RuleView), [ComplianceDtos.RuleUpdateRequest](#m-ComplianceDtos-RuleUpdateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/compliance/controller/ComplianceController.java`, méthode `updateRule`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ruleCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-wallets-compliance-screening-checks"></a>
### GET /sni/api/v1/wallets/compliance/screening/checks

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `PaginatedResponse<ComplianceDtos.CheckView>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ComplianceDtos.CheckView](#m-ComplianceDtos-CheckView)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/compliance/controller/ComplianceController.java`, méthode `checks`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `scope` | requête | `String` |  | pending |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-post-sni-api-v1-wallets-compliance-screening-checks"></a>
### POST /sni/api/v1/wallets/compliance/screening/checks

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `ComplianceDtos.ScreenRequest`
- Réponse : `ComplianceDtos.CheckView`
- Modèles : [ComplianceDtos.CheckView](#m-ComplianceDtos-CheckView), [ComplianceDtos.ScreenRequest](#m-ComplianceDtos-ScreenRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/compliance/controller/ComplianceController.java`, méthode `screen`

<a id="r-post-sni-api-v1-wallets-compliance-screening-checks-checknumber-review"></a>
### POST /sni/api/v1/wallets/compliance/screening/checks/{checkNumber}/review

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `ComplianceDtos.ScreeningReviewRequest`
- Réponse : `ComplianceDtos.CheckView`
- Modèles : [ComplianceDtos.CheckView](#m-ComplianceDtos-CheckView), [ComplianceDtos.ScreeningReviewRequest](#m-ComplianceDtos-ScreeningReviewRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/compliance/controller/ComplianceController.java`, méthode `reviewCheck`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `checkNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-wallets-compliance-screening-lists"></a>
### POST /sni/api/v1/wallets/compliance/screening/lists

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `ComplianceDtos.LoadListRequest`
- Réponse : `Map<String, Object>`
- Modèles : [ComplianceDtos.LoadListRequest](#m-ComplianceDtos-LoadListRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/compliance/controller/ComplianceController.java`, méthode `loadList`

<a id="r-get-sni-api-v1-wallets-compliance-screening-subjects-subjecttype-subjectcode"></a>
### GET /sni/api/v1/wallets/compliance/screening/subjects/{subjectType}/{subjectCode}

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `List<ComplianceDtos.CheckView>`
- Modèles : [ComplianceDtos.CheckView](#m-ComplianceDtos-CheckView)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/compliance/controller/ComplianceController.java`, méthode `subjectChecks`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subjectType` | chemin | `String` | oui |  |
| `subjectCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-wallets-compliance-wallets-walletnumber-cases"></a>
### GET /sni/api/v1/wallets/compliance/wallets/{walletNumber}/cases

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `List<ComplianceDtos.CaseView>`
- Modèles : [ComplianceDtos.CaseView](#m-ComplianceDtos-CaseView)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/compliance/controller/ComplianceController.java`, méthode `walletCases`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-wallets-compliance-wallets-walletnumber-evaluate"></a>
### POST /sni/api/v1/wallets/compliance/wallets/{walletNumber}/evaluate

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `List<ComplianceRuleEngine.Outcome>`
- Modèles : [ComplianceRuleEngine.Outcome](#m-ComplianceRuleEngine-Outcome)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/compliance/controller/ComplianceController.java`, méthode `evaluate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-wallets-compliance-wallets-walletnumber-freeze"></a>
### POST /sni/api/v1/wallets/compliance/wallets/{walletNumber}/freeze

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `ComplianceDtos.FreezeRequest`
- Réponse : `ComplianceDtos.FreezeView`
- Modèles : [ComplianceDtos.FreezeView](#m-ComplianceDtos-FreezeView), [ComplianceDtos.FreezeRequest](#m-ComplianceDtos-FreezeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/compliance/controller/ComplianceController.java`, méthode `freeze`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-wallets-compliance-wallets-walletnumber-freezes"></a>
### GET /sni/api/v1/wallets/compliance/wallets/{walletNumber}/freezes

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `List<ComplianceDtos.FreezeView>`
- Modèles : [ComplianceDtos.FreezeView](#m-ComplianceDtos-FreezeView)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/compliance/controller/ComplianceController.java`, méthode `walletFreezes`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ComplianceAuditService-ChainReport"></a>
#### ComplianceAuditService.ChainReport

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `entries` | entier int64 ⚠ |  |  |
| `brokenEntries` | texte[] |  |  |

<a id="m-ComplianceAuditEntry"></a>
#### ComplianceAuditEntry

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `entryUuid` | uuid |  |  |
| `occurredAt` | date-heure ISO 8601 UTC |  |  |
| `actor` | texte |  |  |
| `actorRole` | texte |  |  |
| `action` | texte |  |  |
| `subjectType` | texte |  |  |
| `subjectCode` | texte |  |  |
| `beforeState` | texte |  |  |
| `afterState` | texte |  |  |
| `rationale` | texte |  |  |
| `previousHash` | texte |  |  |
| `currentHash` | texte |  |  |
| `retainUntil` | date AAAA-MM-JJ |  |  |

<a id="m-ComplianceDtos-CaseView"></a>
#### ComplianceDtos.CaseView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `caseNumber` | texte |  |  |
| `subjectType` | texte |  |  |
| `subjectCode` | texte |  |  |
| `walletNumber` | texte |  |  |
| `title` | texte |  |  |
| `triggeredByFlags` | texte[] |  |  |
| `status` | enum : `OPEN`, `IN_REVIEW`, `ESCALATED`, `CLOSED_CLEARED`, `CLOSED_CONFIRMED`, `CLOSED_REPORTED` |  |  |
| `priority` | enum : `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |  |  |
| `assignedTo` | texte |  |  |
| `assignedAt` | date-heure ISO 8601 UTC |  |  |
| `dueAt` | date-heure ISO 8601 UTC |  |  |
| `overdue` | booléen |  |  |
| `findings` | texte |  |  |
| `decision` | enum : `CLEARED`, `CONFIRMED`, `REPORTED` |  |  |
| `decisionRationale` | texte |  |  |
| `decidedBy` | texte |  |  |
| `decidedAt` | date-heure ISO 8601 UTC |  |  |
| `escalatedTo` | texte |  |  |
| `escalatedAt` | date-heure ISO 8601 UTC |  |  |
| `openedBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-ComplianceDtos-AssignRequest"></a>
#### ComplianceDtos.AssignRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `assignee` | texte | oui | longueur min `1` |

<a id="m-ComplianceDtos-CloseRequest"></a>
#### ComplianceDtos.CloseRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `decision` | enum : `CLEARED`, `CONFIRMED`, `REPORTED` | oui |  |
| `rationale` | texte | oui | longueur min `1` |
| `findings` | texte |  |  |

<a id="m-ComplianceDtos-EscalateRequest"></a>
#### ComplianceDtos.EscalateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `escalatedTo` | texte | oui | longueur min `1` |
| `reason` | texte | oui | longueur min `1` |

<a id="m-ComplianceDtos-NoteView"></a>
#### ComplianceDtos.NoteView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `author` | texte |  |  |
| `note` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-ComplianceDtos-NoteRequest"></a>
#### ComplianceDtos.NoteRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `note` | texte | oui | longueur min `1` |

<a id="m-ComplianceDashboardService-Dashboard"></a>
#### ComplianceDashboardService.Dashboard

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `openCases` | entier int64 ⚠ |  |  |
| `overdueCases` | entier int64 ⚠ |  |  |
| `openByPriority` | objet { clé : entier int64 ⚠ } |  |  |
| `openByAge` | objet { clé : entier int64 ⚠ } |  |  |
| `unreviewedListMatches` | entier int64 ⚠ |  |  |
| `activeFreezes` | entier int64 ⚠ |  |  |
| `rules` | [ComplianceDashboardService.RuleStat](#m-ComplianceDashboardService-RuleStat)[] |  |  |

<a id="m-ComplianceDtos-FreezeView"></a>
#### ComplianceDtos.FreezeView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `freezeNumber` | texte |  |  |
| `walletNumber` | texte |  |  |
| `authority` | texte |  |  |
| `instructionReference` | texte |  |  |
| `instructionDate` | date AAAA-MM-JJ |  |  |
| `rationale` | texte |  |  |
| `frozenBy` | texte |  |  |
| `frozenAt` | date-heure ISO 8601 UTC |  |  |
| `liftedBy` | texte |  |  |
| `liftedAt` | date-heure ISO 8601 UTC |  |  |
| `liftReference` | texte |  |  |
| `liftRationale` | texte |  |  |
| `caseNumber` | texte |  |  |

<a id="m-ComplianceDtos-LiftRequest"></a>
#### ComplianceDtos.LiftRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `liftReference` | texte | oui | longueur min `1` |
| `liftRationale` | texte | oui | longueur min `1` |

<a id="m-ComplianceDtos-RuleView"></a>
#### ComplianceDtos.RuleView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ruleCode` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `category` | enum : `VELOCITY`, `AMOUNT`, `PATTERN`, `COUNTERPARTY`, `IDENTITY`, `GEOGRAPHY` |  |  |
| `detector` | texte |  |  |
| `severity` | enum : `INFO`, `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |  |  |
| `action` | enum : `FLAG`, `REQUIRE_REVIEW`, `BLOCK_OPERATION`, `FREEZE_WALLET` |  |  |
| `amountThreshold` | nombre décimal |  |  |
| `countThreshold` | entier |  |  |
| `ratioThreshold` | nombre décimal |  |  |
| `windowMinutes` | entier |  |  |
| `active` | booléen |  |  |
| `effectiveFrom` | date-heure ISO 8601 UTC |  |  |
| `effectiveTo` | date-heure ISO 8601 UTC |  |  |
| `hitCount` | entier int64 ⚠ |  |  |
| `confirmedCount` | entier int64 ⚠ |  |  |
| `dismissedCount` | entier int64 ⚠ |  |  |
| `falsePositiveRate` | nombre décimal |  |  |
| `createdBy` | texte |  |  |
| `approvedBy` | texte |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-ComplianceDtos-RuleUpdateRequest"></a>
#### ComplianceDtos.RuleUpdateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `severity` | enum : `INFO`, `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |  |  |
| `action` | enum : `FLAG`, `REQUIRE_REVIEW`, `BLOCK_OPERATION`, `FREEZE_WALLET` |  |  |
| `amountThreshold` | nombre décimal |  |  |
| `countThreshold` | entier |  |  |
| `ratioThreshold` | nombre décimal |  |  |
| `windowMinutes` | entier |  |  |
| `active` | booléen |  |  |
| `effectiveFrom` | date-heure ISO 8601 UTC |  |  |
| `effectiveTo` | date-heure ISO 8601 UTC |  |  |
| `rationale` | texte | oui | longueur min `1` |

<a id="m-ComplianceDtos-CheckView"></a>
#### ComplianceDtos.CheckView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `checkNumber` | texte |  |  |
| `subjectType` | texte |  |  |
| `subjectCode` | texte |  |  |
| `subjectName` | texte |  |  |
| `trigger` | enum : `ONBOARDING`, `PERIODIC`, `PHONE_CHANGE`, `MANUAL`, `CASE_REVIEW` |  |  |
| `listsChecked` | texte |  |  |
| `result` | enum : `CLEAR`, `POSSIBLE_MATCH`, `MATCH` |  |  |
| `matchDetails` | texte |  |  |
| `checkedAt` | date-heure ISO 8601 UTC |  |  |
| `checkedBy` | texte |  |  |
| `caseNumber` | texte |  |  |
| `reviewedBy` | texte |  |  |
| `reviewedAt` | date-heure ISO 8601 UTC |  |  |
| `reviewOutcome` | texte |  |  |

<a id="m-ComplianceDtos-ScreenRequest"></a>
#### ComplianceDtos.ScreenRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subjectType` | texte | oui | longueur min `1` |
| `subjectCode` | texte | oui | longueur min `1` |
| `fullName` | texte | oui | longueur min `1` |
| `birthYear` | entier |  |  |

<a id="m-ComplianceDtos-ScreeningReviewRequest"></a>
#### ComplianceDtos.ScreeningReviewRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `trueMatch` | booléen |  |  |
| `rationale` | texte | oui | longueur min `1` |

<a id="m-ComplianceDtos-LoadListRequest"></a>
#### ComplianceDtos.LoadListRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `listCode` | texte | oui | longueur min `1` |
| `listVersion` | texte | oui | longueur min `1` |
| `type` | enum : `SANCTION`, `PEP` | oui |  |
| `entries` | [ComplianceDtos.ListEntryRequest](#m-ComplianceDtos-ListEntryRequest)[] | oui |  |

<a id="m-ComplianceRuleEngine-Outcome"></a>
#### ComplianceRuleEngine.Outcome

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ruleCode` | texte |  |  |
| `action` | enum : `FLAG`, `REQUIRE_REVIEW`, `BLOCK_OPERATION`, `FREEZE_WALLET` |  |  |
| `details` | texte |  |  |
| `newFlag` | booléen |  |  |

<a id="m-ComplianceDtos-FreezeRequest"></a>
#### ComplianceDtos.FreezeRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `authority` | texte | oui | longueur min `1` |
| `instructionReference` | texte | oui | longueur min `1` |
| `instructionDate` | date AAAA-MM-JJ |  |  |
| `rationale` | texte | oui | longueur min `1` |

<a id="m-ComplianceDashboardService-RuleStat"></a>
#### ComplianceDashboardService.RuleStat

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ruleCode` | texte |  |  |
| `name` | texte |  |  |
| `action` | enum : `FLAG`, `REQUIRE_REVIEW`, `BLOCK_OPERATION`, `FREEZE_WALLET` |  |  |
| `hits` | entier int64 ⚠ |  |  |
| `confirmed` | entier int64 ⚠ |  |  |
| `dismissed` | entier int64 ⚠ |  |  |
| `falsePositiveRate` | nombre décimal |  |  |
| `hitsLast30Days` | entier int64 ⚠ |  |  |

<a id="m-ComplianceDtos-ListEntryRequest"></a>
#### ComplianceDtos.ListEntryRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `fullName` | texte | oui | longueur min `1` |
| `aliases` | texte |  |  |
| `birthYear` | entier |  |  |
| `nationality` | texte |  |  |
| `reference` | texte |  |  |
