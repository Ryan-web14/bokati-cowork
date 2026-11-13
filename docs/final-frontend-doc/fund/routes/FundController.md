# FundController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [fund](../README.md) · 39 route(s).

## Routes

<a id="r-post-sni-api-v1-fund-needs"></a>
### POST /sni/api/v1/fund-needs

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Corps JSON (obligatoire) : `FundDtos.NeedRequest`
- Réponse : `FundDtos.Need`
- Modèles : [FundDtos.Need](#m-FundDtos-Need), [FundDtos.NeedRequest](#m-FundDtos-NeedRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `createNeed`

<a id="r-get-sni-api-v1-fund-needs-mine"></a>
### GET /sni/api/v1/fund-needs/mine

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Réponse : `List<FundDtos.Need>`
- Modèles : [FundDtos.Need](#m-FundDtos-Need)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `myNeeds`

<a id="r-get-sni-api-v1-fund-needs-submitted"></a>
### GET /sni/api/v1/fund-needs/submitted

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Réponse : `List<FundDtos.Need>`
- Modèles : [FundDtos.Need](#m-FundDtos-Need)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `submittedNeeds`

<a id="r-get-sni-api-v1-fund-needs-number"></a>
### GET /sni/api/v1/fund-needs/{number}

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Réponse : `FundDtos.Need`
- Modèles : [FundDtos.Need](#m-FundDtos-Need)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `need`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-fund-needs-number"></a>
### PUT /sni/api/v1/fund-needs/{number}

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Corps JSON (obligatoire) : `FundDtos.NeedRequest`
- Réponse : `FundDtos.Need`
- Modèles : [FundDtos.Need](#m-FundDtos-Need), [FundDtos.NeedRequest](#m-FundDtos-NeedRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `updateNeed`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-fund-needs-number-reject"></a>
### PATCH /sni/api/v1/fund-needs/{number}/reject

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:APPROVE_L1`
- Corps JSON (obligatoire) : `FundDtos.Decision`
- Réponse : `FundDtos.Need`
- Modèles : [FundDtos.Need](#m-FundDtos-Need), [FundDtos.Decision](#m-FundDtos-Decision)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `rejectNeed`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-fund-needs-number-submit"></a>
### PATCH /sni/api/v1/fund-needs/{number}/submit

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Réponse : `FundDtos.Need`
- Modèles : [FundDtos.Need](#m-FundDtos-Need)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `submitNeed`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-fund-needs-number-supplier-suggestions"></a>
### GET /sni/api/v1/fund-needs/{number}/supplier-suggestions

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Réponse : `List<SupplierSuggestionDtos.LineSuggestion>`
- Modèles : [SupplierSuggestionDtos.LineSuggestion](#m-SupplierSuggestionDtos-LineSuggestion)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `needSupplierSuggestions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |
| `limit` | requête | `Integer` |  |  |

<a id="r-post-sni-api-v1-fund-needs-number-transform"></a>
### POST /sni/api/v1/fund-needs/{number}/transform

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Corps JSON (obligatoire) : `FundDtos.TransformNeed`
- Réponse : `FundDtos.Request`
- Modèles : [FundDtos.Request](#m-FundDtos-Request), [FundDtos.TransformNeed](#m-FundDtos-TransformNeed)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `transform`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-fund-needs-number-validate"></a>
### PATCH /sni/api/v1/fund-needs/{number}/validate

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:APPROVE_L1`
- Corps JSON (facultatif) : `FundDtos.Decision`
- Réponse : `FundDtos.Need`
- Modèles : [FundDtos.Need](#m-FundDtos-Need), [FundDtos.Decision](#m-FundDtos-Decision)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `validateNeed`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-fund-requests"></a>
### GET /sni/api/v1/fund-requests

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:READ`
- Réponse : `List<FundDtos.Request>`
- Modèles : [FundDtos.Request](#m-FundDtos-Request), [FundRequestStatus](#m-FundRequestStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `all`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `FundRequestStatus` |  |  |

<a id="r-post-sni-api-v1-fund-requests"></a>
### POST /sni/api/v1/fund-requests

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Corps JSON (obligatoire) : `FundDtos.RequestInput`
- Réponse : `FundDtos.Request`
- Modèles : [FundDtos.Request](#m-FundDtos-Request), [FundDtos.RequestInput](#m-FundDtos-RequestInput)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `create`

<a id="r-get-sni-api-v1-fund-requests-advances"></a>
### GET /sni/api/v1/fund-requests/advances

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:READ`
- Réponse : `List<FundDtos.AdvanceRow>`
- Modèles : [FundDtos.AdvanceRow](#m-FundDtos-AdvanceRow)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `advances`

<a id="r-get-sni-api-v1-fund-requests-delegations"></a>
### GET /sni/api/v1/fund-requests/delegations

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Réponse : `List<FundDtos.DelegationView>`
- Modèles : [FundDtos.DelegationView](#m-FundDtos-DelegationView)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `delegations`

<a id="r-post-sni-api-v1-fund-requests-delegations"></a>
### POST /sni/api/v1/fund-requests/delegations

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Corps JSON (obligatoire) : `FundDtos.Delegation`
- Réponse : `FundDtos.DelegationView`
- Modèles : [FundDtos.DelegationView](#m-FundDtos-DelegationView), [FundDtos.Delegation](#m-FundDtos-Delegation)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `delegate`

<a id="r-delete-sni-api-v1-fund-requests-delegations-id"></a>
### DELETE /sni/api/v1/fund-requests/delegations/{id}

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `cancelDelegation`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-fund-requests-import-legacy"></a>
### POST /sni/api/v1/fund-requests/import-legacy

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:READ`
- Réponse : `Map<String, Integer>`
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `importLegacy`

<a id="r-get-sni-api-v1-fund-requests-justifications-to-validate"></a>
### GET /sni/api/v1/fund-requests/justifications-to-validate

- Accès : back-office : rôle d'administration et droit `FUND_JUSTIFICATION:VALIDATE`
- Réponse : `List<FundDtos.Request>`
- Modèles : [FundDtos.Request](#m-FundDtos-Request)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `justificationsToValidate`

<a id="r-get-sni-api-v1-fund-requests-mine"></a>
### GET /sni/api/v1/fund-requests/mine

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Réponse : `List<FundDtos.Request>`
- Modèles : [FundDtos.Request](#m-FundDtos-Request)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `mine`

<a id="r-get-sni-api-v1-fund-requests-to-approve"></a>
### GET /sni/api/v1/fund-requests/to-approve

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Réponse : `List<FundDtos.Request>`
- Modèles : [FundDtos.Request](#m-FundDtos-Request)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `toApprove`

<a id="r-get-sni-api-v1-fund-requests-to-disburse"></a>
### GET /sni/api/v1/fund-requests/to-disburse

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:DISBURSE`
- Réponse : `List<FundDtos.Request>`
- Modèles : [FundDtos.Request](#m-FundDtos-Request)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `toDisburse`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-fund-requests-number"></a>
### GET /sni/api/v1/fund-requests/{number}

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Réponse : `FundDtos.Request`
- Modèles : [FundDtos.Request](#m-FundDtos-Request)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-fund-requests-number"></a>
### PUT /sni/api/v1/fund-requests/{number}

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Corps JSON (obligatoire) : `FundDtos.RequestInput`
- Réponse : `FundDtos.Request`
- Modèles : [FundDtos.Request](#m-FundDtos-Request), [FundDtos.RequestInput](#m-FundDtos-RequestInput)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-fund-requests-number-approve"></a>
### PATCH /sni/api/v1/fund-requests/{number}/approve

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Corps JSON (facultatif) : `FundDtos.Decision`
- Réponse : `FundDtos.Request`
- Modèles : [FundDtos.Request](#m-FundDtos-Request), [FundDtos.Decision](#m-FundDtos-Decision)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `approve`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-fund-requests-number-cancel"></a>
### PATCH /sni/api/v1/fund-requests/{number}/cancel

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Corps JSON (facultatif) : `FundDtos.Decision`
- Réponse : `FundDtos.Request`
- Modèles : [FundDtos.Request](#m-FundDtos-Request), [FundDtos.Decision](#m-FundDtos-Decision)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-fund-requests-number-complement"></a>
### POST /sni/api/v1/fund-requests/{number}/complement

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:DISBURSE`
- Corps JSON (obligatoire) : `FundDtos.CashBack`
- Réponse : `FundDtos.Request`
- Modèles : [FundDtos.Request](#m-FundDtos-Request), [FundDtos.CashBack](#m-FundDtos-CashBack)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `complement`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-fund-requests-number-disburse"></a>
### POST /sni/api/v1/fund-requests/{number}/disburse

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:DISBURSE`
- Corps JSON (obligatoire) : `FundDtos.Disburse`
- Réponse : `FundDtos.Request`
- Modèles : [FundDtos.Request](#m-FundDtos-Request), [FundDtos.Disburse](#m-FundDtos-Disburse)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `disburse`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-fund-requests-number-duplicate"></a>
### POST /sni/api/v1/fund-requests/{number}/duplicate

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Réponse : `FundDtos.Request`
- Modèles : [FundDtos.Request](#m-FundDtos-Request)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `duplicate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-fund-requests-number-expenses"></a>
### POST /sni/api/v1/fund-requests/{number}/expenses

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Corps JSON (obligatoire) : `FundDtos.ExpenseInput`
- Réponse : `FundDtos.Request`
- Modèles : [FundDtos.Request](#m-FundDtos-Request), [FundDtos.ExpenseInput](#m-FundDtos-ExpenseInput)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `addExpense`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-fund-requests-number-expenses-line"></a>
### DELETE /sni/api/v1/fund-requests/{number}/expenses/{line}

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Réponse : `FundDtos.Request`
- Modèles : [FundDtos.Request](#m-FundDtos-Request)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `deleteExpense`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |
| `line` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-fund-requests-number-expenses-line-reject"></a>
### PATCH /sni/api/v1/fund-requests/{number}/expenses/{line}/reject

- Accès : back-office : rôle d'administration et droit `FUND_JUSTIFICATION:VALIDATE`
- Corps JSON (obligatoire) : `FundDtos.Reject`
- Réponse : `FundDtos.Request`
- Modèles : [FundDtos.Request](#m-FundDtos-Request), [FundDtos.Reject](#m-FundDtos-Reject)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `rejectExpense`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |
| `line` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-fund-requests-number-expenses-line-validate"></a>
### PATCH /sni/api/v1/fund-requests/{number}/expenses/{line}/validate

- Accès : back-office : rôle d'administration et droit `FUND_JUSTIFICATION:VALIDATE`
- Réponse : `FundDtos.Request`
- Modèles : [FundDtos.Request](#m-FundDtos-Request)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `validateExpense`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |
| `line` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-fund-requests-number-insight"></a>
### GET /sni/api/v1/fund-requests/{number}/insight

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Réponse : `FundDtos.Insight`
- Modèles : [FundDtos.Insight](#m-FundDtos-Insight)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `insight`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-fund-requests-number-reject"></a>
### PATCH /sni/api/v1/fund-requests/{number}/reject

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Corps JSON (obligatoire) : `FundDtos.Decision`
- Réponse : `FundDtos.Request`
- Modèles : [FundDtos.Request](#m-FundDtos-Request), [FundDtos.Decision](#m-FundDtos-Decision)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `reject`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-fund-requests-number-return"></a>
### POST /sni/api/v1/fund-requests/{number}/return

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:DISBURSE`
- Corps JSON (obligatoire) : `FundDtos.CashBack`
- Réponse : `FundDtos.Request`
- Modèles : [FundDtos.Request](#m-FundDtos-Request), [FundDtos.CashBack](#m-FundDtos-CashBack)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `returnRemainder`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-fund-requests-number-send-back"></a>
### PATCH /sni/api/v1/fund-requests/{number}/send-back

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Corps JSON (obligatoire) : `FundDtos.Decision`
- Réponse : `FundDtos.Request`
- Modèles : [FundDtos.Request](#m-FundDtos-Request), [FundDtos.Decision](#m-FundDtos-Decision)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `sendBack`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-fund-requests-number-submit"></a>
### PATCH /sni/api/v1/fund-requests/{number}/submit

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Réponse : `FundDtos.Request`
- Modèles : [FundDtos.Request](#m-FundDtos-Request)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `submit`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-fund-requests-number-supplier-suggestions"></a>
### GET /sni/api/v1/fund-requests/{number}/supplier-suggestions

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Réponse : `List<SupplierSuggestionDtos.LineSuggestion>`
- Modèles : [SupplierSuggestionDtos.LineSuggestion](#m-SupplierSuggestionDtos-LineSuggestion)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `requestSupplierSuggestions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |
| `limit` | requête | `Integer` |  |  |

<a id="r-patch-sni-api-v1-fund-requests-number-transfer"></a>
### PATCH /sni/api/v1/fund-requests/{number}/transfer

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:DISBURSE`
- Corps JSON (obligatoire) : `FundDtos.LinkTransfer`
- Réponse : `FundDtos.Request`
- Modèles : [FundDtos.Request](#m-FundDtos-Request), [FundDtos.LinkTransfer](#m-FundDtos-LinkTransfer)
- Code : `src/main/java/com/sni/bokaticowork/features/fund/controller/FundController.java`, méthode `linkTransfer`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-FundDtos-Need"></a>
#### FundDtos.Need

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `number` | texte |  |  |
| `requester` | texte |  |  |
| `department` | texte |  |  |
| `locationCode` | texte |  |  |
| `costCenter` | texte |  |  |
| `subject` | texte |  |  |
| `urgency` | enum : `NORMAL`, `URGENT`, `CRITICAL` |  |  |
| `wantedDate` | date AAAA-MM-JJ |  |  |
| `status` | enum : `DRAFT`, `SUBMITTED`, `VALIDATED`, `REJECTED`, `PARTIALLY_TRANSFORMED`, `TRANSFORMED` |  |  |
| `totalAmount` | nombre décimal |  |  |
| `totalInWords` | texte |  |  |
| `submittedAt` | date-heure ISO 8601 UTC |  |  |
| `decidedBy` | texte |  |  |
| `decidedAt` | date-heure ISO 8601 UTC |  |  |
| `decisionNote` | texte |  |  |
| `attachments` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `lines` | [FundDtos.NeedLine](#m-FundDtos-NeedLine)[] |  |  |

<a id="m-FundDtos-NeedRequest"></a>
#### FundDtos.NeedRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subject` | texte | oui | longueur min `1` |
| `department` | texte |  |  |
| `locationCode` | texte |  |  |
| `costCenter` | texte |  |  |
| `urgency` | enum : `NORMAL`, `URGENT`, `CRITICAL` |  |  |
| `wantedDate` | date AAAA-MM-JJ |  |  |
| `lines` | [FundDtos.NeedLineRequest](#m-FundDtos-NeedLineRequest)[] | oui | éléments min `1` |
| `attachments` | texte |  |  |

<a id="m-FundDtos-Decision"></a>
#### FundDtos.Decision

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `comment` | texte |  |  |
| `reducedAmount` | nombre décimal |  |  |

<a id="m-SupplierSuggestionDtos-LineSuggestion"></a>
#### SupplierSuggestionDtos.LineSuggestion

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `lineNo` | entier |  |  |
| `designation` | texte |  |  |
| `natureCode` | texte |  |  |
| `supplierCode` | texte |  |  |
| `supplier` | texte |  |  |
| `result` | [SupplierSuggestionDtos.SuggestionResult](#m-SupplierSuggestionDtos-SuggestionResult) |  |  |

<a id="m-FundDtos-Request"></a>
#### FundDtos.Request

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `number` | texte |  |  |
| `type` | enum : `DIRECT_EXPENSE`, `ADVANCE`, `EXPENSE_REFUND`, `SUPPLIER_PAYMENT`, `STAFF_ADVANCE`, `CASH_SUPPLY`, `FUND_REMITTANCE` |  |  |
| `status` | enum : `DRAFT`, `SUBMITTED`, `IN_APPROVAL`, `APPROVED`, `PARTIALLY_DISBURSED`, `DISBURSED`, `TO_JUSTIFY`, `PARTIALLY_JUSTIFIED`, `JUSTIFIED`, `SETTLED`, `REJECTED`, `CANCELLED`, `EXPIRED` |  |  |
| `requester` | texte |  |  |
| `requesterDepartment` | texte |  |  |
| `requesterPhone` | texte |  |  |
| `beneficiaryType` | enum : `SELF`, `EMPLOYEE`, `THIRD_PARTY` |  |  |
| `beneficiaryCode` | texte |  |  |
| `beneficiaryName` | texte |  |  |
| `beneficiaryIdNumber` | texte |  |  |
| `beneficiaryPhone` | texte |  |  |
| `registerCode` | texte |  |  |
| `subject` | texte |  |  |
| `totalAmount` | nombre décimal |  |  |
| `totalInWords` | texte |  |  |
| `approvedAmount` | nombre décimal |  |  |
| `disbursedAmount` | nombre décimal |  |  |
| `justifiedAmount` | nombre décimal |  |  |
| `returnedAmount` | nombre décimal |  |  |
| `complementAmount` | nombre décimal |  |  |
| `remainingToJustify` | nombre décimal |  |  |
| `remainingToDisburse` | nombre décimal |  |  |
| `deliveryMode` | enum : `CASH`, `MOBILE_MONEY`, `TRANSFER` |  |  |
| `wantedDate` | date AAAA-MM-JJ |  |  |
| `missionDestination` | texte |  |  |
| `missionStart` | date AAAA-MM-JJ |  |  |
| `missionEnd` | date AAAA-MM-JJ |  |  |
| `unplanned` | booléen |  |  |
| `unplannedReason` | texte |  |  |
| `needStatementNumber` | texte |  |  |
| `transferOrderNumber` | texte |  |  |
| `requiredLevels` | entier |  |  |
| `currentLevel` | entier |  |  |
| `lastComment` | texte |  |  |
| `submittedAt` | date-heure ISO 8601 UTC |  |  |
| `approvedAt` | date-heure ISO 8601 UTC |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |
| `disbursedAt` | date-heure ISO 8601 UTC |  |  |
| `justificationDueDate` | date AAAA-MM-JJ |  |  |
| `settledAt` | date-heure ISO 8601 UTC |  |  |
| `legacyRequestNumber` | texte |  |  |
| `attachments` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `lines` | [FundDtos.RequestLine](#m-FundDtos-RequestLine)[] |  |  |
| `timeline` | [FundDtos.Approval](#m-FundDtos-Approval)[] |  |  |
| `disbursements` | [FundDtos.Disbursement](#m-FundDtos-Disbursement)[] |  |  |
| `justification` | [FundDtos.ExpenseLine](#m-FundDtos-ExpenseLine)[] |  |  |
| `insight` | [FundDtos.Insight](#m-FundDtos-Insight) |  |  |

<a id="m-FundDtos-TransformNeed"></a>
#### FundDtos.TransformNeed

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | enum : `DIRECT_EXPENSE`, `ADVANCE`, `EXPENSE_REFUND`, `SUPPLIER_PAYMENT`, `STAFF_ADVANCE`, `CASH_SUPPLY`, `FUND_REMITTANCE` | oui |  |
| `lineNos` | entier[] |  |  |
| `registerCode` | texte |  |  |

<a id="m-FundRequestStatus"></a>
#### FundRequestStatus

Énumération : `DRAFT`, `SUBMITTED`, `IN_APPROVAL`, `APPROVED`, `PARTIALLY_DISBURSED`, `DISBURSED`, `TO_JUSTIFY`, `PARTIALLY_JUSTIFIED`, `JUSTIFIED`, `SETTLED`, `REJECTED`, `CANCELLED`, `EXPIRED`

<a id="m-FundDtos-RequestInput"></a>
#### FundDtos.RequestInput

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | enum : `DIRECT_EXPENSE`, `ADVANCE`, `EXPENSE_REFUND`, `SUPPLIER_PAYMENT`, `STAFF_ADVANCE`, `CASH_SUPPLY`, `FUND_REMITTANCE` | oui |  |
| `subject` | texte | oui | longueur min `1` |
| `requesterDepartment` | texte |  |  |
| `requesterPhone` | texte |  |  |
| `beneficiaryType` | enum : `SELF`, `EMPLOYEE`, `THIRD_PARTY` |  |  |
| `beneficiaryCode` | texte |  |  |
| `beneficiaryName` | texte |  |  |
| `beneficiaryIdNumber` | texte |  |  |
| `beneficiaryPhone` | texte |  |  |
| `registerCode` | texte |  |  |
| `deliveryMode` | enum : `CASH`, `MOBILE_MONEY`, `TRANSFER` |  |  |
| `wantedDate` | date AAAA-MM-JJ |  |  |
| `missionDestination` | texte |  |  |
| `missionStart` | date AAAA-MM-JJ |  |  |
| `missionEnd` | date AAAA-MM-JJ |  |  |
| `unplanned` | booléen |  |  |
| `unplannedReason` | texte |  |  |
| `lines` | [FundDtos.RequestLine](#m-FundDtos-RequestLine)[] | oui | éléments min `1` |
| `attachments` | texte |  |  |
| `submit` | booléen |  |  |
| `forecastNumber` | texte |  |  |
| `unplannedCode` | texte |  |  |

<a id="m-FundDtos-AdvanceRow"></a>
#### FundDtos.AdvanceRow

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `number` | texte |  |  |
| `beneficiaryCode` | texte |  |  |
| `beneficiaryName` | texte |  |  |
| `disbursed` | nombre décimal |  |  |
| `justified` | nombre décimal |  |  |
| `returned` | nombre décimal |  |  |
| `remaining` | nombre décimal |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `daysOverdue` | entier int64 ⚠ |  |  |
| `status` | enum : `DRAFT`, `SUBMITTED`, `IN_APPROVAL`, `APPROVED`, `PARTIALLY_DISBURSED`, `DISBURSED`, `TO_JUSTIFY`, `PARTIALLY_JUSTIFIED`, `JUSTIFIED`, `SETTLED`, `REJECTED`, `CANCELLED`, `EXPIRED` |  |  |

<a id="m-FundDtos-DelegationView"></a>
#### FundDtos.DelegationView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `delegator` | texte |  |  |
| `delegate` | texte |  |  |
| `level` | entier |  |  |
| `startsOn` | date AAAA-MM-JJ |  |  |
| `endsOn` | date AAAA-MM-JJ |  |  |

<a id="m-FundDtos-Delegation"></a>
#### FundDtos.Delegation

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `delegate` | texte | oui | longueur min `1` |
| `level` | entier | oui |  |
| `startsOn` | date AAAA-MM-JJ | oui |  |
| `endsOn` | date AAAA-MM-JJ | oui |  |

<a id="m-FundDtos-CashBack"></a>
#### FundDtos.CashBack

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `sessionNumber` | texte | oui | longueur min `1` |
| `amount` | nombre décimal |  |  |
| `signedByName` | texte |  |  |
| `signature` | texte |  |  |

<a id="m-FundDtos-Disburse"></a>
#### FundDtos.Disburse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `sessionNumber` | texte | oui | longueur min `1` |
| `amount` | nombre décimal |  |  |
| `signedByName` | texte |  |  |
| `signature` | texte |  |  |

<a id="m-FundDtos-ExpenseInput"></a>
#### FundDtos.ExpenseInput

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `expenseDate` | date AAAA-MM-JJ | oui |  |
| `natureCode` | texte | oui | longueur min `1` |
| `supplier` | texte |  |  |
| `designation` | texte | oui | longueur min `1` |
| `amount` | nombre décimal | oui |  |
| `vatAmount` | nombre décimal |  |  |
| `section` | texte |  |  |
| `documentReference` | texte |  |  |
| `honourStatement` | booléen |  |  |
| `requestLineNo` | entier |  |  |
| `supplierCode` | texte |  |  |

<a id="m-FundDtos-Reject"></a>
#### FundDtos.Reject

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte | oui | longueur min `1` |

<a id="m-FundDtos-Insight"></a>
#### FundDtos.Insight

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `requiredLevels` | entier |  |  |
| `levelLabels` | texte[] |  |  |
| `registerBalance` | nombre décimal |  |  |
| `registerCanCover` | booléen |  |  |
| `openAdvances` | texte[] |  |  |
| `blockedByOverdueAdvance` | booléen |  |  |
| `warning` | texte |  |  |
| `budget` | [BudgetControl.Impact](#m-BudgetControl-Impact) |  |  |
| `budgetApproval` | texte |  |  |

<a id="m-FundDtos-LinkTransfer"></a>
#### FundDtos.LinkTransfer

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `transferOrderNumber` | texte | oui | longueur min `1` |

<a id="m-FundDtos-NeedLine"></a>
#### FundDtos.NeedLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `lineNo` | entier |  |  |
| `designation` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `unit` | texte |  |  |
| `unitPrice` | nombre décimal |  |  |
| `totalAmount` | nombre décimal |  |  |
| `natureCode` | texte |  |  |
| `supplier` | texte |  |  |
| `transformedRequestNumber` | texte |  |  |
| `supplierCode` | texte |  |  |

<a id="m-FundDtos-NeedLineRequest"></a>
#### FundDtos.NeedLineRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `designation` | texte | oui | longueur min `1` |
| `quantity` | nombre décimal |  |  |
| `unit` | texte |  |  |
| `unitPrice` | nombre décimal |  |  |
| `natureCode` | texte |  |  |
| `supplier` | texte |  |  |
| `supplierCode` | texte |  |  |

<a id="m-SupplierSuggestionDtos-SuggestionResult"></a>
#### SupplierSuggestionDtos.SuggestionResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `natureCode` | texte |  |  |
| `natureLabel` | texte |  |  |
| `natureGuessed` | booléen |  |  |
| `inventoryCategoryCode` | texte |  |  |
| `supplierCategoryCode` | texte |  |  |
| `itemCode` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `limit` | entier |  |  |
| `suggestions` | [SupplierSuggestionDtos.Suggestion](#m-SupplierSuggestionDtos-Suggestion)[] |  |  |

<a id="m-FundDtos-RequestLine"></a>
#### FundDtos.RequestLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `natureCode` | texte |  |  |
| `designation` | texte | oui | longueur min `1` |
| `amount` | nombre décimal | oui |  |
| `supplier` | texte |  |  |
| `section` | texte |  |  |
| `supplierCode` | texte |  |  |

<a id="m-FundDtos-Approval"></a>
#### FundDtos.Approval

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `level` | entier |  |  |
| `approver` | texte |  |  |
| `decision` | enum : `SUBMITTED`, `APPROVED`, `REJECTED`, `RETURNED`, `REDUCED`, `CANCELLED`, `EXPIRED` |  |  |
| `amountBefore` | nombre décimal |  |  |
| `amountAfter` | nombre décimal |  |  |
| `comment` | texte |  |  |
| `delegatedFrom` | texte |  |  |
| `decidedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-FundDtos-Disbursement"></a>
#### FundDtos.Disbursement

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `kind` | enum : `DISBURSEMENT`, `COMPLEMENT`, `RETURN` |  |  |
| `movementNumber` | texte |  |  |
| `sessionNumber` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `signedByName` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-FundDtos-ExpenseLine"></a>
#### FundDtos.ExpenseLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `number` | texte |  |  |
| `lineNo` | entier |  |  |
| `expenseDate` | date AAAA-MM-JJ |  |  |
| `natureCode` | texte |  |  |
| `supplier` | texte |  |  |
| `designation` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `vatAmount` | nombre décimal |  |  |
| `section` | texte |  |  |
| `documentReference` | texte |  |  |
| `honourStatement` | booléen |  |  |
| `requestLineNo` | entier |  |  |
| `status` | enum : `PENDING`, `VALIDATED`, `REJECTED` |  |  |
| `rejectReason` | texte |  |  |
| `enteredBy` | texte |  |  |
| `validatedBy` | texte |  |  |
| `validatedAt` | date-heure ISO 8601 UTC |  |  |
| `supplierCode` | texte |  |  |

<a id="m-BudgetControl-Impact"></a>
#### BudgetControl.Impact

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `budgetActive` | booléen |  |  |
| `lines` | [BudgetControl.LineImpact](#m-BudgetControl-LineImpact)[] |  |  |
| `blocked` | booléen |  |  |
| `unbudgeted` | texte[] |  |  |
| `messages` | texte[] |  |  |

<a id="m-SupplierSuggestionDtos-Suggestion"></a>
#### SupplierSuggestionDtos.Suggestion

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `rank` | entier |  |  |
| `supplierCode` | texte |  |  |
| `supplierName` | texte |  |  |
| `phone` | texte |  |  |
| `email` | texte |  |  |
| `score` | entier |  |  |
| `reasons` | texte[] |  |  |
| `declared` | booléen |  |  |
| `historyCount` | entier int64 ⚠ |  |  |
| `contractUnitPrice` | entier int64 ⚠ |  |  |
| `leadTimeDays` | entier |  |  |
| `categories` | [SupplierSuggestionDtos.SupplierCategoryView](#m-SupplierSuggestionDtos-SupplierCategoryView)[] |  |  |

<a id="m-BudgetControl-LineImpact"></a>
#### BudgetControl.LineImpact

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `lineId` | entier int64 ⚠ |  |  |
| `itemCode` | texte |  |  |
| `itemLabel` | texte |  |  |
| `sectionCode` | texte |  |  |
| `controlLevel` | texte |  |  |
| `budgeted` | nombre décimal |  |  |
| `committed` | nombre décimal |  |  |
| `realized` | nombre décimal |  |  |
| `available` | nombre décimal |  |  |
| `requested` | nombre décimal |  |  |
| `availableAfter` | nombre décimal |  |  |
| `status` | enum : `GREEN`, `ORANGE`, `RED` |  |  |
| `openForecasts` | texte[] |  |  |

<a id="m-SupplierSuggestionDtos-SupplierCategoryView"></a>
#### SupplierSuggestionDtos.SupplierCategoryView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `kind` | enum : `NATURE`, `INVENTORY_CATEGORY`, `SUPPLIER_CATEGORY` |  |  |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `active` | booléen |  |  |
