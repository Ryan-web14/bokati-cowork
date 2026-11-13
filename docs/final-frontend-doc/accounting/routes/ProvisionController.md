# ProvisionController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [accounting](../README.md) · 25 route(s).

## Routes

<a id="r-get-sni-api-v1-accounting-provisions-closing-check"></a>
### GET /sni/api/v1/accounting/provisions/closing-check

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<ReceivableRiskService.Finding>`
- Modèles : [ReceivableRiskService.Finding](#m-ReceivableRiskService-Finding)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `closingCheck`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | requête | `int` | oui |  |

<a id="r-get-sni-api-v1-accounting-provisions-reports-report"></a>
### GET /sni/api/v1/accounting/provisions/reports/{report}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `non typé, voir le code`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `report`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `report` | chemin | `String` | oui |  |
| `year` | requête | `Integer` |  |  |
| `date` | requête | `LocalDate` |  |  |
| `segment` | requête | `String` |  |  |
| `format` | requête | `String` |  | json |

<a id="r-post-sni-api-v1-accounting-provisions-reviews"></a>
### POST /sni/api/v1/accounting/provisions/reviews

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `RiskProvisionService.ReviewRequest`
- Réponse : `AccProvisionReview`
- Modèles : [AccProvisionReview](#m-AccProvisionReview), [RiskProvisionService.ReviewRequest](#m-RiskProvisionService-ReviewRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `prepareReview`

<a id="r-post-sni-api-v1-accounting-provisions-reviews-id-validate"></a>
### POST /sni/api/v1/accounting/provisions/reviews/{id}/validate

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Réponse : `AccProvisionReview`
- Modèles : [AccProvisionReview](#m-AccProvisionReview)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `validateReview`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-accounting-receivables-aging"></a>
### GET /sni/api/v1/accounting/receivables/aging

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<ReceivableRiskService.ReceivableView>`
- Modèles : [ReceivableRiskService.ReceivableView](#m-ReceivableRiskService-ReceivableView)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `aging`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `date` | requête | `LocalDate` |  |  |
| `segment` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-accounting-receivables-controls"></a>
### GET /sni/api/v1/accounting/receivables/controls

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<ReceivableRiskService.Finding>`
- Modèles : [ReceivableRiskService.Finding](#m-ReceivableRiskService-Finding)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `controls`

<a id="r-post-sni-api-v1-accounting-receivables-impairment-runs"></a>
### POST /sni/api/v1/accounting/receivables/impairment-runs

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Réponse : `ReceivableRiskService.ImpairmentRun`
- Modèles : [ReceivableRiskService.ImpairmentRun](#m-ReceivableRiskService-ImpairmentRun)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `runImpairment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `period` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-receivables-impairment-runs-preview"></a>
### GET /sni/api/v1/accounting/receivables/impairment-runs/preview

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `ReceivableRiskService.ImpairmentRun`
- Modèles : [ReceivableRiskService.ImpairmentRun](#m-ReceivableRiskService-ImpairmentRun)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `previewImpairment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `period` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-receivables-proposals"></a>
### GET /sni/api/v1/accounting/receivables/proposals

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<AccReceivableProposal>`
- Modèles : [AccReceivableProposal](#m-AccReceivableProposal)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `proposals`

<a id="r-post-sni-api-v1-accounting-receivables-proposals-id-decision"></a>
### POST /sni/api/v1/accounting/receivables/proposals/{id}/decision

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `ReceivableRiskService.Decision`
- Réponse : `AccReceivableProposal`
- Modèles : [AccReceivableProposal](#m-AccReceivableProposal), [ReceivableRiskService.Decision](#m-ReceivableRiskService-Decision)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `decide`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-accounting-receivables-write-offs"></a>
### GET /sni/api/v1/accounting/receivables/write-offs

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<AccReceivableWriteoff>`
- Modèles : [AccReceivableWriteoff](#m-AccReceivableWriteoff)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `writeOffs`

<a id="r-post-sni-api-v1-accounting-receivables-write-offs"></a>
### POST /sni/api/v1/accounting/receivables/write-offs

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `ReceivableRiskService.WriteOffRequest`
- Réponse : `AccReceivableWriteoff`
- Modèles : [AccReceivableWriteoff](#m-AccReceivableWriteoff), [ReceivableRiskService.WriteOffRequest](#m-ReceivableRiskService-WriteOffRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `prepareWriteOff`

<a id="r-post-sni-api-v1-accounting-receivables-write-offs-id-approve"></a>
### POST /sni/api/v1/accounting/receivables/write-offs/{id}/approve

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Réponse : `AccReceivableWriteoff`
- Modèles : [AccReceivableWriteoff](#m-AccReceivableWriteoff)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `approveWriteOff`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-accounting-receivables-write-offs-id-reject"></a>
### POST /sni/api/v1/accounting/receivables/write-offs/{id}/reject

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (facultatif) : `ProvisionController.RejectRequest`
- Réponse : `AccReceivableWriteoff`
- Modèles : [AccReceivableWriteoff](#m-AccReceivableWriteoff), [ProvisionController.RejectRequest](#m-ProvisionController-RejectRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `rejectWriteOff`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-accounting-receivables-documentnumber"></a>
### GET /sni/api/v1/accounting/receivables/{documentNumber}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `ReceivableRiskService.ReceivableView`
- Modèles : [ReceivableRiskService.ReceivableView](#m-ReceivableRiskService-ReceivableView)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `receivable`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-receivables-documentnumber-back-to-normal"></a>
### POST /sni/api/v1/accounting/receivables/{documentNumber}/back-to-normal

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `ReceivableRiskService.BackToNormalRequest`
- Réponse : `ReceivableRiskService.ReceivableView`
- Modèles : [ReceivableRiskService.ReceivableView](#m-ReceivableRiskService-ReceivableView), [ReceivableRiskService.BackToNormalRequest](#m-ReceivableRiskService-BackToNormalRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `backToNormal`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-receivables-documentnumber-classify"></a>
### POST /sni/api/v1/accounting/receivables/{documentNumber}/classify

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `ReceivableRiskService.ClassifyRequest`
- Réponse : `ReceivableRiskService.ReceivableView`
- Modèles : [ReceivableRiskService.ReceivableView](#m-ReceivableRiskService-ReceivableView), [ReceivableRiskService.ClassifyRequest](#m-ReceivableRiskService-ClassifyRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `classify`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-receivables-documentnumber-individual-rate"></a>
### POST /sni/api/v1/accounting/receivables/{documentNumber}/individual-rate

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `ProvisionController.RateRequest`
- Réponse : `ReceivableRiskService.ReceivableView`
- Modèles : [ReceivableRiskService.ReceivableView](#m-ReceivableRiskService-ReceivableView), [ProvisionController.RateRequest](#m-ProvisionController-RateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `individualRate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-risks"></a>
### GET /sni/api/v1/accounting/risks

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<AccRisk>`
- Modèles : [AccRisk](#m-AccRisk)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `risks`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-accounting-risks"></a>
### POST /sni/api/v1/accounting/risks

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `RiskProvisionService.RiskRequest`
- Réponse : `RiskProvisionService.RiskView`
- Modèles : [RiskProvisionService.RiskView](#m-RiskProvisionService-RiskView), [RiskProvisionService.RiskRequest](#m-RiskProvisionService-RiskRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `createRisk`

<a id="r-post-sni-api-v1-accounting-risks-reclass-short-term"></a>
### POST /sni/api/v1/accounting/risks/reclass-short-term

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Réponse : `List<String>`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `reclass`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `date` | requête | `LocalDate` |  |  |

<a id="r-post-sni-api-v1-accounting-risks-retirement-obligations"></a>
### POST /sni/api/v1/accounting/risks/retirement-obligations

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `RiskProvisionService.RetirementRequest`
- Réponse : `AccRetirementObligation`
- Modèles : [AccRetirementObligation](#m-AccRetirementObligation), [RiskProvisionService.RetirementRequest](#m-RiskProvisionService-RetirementRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `retirement`

<a id="r-get-sni-api-v1-accounting-risks-code"></a>
### GET /sni/api/v1/accounting/risks/{code}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `RiskProvisionService.RiskView`
- Modèles : [RiskProvisionService.RiskView](#m-RiskProvisionService-RiskView)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `risk`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-risks-code-provision"></a>
### POST /sni/api/v1/accounting/risks/{code}/provision

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `RiskProvisionService.ProvisionRequest`
- Réponse : `RiskProvisionService.RiskView`
- Modèles : [RiskProvisionService.RiskView](#m-RiskProvisionService-RiskView), [RiskProvisionService.ProvisionRequest](#m-RiskProvisionService-ProvisionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `provision`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-risks-code-valuations"></a>
### POST /sni/api/v1/accounting/risks/{code}/valuations

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `RiskProvisionService.ValuationRequest`
- Réponse : `RiskProvisionService.RiskView`
- Modèles : [RiskProvisionService.RiskView](#m-RiskProvisionService-RiskView), [RiskProvisionService.ValuationRequest](#m-RiskProvisionService-ValuationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/ProvisionController.java`, méthode `valuate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ReceivableRiskService-Finding"></a>
#### ReceivableRiskService.Finding

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `severity` | texte |  |  |
| `subject` | texte |  |  |
| `message` | texte |  |  |

<a id="m-AccProvisionReview"></a>
#### AccProvisionReview

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `fiscalYear` | texte |  |  |
| `reviewDate` | date AAAA-MM-JJ |  |  |
| `voucherNumber` | texte |  |  |
| `summary` | texte |  |  |
| `status` | texte |  |  |
| `reviewedBy` | texte |  |  |
| `validatedBy` | texte |  |  |
| `validatedAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-RiskProvisionService-ReviewRequest"></a>
#### RiskProvisionService.ReviewRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `fiscalYear` | texte |  |  |
| `summary` | texte |  |  |

<a id="m-ReceivableRiskService-ReceivableView"></a>
#### ReceivableRiskService.ReceivableView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte |  |  |
| `customerCode` | texte |  |  |
| `customerType` | texte |  |  |
| `status` | texte |  |  |
| `account` | texte |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `daysOverdue` | entier int64 ⚠ |  |  |
| `balanceDue` | nombre décimal |  |  |
| `baseHt` | nombre décimal |  |  |
| `impairment` | nombre décimal |  |  |
| `net` | nombre décimal |  |  |
| `reason` | texte |  |  |
| `voucherNumber` | texte |  |  |

<a id="m-ReceivableRiskService-ImpairmentRun"></a>
#### ReceivableRiskService.ImpairmentRun

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `period` | texte |  |  |
| `simulated` | booléen |  |  |
| `allowances` | nombre décimal |  |  |
| `releases` | nombre décimal |  |  |
| `entryNumber` | texte |  |  |
| `lines` | [ReceivableRiskService.ImpairmentLine](#m-ReceivableRiskService-ImpairmentLine)[] |  |  |
| `warnings` | texte[] |  |  |

<a id="m-AccReceivableProposal"></a>
#### AccReceivableProposal

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `documentNumber` | texte |  |  |
| `customerCode` | texte |  |  |
| `triggerCode` | texte |  |  |
| `targetStatus` | texte |  |  |
| `daysOverdue` | entier |  |  |
| `balanceDue` | nombre décimal |  |  |
| `guarantees` | nombre décimal |  |  |
| `suggested` | nombre décimal |  |  |
| `status` | texte |  |  |
| `postponedUntil` | date AAAA-MM-JJ |  |  |
| `decidedBy` | texte |  |  |
| `decidedAt` | date-heure ISO 8601 UTC |  |  |
| `reason` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-ReceivableRiskService-Decision"></a>
#### ReceivableRiskService.Decision

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `decision` | texte |  |  |
| `postponedUntil` | date AAAA-MM-JJ |  |  |
| `reason` | texte |  |  |
| `targetStatus` | texte |  |  |

<a id="m-AccReceivableWriteoff"></a>
#### AccReceivableWriteoff

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `voucherNumber` | texte |  |  |
| `customerCode` | texte |  |  |
| `documentNumbers` | texte |  |  |
| `amountHt` | nombre décimal |  |  |
| `amountVat` | nombre décimal |  |  |
| `amountTtc` | nombre décimal |  |  |
| `impairmentReleased` | nombre décimal |  |  |
| `vatRecovered` | booléen |  |  |
| `reason` | texte |  |  |
| `comment` | texte |  |  |
| `justificationRefs` | texte |  |  |
| `origin` | texte |  |  |
| `status` | texte |  |  |
| `approvalLevel` | texte |  |  |
| `preparedBy` | texte |  |  |
| `approvedBy` | texte |  |  |
| `approvedAt` | date-heure ISO 8601 UTC |  |  |
| `entryNumber` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-ReceivableRiskService-WriteOffRequest"></a>
#### ReceivableRiskService.WriteOffRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumbers` | texte[] |  |  |
| `reason` | texte |  |  |
| `comment` | texte |  |  |
| `justificationRefs` | texte |  |  |
| `vatRecovered` | booléen |  |  |

<a id="m-ProvisionController-RejectRequest"></a>
#### ProvisionController.RejectRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  |  |

<a id="m-ReceivableRiskService-BackToNormalRequest"></a>
#### ReceivableRiskService.BackToNormalRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  |  |
| `date` | date AAAA-MM-JJ |  |  |

<a id="m-ReceivableRiskService-ClassifyRequest"></a>
#### ReceivableRiskService.ClassifyRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `status` | texte |  |  |
| `reason` | texte |  |  |
| `date` | date AAAA-MM-JJ |  |  |
| `individualRate` | nombre décimal |  |  |
| `rateReason` | texte |  |  |
| `deductible` | booléen |  |  |

<a id="m-ProvisionController-RateRequest"></a>
#### ProvisionController.RateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `rate` | nombre décimal |  |  |
| `reason` | texte |  |  |

<a id="m-AccRisk"></a>
#### AccRisk

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `code` | texte |  |  |
| `nature` | texte |  |  |
| `title` | texte |  |  |
| `thirdParty` | texte |  |  |
| `contractRef` | texte |  |  |
| `originRef` | texte |  |  |
| `description` | texte |  |  |
| `counsel` | texte |  |  |
| `jurisdiction` | texte |  |  |
| `nextDeadline` | date AAAA-MM-JJ |  |  |
| `probability` | texte |  |  |
| `estimatedAmount` | nombre décimal |  |  |
| `estimationMethod` | texte |  |  |
| `estimatedAt` | date AAAA-MM-JJ |  |  |
| `outflowDate` | date AAAA-MM-JJ |  |  |
| `term` | texte |  |  |
| `provisionAccount` | texte |  |  |
| `provisionAmount` | nombre décimal |  |  |
| `deductible` | booléen |  |  |
| `sectionCode` | texte |  |  |
| `documents` | texte |  |  |
| `status` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-RiskProvisionService-RiskView"></a>
#### RiskProvisionService.RiskView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `risk` | [AccRisk](#m-AccRisk) |  |  |
| `natureLabel` | texte |  |  |
| `contingentLiability` | booléen |  |  |
| `valuations` | [AccRiskValuation](#m-AccRiskValuation)[] |  |  |
| `moves` | [AccRiskProvisionMove](#m-AccRiskProvisionMove)[] |  |  |
| `warnings` | texte[] |  |  |

<a id="m-RiskProvisionService-RiskRequest"></a>
#### RiskProvisionService.RiskRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `nature` | texte |  |  |
| `title` | texte |  |  |
| `thirdParty` | texte |  |  |
| `contractRef` | texte |  |  |
| `originRef` | texte |  |  |
| `description` | texte |  |  |
| `counsel` | texte |  |  |
| `jurisdiction` | texte |  |  |
| `nextDeadline` | date AAAA-MM-JJ |  |  |
| `probability` | texte |  |  |
| `estimatedAmount` | nombre décimal |  |  |
| `estimationMethod` | texte |  |  |
| `outflowDate` | date AAAA-MM-JJ |  |  |
| `deductible` | booléen |  |  |
| `sectionCode` | texte |  |  |
| `documents` | texte |  |  |

<a id="m-AccRetirementObligation"></a>
#### AccRetirementObligation

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `fiscalYear` | texte |  |  |
| `computedAt` | date AAAA-MM-JJ |  |  |
| `method` | texte |  |  |
| `assumptions` | texte |  |  |
| `headcount` | entier |  |  |
| `amount` | nombre décimal |  |  |
| `previous` | nombre décimal |  |  |
| `detail` | texte |  |  |
| `entryNumber` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-RiskProvisionService-RetirementRequest"></a>
#### RiskProvisionService.RetirementRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `fiscalYear` | texte |  |  |
| `method` | texte |  |  |
| `assumptions` | texte |  |  |
| `headcount` | entier |  |  |
| `amount` | nombre décimal |  |  |
| `detail` | texte |  |  |

<a id="m-RiskProvisionService-ProvisionRequest"></a>
#### RiskProvisionService.ProvisionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `kind` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `date` | date AAAA-MM-JJ |  |  |
| `reason` | texte |  |  |
| `closeRisk` | booléen |  |  |

<a id="m-RiskProvisionService-ValuationRequest"></a>
#### RiskProvisionService.ValuationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `amount` | nombre décimal |  |  |
| `probability` | texte |  |  |
| `reason` | texte |  |  |
| `documentRef` | texte |  |  |
| `outflowDate` | date AAAA-MM-JJ |  |  |

<a id="m-ReceivableRiskService-ImpairmentLine"></a>
#### ReceivableRiskService.ImpairmentLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte |  |  |
| `customerCode` | texte |  |  |
| `status` | texte |  |  |
| `daysOverdue` | entier int64 ⚠ |  |  |
| `remainingTtc` | nombre décimal |  |  |
| `baseHt` | nombre décimal |  |  |
| `guarantees` | nombre décimal |  |  |
| `method` | texte |  |  |
| `rate` | nombre décimal |  |  |
| `amount` | nombre décimal |  |  |
| `previous` | nombre décimal |  |  |
| `difference` | nombre décimal |  |  |
| `deductible` | booléen |  |  |

<a id="m-AccRiskValuation"></a>
#### AccRiskValuation

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `riskId` | entier int64 ⚠ |  |  |
| `valuationDate` | date AAAA-MM-JJ |  |  |
| `amount` | nombre décimal |  |  |
| `probability` | texte |  |  |
| `reason` | texte |  |  |
| `documentRef` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-AccRiskProvisionMove"></a>
#### AccRiskProvisionMove

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `riskId` | entier int64 ⚠ |  |  |
| `kind` | texte |  |  |
| `moveDate` | date AAAA-MM-JJ |  |  |
| `amount` | nombre décimal |  |  |
| `fromAccount` | texte |  |  |
| `toAccount` | texte |  |  |
| `reason` | texte |  |  |
| `entryNumber` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
