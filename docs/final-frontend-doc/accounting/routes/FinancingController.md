# FinancingController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [accounting](../README.md) · 40 route(s).

## Routes

<a id="r-get-sni-api-v1-accounting-commitment-types"></a>
### GET /sni/api/v1/accounting/commitment-types

- Accès : back-office : rôle d'administration et droit `FINANCING:READ`
- Réponse : `List<AccCommitmentType>`
- Modèles : [AccCommitmentType](#m-AccCommitmentType)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `commitmentTypes`

<a id="r-post-sni-api-v1-accounting-commitment-types"></a>
### POST /sni/api/v1/accounting/commitment-types

- Accès : back-office : rôle d'administration et droit `FINANCING:MANAGE`
- Corps JSON (obligatoire) : `AccCommitmentType`
- Réponse : `AccCommitmentType`
- Modèles : [AccCommitmentType](#m-AccCommitmentType)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `saveType`

<a id="r-get-sni-api-v1-accounting-commitments"></a>
### GET /sni/api/v1/accounting/commitments

- Accès : back-office : rôle d'administration et droit `FINANCING:READ`
- Réponse : `List<AccCommitment>`
- Modèles : [AccCommitment](#m-AccCommitment)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `commitments`

<a id="r-post-sni-api-v1-accounting-commitments"></a>
### POST /sni/api/v1/accounting/commitments

- Accès : back-office : rôle d'administration et droit `FINANCING:MANAGE`
- Corps JSON (obligatoire) : `CommitmentService.CommitmentRequest`
- Réponse : `AccCommitment`
- Modèles : [AccCommitment](#m-AccCommitment), [CommitmentService.CommitmentRequest](#m-CommitmentService-CommitmentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `saveCommitment`

<a id="r-get-sni-api-v1-accounting-commitments-note"></a>
### GET /sni/api/v1/accounting/commitments/note

- Accès : back-office : rôle d'administration et droit `FINANCING:READ`
- Réponse : `List<CommitmentService.NoteLine>`
- Modèles : [CommitmentService.NoteLine](#m-CommitmentService-NoteLine)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `commitmentNote`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `date` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-accounting-commitments-reconciliation"></a>
### GET /sni/api/v1/accounting/commitments/reconciliation

- Accès : back-office : rôle d'administration et droit `FINANCING:READ`
- Réponse : `CommitmentService.Reconciliation`
- Modèles : [CommitmentService.Reconciliation](#m-CommitmentService-Reconciliation)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `commitmentReconciliation`

<a id="r-post-sni-api-v1-accounting-commitments-code-release"></a>
### POST /sni/api/v1/accounting/commitments/{code}/release

- Accès : back-office : rôle d'administration et droit `FINANCING:MANAGE`
- Réponse : `AccCommitment`
- Modèles : [AccCommitment](#m-AccCommitment)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `release`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `date` | requête | `LocalDate` |  |  |
| `reference` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-accounting-loans"></a>
### GET /sni/api/v1/accounting/loans

- Accès : back-office : rôle d'administration et droit `FINANCING:READ`
- Réponse : `List<AccLoan>`
- Modèles : [AccLoan](#m-AccLoan)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `loans`

<a id="r-post-sni-api-v1-accounting-loans"></a>
### POST /sni/api/v1/accounting/loans

- Accès : back-office : rôle d'administration et droit `FINANCING:MANAGE`
- Corps JSON (obligatoire) : `LoanService.LoanRequest`
- Réponse : `LoanService.LoanView`
- Modèles : [LoanService.LoanView](#m-LoanService-LoanView), [LoanService.LoanRequest](#m-LoanService-LoanRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `createLoan`

<a id="r-post-sni-api-v1-accounting-loans-accrue"></a>
### POST /sni/api/v1/accounting/loans/accrue

- Accès : back-office : rôle d'administration et droit `FINANCING:MANAGE`
- Réponse : `List<LoanService.Accrual>`
- Modèles : [LoanService.Accrual](#m-LoanService-Accrual)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `accrue`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `date` | requête | `LocalDate` | oui |  |

<a id="r-get-sni-api-v1-accounting-loans-note"></a>
### GET /sni/api/v1/accounting/loans/note

- Accès : back-office : rôle d'administration et droit `FINANCING:READ`
- Réponse : `List<LoanService.NoteLine>`
- Modèles : [LoanService.NoteLine](#m-LoanService-NoteLine)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `loanNote`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `date` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-accounting-loans-upcoming"></a>
### GET /sni/api/v1/accounting/loans/upcoming

- Accès : back-office : rôle d'administration et droit `FINANCING:READ`
- Réponse : `List<Map<String, Object>>`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `upcoming`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `LocalDate` | oui |  |
| `to` | requête | `LocalDate` | oui |  |

<a id="r-get-sni-api-v1-accounting-loans-code"></a>
### GET /sni/api/v1/accounting/loans/{code}

- Accès : back-office : rôle d'administration et droit `FINANCING:READ`
- Réponse : `LoanService.LoanView`
- Modèles : [LoanService.LoanView](#m-LoanService-LoanView)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `loan`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-loans-code-disburse"></a>
### POST /sni/api/v1/accounting/loans/{code}/disburse

- Accès : back-office : rôle d'administration et droit `FINANCING:MANAGE`
- Réponse : `LoanService.LoanView`
- Modèles : [LoanService.LoanView](#m-LoanService-LoanView)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `disburse`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-loans-code-export"></a>
### GET /sni/api/v1/accounting/loans/{code}/export

- Accès : back-office : rôle d'administration et droit `FINANCING:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `loanCsv`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-loans-code-history"></a>
### GET /sni/api/v1/accounting/loans/{code}/history

- Accès : back-office : rôle d'administration et droit `FINANCING:READ`
- Réponse : `List<AccLoanInstallment>`
- Modèles : [AccLoanInstallment](#m-AccLoanInstallment)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `history`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-loans-code-installments-number-post"></a>
### POST /sni/api/v1/accounting/loans/{code}/installments/{number}/post

- Accès : back-office : rôle d'administration et droit `FINANCING:MANAGE`
- Réponse : `AccLoanInstallment`
- Modèles : [AccLoanInstallment](#m-AccLoanInstallment)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `postInstallment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `number` | chemin | `int` | oui |  |

<a id="r-get-sni-api-v1-accounting-loans-code-pdf"></a>
### GET /sni/api/v1/accounting/loans/{code}/pdf

- Accès : back-office : rôle d'administration et droit `FINANCING:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `loanPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-loans-code-postpone"></a>
### POST /sni/api/v1/accounting/loans/{code}/postpone

- Accès : back-office : rôle d'administration et droit `FINANCING:MANAGE`
- Réponse : `LoanService.LoanView`
- Modèles : [LoanService.LoanView](#m-LoanService-LoanView)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `postpone`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-loans-code-prepay"></a>
### POST /sni/api/v1/accounting/loans/{code}/prepay

- Accès : back-office : rôle d'administration et droit `FINANCING:MANAGE`
- Corps JSON (obligatoire) : `LoanService.Prepayment`
- Réponse : `LoanService.LoanView`
- Modèles : [LoanService.LoanView](#m-LoanService-LoanView), [LoanService.Prepayment](#m-LoanService-Prepayment)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `prepay`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-loans-code-rate"></a>
### POST /sni/api/v1/accounting/loans/{code}/rate

- Accès : back-office : rôle d'administration et droit `FINANCING:MANAGE`
- Réponse : `LoanService.LoanView`
- Modèles : [LoanService.LoanView](#m-LoanService-LoanView)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `rate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `rate` | requête | `BigDecimal` | oui |  |
| `date` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-accounting-partners"></a>
### GET /sni/api/v1/accounting/partners

- Accès : back-office : rôle d'administration et droit `FINANCING:READ`
- Réponse : `List<AccPartnerAccount>`
- Modèles : [AccPartnerAccount](#m-AccPartnerAccount)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `partners`

<a id="r-post-sni-api-v1-accounting-partners"></a>
### POST /sni/api/v1/accounting/partners

- Accès : back-office : rôle d'administration et droit `FINANCING:MANAGE`
- Corps JSON (obligatoire) : `PartnerAccountService.PartnerRequest`
- Réponse : `AccPartnerAccount`
- Modèles : [AccPartnerAccount](#m-AccPartnerAccount), [PartnerAccountService.PartnerRequest](#m-PartnerAccountService-PartnerRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `savePartner`

<a id="r-get-sni-api-v1-accounting-partners-note"></a>
### GET /sni/api/v1/accounting/partners/note

- Accès : back-office : rôle d'administration et droit `FINANCING:READ`
- Réponse : `List<PartnerAccountService.NoteLine>`
- Modèles : [PartnerAccountService.NoteLine](#m-PartnerAccountService-NoteLine)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `partnerNote`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `date` | requête | `LocalDate` |  |  |

<a id="r-post-sni-api-v1-accounting-partners-code-interest"></a>
### POST /sni/api/v1/accounting/partners/{code}/interest

- Accès : back-office : rôle d'administration et droit `FINANCING:MANAGE`
- Réponse : `AccPartnerInterest`
- Modèles : [AccPartnerInterest](#m-AccPartnerInterest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `interest`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `from` | requête | `LocalDate` | oui |  |
| `to` | requête | `LocalDate` | oui |  |

<a id="r-get-sni-api-v1-accounting-partners-code-interests"></a>
### GET /sni/api/v1/accounting/partners/{code}/interests

- Accès : back-office : rôle d'administration et droit `FINANCING:READ`
- Réponse : `List<AccPartnerInterest>`
- Modèles : [AccPartnerInterest](#m-AccPartnerInterest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `interests`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-partners-code-movements"></a>
### POST /sni/api/v1/accounting/partners/{code}/movements

- Accès : back-office : rôle d'administration et droit `FINANCING:MANAGE`
- Corps JSON (obligatoire) : `PartnerAccountService.Movement`
- Réponse : `String`
- Modèles : [PartnerAccountService.Movement](#m-PartnerAccountService-Movement)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `move`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-partners-code-statement"></a>
### GET /sni/api/v1/accounting/partners/{code}/statement

- Accès : back-office : rôle d'administration et droit `FINANCING:READ`
- Réponse : `PartnerAccountService.Statement`
- Modèles : [PartnerAccountService.Statement](#m-PartnerAccountService-Statement)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `statement`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-accounting-payroll-employees"></a>
### GET /sni/api/v1/accounting/payroll/employees

- Accès : back-office : rôle d'administration et droit `PAYROLL:IMPORT`
- Réponse : `List<AccPayrollEmployee>`
- Modèles : [AccPayrollEmployee](#m-AccPayrollEmployee)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `payrollEmployees`

<a id="r-post-sni-api-v1-accounting-payroll-employees"></a>
### POST /sni/api/v1/accounting/payroll/employees

- Accès : back-office : rôle d'administration et droit `PAYROLL_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `AccPayrollEmployee`
- Réponse : `AccPayrollEmployee`
- Modèles : [AccPayrollEmployee](#m-AccPayrollEmployee)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `saveEmployee`

<a id="r-get-sni-api-v1-accounting-payroll-imports"></a>
### GET /sni/api/v1/accounting/payroll/imports

- Accès : back-office : rôle d'administration et droit `PAYROLL:IMPORT`
- Réponse : `List<AccPayrollImport>`
- Modèles : [AccPayrollImport](#m-AccPayrollImport)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `payrollImports`

<a id="r-post-sni-api-v1-accounting-payroll-imports"></a>
### POST /sni/api/v1/accounting/payroll/imports

- Accès : back-office : rôle d'administration et droit `PAYROLL:IMPORT`
- Envoi : `multipart/form-data`
- Réponse : `PayrollService.ImportView`
- Modèles : [PayrollService.ImportView](#m-PayrollService-ImportView)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `importPayroll`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `period` | requête | `String` | oui |  |
| `template` | requête | `String` |  |  |
| `file` | partie multipart | `MultipartFile` | oui |  |

<a id="r-get-sni-api-v1-accounting-payroll-imports-id"></a>
### GET /sni/api/v1/accounting/payroll/imports/{id}

- Accès : back-office : rôle d'administration et droit `PAYROLL:IMPORT`
- Réponse : `PayrollService.ImportView`
- Modèles : [PayrollService.ImportView](#m-PayrollService-ImportView)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `payrollImport`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-accounting-payroll-imports-id-lines"></a>
### GET /sni/api/v1/accounting/payroll/imports/{id}/lines

- Accès : back-office : rôle d'administration et droit `PAYROLL:IMPORT`
- Réponse : `List<AccPayrollLine>`
- Modèles : [AccPayrollLine](#m-AccPayrollLine)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `payrollLines`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-accounting-payroll-imports-id-pay"></a>
### POST /sni/api/v1/accounting/payroll/imports/{id}/pay

- Accès : back-office : rôle d'administration et droit `PAYROLL:POST`
- Corps JSON (obligatoire) : `PayrollService.PaymentRequest`
- Réponse : `PayrollService.ImportView`
- Modèles : [PayrollService.ImportView](#m-PayrollService-ImportView), [PayrollService.PaymentRequest](#m-PayrollService-PaymentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `payPayroll`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-accounting-payroll-imports-id-post"></a>
### POST /sni/api/v1/accounting/payroll/imports/{id}/post

- Accès : back-office : rôle d'administration et droit `PAYROLL:POST`
- Réponse : `PayrollService.ImportView`
- Modèles : [PayrollService.ImportView](#m-PayrollService-ImportView)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `postPayroll`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-accounting-payroll-rubrics"></a>
### GET /sni/api/v1/accounting/payroll/rubrics

- Accès : back-office : rôle d'administration et droit `PAYROLL:IMPORT`
- Réponse : `List<AccPayrollRubric>`
- Modèles : [AccPayrollRubric](#m-AccPayrollRubric)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `rubrics`

<a id="r-post-sni-api-v1-accounting-payroll-rubrics"></a>
### POST /sni/api/v1/accounting/payroll/rubrics

- Accès : back-office : rôle d'administration et droit `PAYROLL_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `AccPayrollRubric`
- Réponse : `AccPayrollRubric`
- Modèles : [AccPayrollRubric](#m-AccPayrollRubric)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `saveRubric`

<a id="r-get-sni-api-v1-accounting-payroll-templates"></a>
### GET /sni/api/v1/accounting/payroll/templates

- Accès : back-office : rôle d'administration et droit `PAYROLL:IMPORT`
- Réponse : `List<AccPayrollTemplate>`
- Modèles : [AccPayrollTemplate](#m-AccPayrollTemplate)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `payrollTemplates`

<a id="r-post-sni-api-v1-accounting-payroll-templates"></a>
### POST /sni/api/v1/accounting/payroll/templates

- Accès : back-office : rôle d'administration et droit `PAYROLL_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `AccPayrollTemplate`
- Réponse : `AccPayrollTemplate`
- Modèles : [AccPayrollTemplate](#m-AccPayrollTemplate)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FinancingController.java`, méthode `savePayrollTemplate`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-AccCommitmentType"></a>
#### AccCommitmentType

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `defaultDirection` | texte |  |  |
| `lease` | booléen |  |  |
| `active` | booléen |  |  |

<a id="m-AccCommitment"></a>
#### AccCommitment

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `code` | texte |  |  |
| `typeCode` | texte |  |  |
| `direction` | texte |  |  |
| `thirdPartyCode` | texte |  |  |
| `thirdPartyName` | texte |  |  |
| `description` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `monthlyAmount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `endDate` | date AAAA-MM-JJ |  |  |
| `renewalDate` | date AAAA-MM-JJ |  |  |
| `noticeDate` | date AAAA-MM-JJ |  |  |
| `linkedType` | texte |  |  |
| `linkedCode` | texte |  |  |
| `documentRefs` | texte |  |  |
| `status` | texte |  |  |
| `bookedAmount` | nombre décimal |  |  |
| `lastEntryNumber` | texte |  |  |
| `releasedOn` | date AAAA-MM-JJ |  |  |
| `releaseReference` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CommitmentService-CommitmentRequest"></a>
#### CommitmentService.CommitmentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `typeCode` | texte |  |  |
| `direction` | texte |  |  |
| `thirdPartyCode` | texte |  |  |
| `thirdPartyName` | texte |  |  |
| `description` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `monthlyAmount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `endDate` | date AAAA-MM-JJ |  |  |
| `renewalDate` | date AAAA-MM-JJ |  |  |
| `noticeDate` | date AAAA-MM-JJ |  |  |
| `linkedType` | texte |  |  |
| `linkedCode` | texte |  |  |
| `documentRefs` | texte |  |  |

<a id="m-CommitmentService-NoteLine"></a>
#### CommitmentService.NoteLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `direction` | texte |  |  |
| `typeCode` | texte |  |  |
| `label` | texte |  |  |
| `count` | entier |  |  |
| `total` | nombre décimal |  |  |
| `withinOneYear` | nombre décimal |  |  |
| `oneToFiveYears` | nombre décimal |  |  |
| `beyondFiveYears` | nombre décimal |  |  |

<a id="m-CommitmentService-Reconciliation"></a>
#### CommitmentService.Reconciliation

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `givenRegister` | nombre décimal |  |  |
| `givenAccount` | nombre décimal |  |  |
| `givenCounterAccount` | nombre décimal |  |  |
| `givenGap` | nombre décimal |  |  |
| `receivedRegister` | nombre décimal |  |  |
| `receivedAccount` | nombre décimal |  |  |
| `receivedCounterAccount` | nombre décimal |  |  |
| `receivedGap` | nombre décimal |  |  |
| `balanced` | booléen |  |  |

<a id="m-AccLoan"></a>
#### AccLoan

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `code` | texte |  |  |
| `lenderType` | texte |  |  |
| `lenderCode` | texte |  |  |
| `lenderName` | texte |  |  |
| `purpose` | texte |  |  |
| `principal` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `disbursedOn` | date AAAA-MM-JJ |  |  |
| `firstDueDate` | date AAAA-MM-JJ |  |  |
| `rate` | nombre décimal |  |  |
| `rateType` | texte |  |  |
| `dayCount` | texte |  |  |
| `frequencyMonths` | entier |  |  |
| `termCount` | entier |  |  |
| `deferralType` | texte |  |  |
| `deferralCount` | entier |  |  |
| `method` | texte |  |  |
| `fees` | nombre décimal |  |  |
| `insurance` | nombre décimal |  |  |
| `loanAccount` | texte |  |  |
| `interestAccount` | texte |  |  |
| `insuranceAccount` | texte |  |  |
| `feesAccount` | texte |  |  |
| `accruedAccount` | texte |  |  |
| `bankAccountCode` | texte |  |  |
| `sectionCode` | texte |  |  |
| `budgetItemCode` | texte |  |  |
| `postingMode` | texte |  |  |
| `scheduleVersion` | entier |  |  |
| `status` | texte |  |  |
| `disbursementEntry` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `closedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-LoanService-LoanView"></a>
#### LoanService.LoanView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `loan` | [AccLoan](#m-AccLoan) |  |  |
| `schedule` | [AccLoanInstallment](#m-AccLoanInstallment)[] |  |  |
| `events` | [AccLoanEvent](#m-AccLoanEvent)[] |  |  |
| `outstanding` | nombre décimal |  |  |

<a id="m-LoanService-LoanRequest"></a>
#### LoanService.LoanRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `lenderType` | texte |  |  |
| `lenderCode` | texte |  |  |
| `lenderName` | texte |  |  |
| `purpose` | texte |  |  |
| `principal` | nombre décimal |  |  |
| `disbursedOn` | date AAAA-MM-JJ |  |  |
| `firstDueDate` | date AAAA-MM-JJ |  |  |
| `rate` | nombre décimal |  |  |
| `rateType` | texte |  |  |
| `dayCount` | texte |  |  |
| `frequencyMonths` | entier |  |  |
| `termCount` | entier |  |  |
| `deferralType` | texte |  |  |
| `deferralCount` | entier |  |  |
| `method` | texte |  |  |
| `fees` | nombre décimal |  |  |
| `insurance` | nombre décimal |  |  |
| `loanAccount` | texte |  |  |
| `interestAccount` | texte |  |  |
| `insuranceAccount` | texte |  |  |
| `feesAccount` | texte |  |  |
| `accruedAccount` | texte |  |  |
| `bankAccountCode` | texte |  |  |
| `sectionCode` | texte |  |  |
| `budgetItemCode` | texte |  |  |
| `postingMode` | texte |  |  |

<a id="m-LoanService-Accrual"></a>
#### LoanService.Accrual

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `loanCode` | texte |  |  |
| `interest` | nombre décimal |  |  |
| `entryNumber` | texte |  |  |
| `reversalNumber` | texte |  |  |

<a id="m-LoanService-NoteLine"></a>
#### LoanService.NoteLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `lender` | texte |  |  |
| `account` | texte |  |  |
| `outstanding` | nombre décimal |  |  |
| `withinOneYear` | nombre décimal |  |  |
| `beyondOneYear` | nombre décimal |  |  |
| `rate` | nombre décimal |  |  |
| `lastDueDate` | date AAAA-MM-JJ |  |  |
| `guarantees` | texte[] |  |  |

<a id="m-AccLoanInstallment"></a>
#### AccLoanInstallment

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `loanId` | entier int64 ⚠ |  |  |
| `version` | entier |  |  |
| `number` | entier |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `openingBalance` | nombre décimal |  |  |
| `interest` | nombre décimal |  |  |
| `principal` | nombre décimal |  |  |
| `insurance` | nombre décimal |  |  |
| `payment` | nombre décimal |  |  |
| `closingBalance` | nombre décimal |  |  |
| `status` | texte |  |  |
| `entryNumber` | texte |  |  |
| `postedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-LoanService-Prepayment"></a>
#### LoanService.Prepayment

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `amount` | nombre décimal |  |  |
| `penalty` | nombre décimal |  |  |
| `mode` | texte |  |  |

<a id="m-AccPartnerAccount"></a>
#### AccPartnerAccount

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `code` | texte |  |  |
| `partnerName` | texte |  |  |
| `thirdPartyCode` | texte |  |  |
| `accountNumber` | texte |  |  |
| `blocked` | booléen |  |  |
| `blockedUntil` | date AAAA-MM-JJ |  |  |
| `rate` | nombre décimal |  |  |
| `admittedRate` | nombre décimal |  |  |
| `cap` | nombre décimal |  |  |
| `openedOn` | date AAAA-MM-JJ |  |  |
| `status` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-PartnerAccountService-PartnerRequest"></a>
#### PartnerAccountService.PartnerRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `partnerName` | texte |  |  |
| `thirdPartyCode` | texte |  |  |
| `accountNumber` | texte |  |  |
| `blocked` | booléen |  |  |
| `blockedUntil` | date AAAA-MM-JJ |  |  |
| `rate` | nombre décimal |  |  |
| `admittedRate` | nombre décimal |  |  |
| `cap` | nombre décimal |  |  |
| `openedOn` | date AAAA-MM-JJ |  |  |

<a id="m-PartnerAccountService-NoteLine"></a>
#### PartnerAccountService.NoteLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `partnerName` | texte |  |  |
| `account` | texte |  |  |
| `balance` | nombre décimal |  |  |
| `rate` | nombre décimal |  |  |
| `blocked` | booléen |  |  |
| `blockedUntil` | date AAAA-MM-JJ |  |  |
| `interestOfYear` | nombre décimal |  |  |
| `nonDeductibleOfYear` | nombre décimal |  |  |

<a id="m-AccPartnerInterest"></a>
#### AccPartnerInterest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `partnerCode` | texte |  |  |
| `fromDate` | date AAAA-MM-JJ |  |  |
| `toDate` | date AAAA-MM-JJ |  |  |
| `averageBalance` | nombre décimal |  |  |
| `rate` | nombre décimal |  |  |
| `interest` | nombre décimal |  |  |
| `nonDeductible` | nombre décimal |  |  |
| `entryNumber` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-PartnerAccountService-Movement"></a>
#### PartnerAccountService.Movement

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | texte |  |  |
| `date` | date AAAA-MM-JJ |  |  |
| `amount` | nombre décimal |  |  |
| `counterAccount` | texte |  |  |
| `vatAmount` | nombre décimal |  |  |
| `vatAccount` | texte |  |  |
| `journalCode` | texte |  |  |
| `label` | texte |  |  |
| `reference` | texte |  |  |

<a id="m-PartnerAccountService-Statement"></a>
#### PartnerAccountService.Statement

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `partner` | [AccPartnerAccount](#m-AccPartnerAccount) |  |  |
| `from` | date AAAA-MM-JJ |  |  |
| `to` | date AAAA-MM-JJ |  |  |
| `opening` | nombre décimal |  |  |
| `lines` | [PartnerAccountService.StatementLine](#m-PartnerAccountService-StatementLine)[] |  |  |
| `closing` | nombre décimal |  |  |

<a id="m-AccPayrollEmployee"></a>
#### AccPayrollEmployee

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `matricule` | texte |  |  |
| `fullName` | texte |  |  |
| `thirdPartyCode` | texte |  |  |
| `sectionCode` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-AccPayrollImport"></a>
#### AccPayrollImport

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `periodCode` | texte |  |  |
| `templateCode` | texte |  |  |
| `fileName` | texte |  |  |
| `status` | texte |  |  |
| `grossTotal` | nombre décimal |  |  |
| `employerTotal` | nombre décimal |  |  |
| `deductionTotal` | nombre décimal |  |  |
| `advanceTotal` | nombre décimal |  |  |
| `netTotal` | nombre décimal |  |  |
| `paidTotal` | nombre décimal |  |  |
| `employeeCount` | entier |  |  |
| `errorsJson` | texte |  |  |
| `warningsJson` | texte |  |  |
| `entryNumber` | texte |  |  |
| `importedBy` | texte |  |  |
| `importedAt` | date-heure ISO 8601 UTC |  |  |
| `postedBy` | texte |  |  |
| `postedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-PayrollService-ImportView"></a>
#### PayrollService.ImportView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `payroll` | [AccPayrollImport](#m-AccPayrollImport) |  |  |
| `errors` | texte[] |  |  |
| `warnings` | texte[] |  |  |
| `employees` | [PayrollService.EmployeeTotals](#m-PayrollService-EmployeeTotals)[] |  |  |

<a id="m-AccPayrollLine"></a>
#### AccPayrollLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `importId` | entier int64 ⚠ |  |  |
| `lineNo` | entier |  |  |
| `matricule` | texte |  |  |
| `employeeName` | texte |  |  |
| `rubricCode` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `sectionCode` | texte |  |  |

<a id="m-PayrollService-PaymentRequest"></a>
#### PayrollService.PaymentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `treasuryAccount` | texte |  |  |
| `journalCode` | texte |  |  |
| `date` | date AAAA-MM-JJ |  |  |
| `matricules` | texte[] |  |  |

<a id="m-AccPayrollRubric"></a>
#### AccPayrollRubric

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `kind` | texte |  |  |
| `account` | texte |  |  |
| `counterAccount` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-AccPayrollTemplate"></a>
#### AccPayrollTemplate

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `separator` | texte |  |  |
| `headerRows` | entier |  |  |
| `employeeColumn` | texte |  |  |
| `nameColumn` | texte |  |  |
| `rubricColumn` | texte |  |  |
| `amountColumn` | texte |  |  |
| `sectionColumn` | texte |  |  |
| `decimalSeparator` | texte |  |  |
| `encoding` | texte |  |  |
| `createdBy` | texte |  |  |

<a id="m-AccLoanEvent"></a>
#### AccLoanEvent

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `loanId` | entier int64 ⚠ |  |  |
| `kind` | texte |  |  |
| `eventDate` | date AAAA-MM-JJ |  |  |
| `amount` | nombre décimal |  |  |
| `rate` | nombre décimal |  |  |
| `detail` | texte |  |  |
| `fromVersion` | entier |  |  |
| `toVersion` | entier |  |  |
| `entryNumber` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-PartnerAccountService-StatementLine"></a>
#### PartnerAccountService.StatementLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `entryNumber` | texte |  |  |
| `label` | texte |  |  |
| `reference` | texte |  |  |
| `debit` | nombre décimal |  |  |
| `credit` | nombre décimal |  |  |
| `balance` | nombre décimal |  |  |

<a id="m-PayrollService-EmployeeTotals"></a>
#### PayrollService.EmployeeTotals

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `matricule` | texte |  |  |
| `name` | texte |  |  |
| `thirdPartyCode` | texte |  |  |
| `section` | texte |  |  |
| `gross` | nombre décimal |  |  |
| `deductions` | nombre décimal |  |  |
| `advances` | nombre décimal |  |  |
| `net` | nombre décimal |  |  |
| `employerCharges` | nombre décimal |  |  |
| `difference` | nombre décimal |  |  |
