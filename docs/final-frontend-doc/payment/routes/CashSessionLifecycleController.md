# CashSessionLifecycleController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 23 route(s).

## Routes

<a id="r-get-sni-api-v1-cash-registers-denominations"></a>
### GET /sni/api/v1/cash-registers/denominations

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `List<CashDenominationResponse>`
- Modèles : [CashDenominationResponse](#m-CashDenominationResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionLifecycleController.java`, méthode `denominations`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `currency` | requête | `String` |  |  |
| `includeRetired` | requête | `boolean` |  | false |

<a id="r-put-sni-api-v1-cash-registers-denominations-currency"></a>
### PUT /sni/api/v1/cash-registers/denominations/{currency}

- Accès : back-office : rôle d'administration et droit `CASH_REGISTER:MANAGE`
- Corps JSON (obligatoire) : `List<CashDenominationRequest>`
- Réponse : `List<CashDenominationResponse>`
- Modèles : [CashDenominationResponse](#m-CashDenominationResponse), [CashDenominationRequest](#m-CashDenominationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionLifecycleController.java`, méthode `replaceDenominations`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `currency` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-handovers-handovernumber-accept"></a>
### PATCH /sni/api/v1/cash-registers/handovers/{handoverNumber}/accept

- Accès : back-office : rôle d'administration et droit `CASH_SESSION:OPEN`
- Corps JSON (facultatif) : `CashHandoverAnswerRequest`
- Réponse : `CashSessionStepResponse`
- Modèles : [CashSessionStepResponse](#m-CashSessionStepResponse), [CashHandoverAnswerRequest](#m-CashHandoverAnswerRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionLifecycleController.java`, méthode `acceptHandover`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `handoverNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-handovers-handovernumber-cancel"></a>
### PATCH /sni/api/v1/cash-registers/handovers/{handoverNumber}/cancel

- Accès : back-office : rôle d'administration et droit `CASH_SESSION:OPEN`
- Réponse : `CashHandoverResponse`
- Modèles : [CashHandoverResponse](#m-CashHandoverResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionLifecycleController.java`, méthode `cancelHandover`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `handoverNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-handovers-handovernumber-dispute"></a>
### PATCH /sni/api/v1/cash-registers/handovers/{handoverNumber}/dispute

- Accès : back-office : rôle d'administration et droit `CASH_SESSION:OPEN`
- Corps JSON (obligatoire) : `CashHandoverAnswerRequest`
- Réponse : `CashSessionStepResponse`
- Modèles : [CashSessionStepResponse](#m-CashSessionStepResponse), [CashHandoverAnswerRequest](#m-CashHandoverAnswerRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionLifecycleController.java`, méthode `disputeHandover`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `handoverNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-sessions-sessionnumber-cancel"></a>
### PATCH /sni/api/v1/cash-registers/sessions/{sessionNumber}/cancel

- Accès : back-office : rôle d'administration et droit `CASH_SESSION:CLOSE`
- Corps JSON (obligatoire) : `CashReasonRequest`
- Réponse : `CashSessionStepResponse`
- Modèles : [CashSessionStepResponse](#m-CashSessionStepResponse), [CashReasonRequest](#m-CashReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionLifecycleController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-sessions-sessionnumber-confirm-opening"></a>
### PATCH /sni/api/v1/cash-registers/sessions/{sessionNumber}/confirm-opening

- Accès : back-office : rôle d'administration et droit `CASH_SESSION:CLOSE`
- Corps JSON (facultatif) : `CashReasonRequest`
- Réponse : `CashSessionStepResponse`
- Modèles : [CashSessionStepResponse](#m-CashSessionStepResponse), [CashReasonRequest](#m-CashReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionLifecycleController.java`, méthode `confirmOpening`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-sessions-sessionnumber-contest"></a>
### PATCH /sni/api/v1/cash-registers/sessions/{sessionNumber}/contest

- Accès : back-office : rôle d'administration et droit `CASH_SESSION:CLOSE`
- Corps JSON (obligatoire) : `CashReasonRequest`
- Réponse : `CashSessionStepResponse`
- Modèles : [CashSessionStepResponse](#m-CashSessionStepResponse), [CashReasonRequest](#m-CashReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionLifecycleController.java`, méthode `contest`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-cash-registers-sessions-sessionnumber-counts"></a>
### GET /sni/api/v1/cash-registers/sessions/{sessionNumber}/counts

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `List<CashCountResponse>`
- Modèles : [CashCountResponse](#m-CashCountResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionLifecycleController.java`, méthode `counts`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-cash-registers-sessions-sessionnumber-counts"></a>
### POST /sni/api/v1/cash-registers/sessions/{sessionNumber}/counts

- Accès : back-office : rôle d'administration et droit `CASH_SESSION:CLOSE`
- Corps JSON (obligatoire) : `CashCountRequest`
- Réponse : `CashSessionStepResponse`
- Modèles : [CashSessionStepResponse](#m-CashSessionStepResponse), [CashCountRequest](#m-CashCountRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionLifecycleController.java`, méthode `submitCount`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-sessions-sessionnumber-force-close"></a>
### PATCH /sni/api/v1/cash-registers/sessions/{sessionNumber}/force-close

- Accès : back-office : rôle d'administration et droit `CASH_SESSION:FORCE_CLOSE`
- Corps JSON (obligatoire) : `CashSessionLifecycleController.ForceCloseRequest`
- Réponse : `CashSessionStepResponse`
- Modèles : [CashSessionStepResponse](#m-CashSessionStepResponse), [CashSessionLifecycleController.ForceCloseRequest](#m-CashSessionLifecycleController-ForceCloseRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionLifecycleController.java`, méthode `forceClose`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-cash-registers-sessions-sessionnumber-handover"></a>
### POST /sni/api/v1/cash-registers/sessions/{sessionNumber}/handover

- Accès : back-office : rôle d'administration et droit `CASH_SESSION:CLOSE`
- Corps JSON (obligatoire) : `CashHandoverRequest`
- Réponse : `CashHandoverResponse`
- Modèles : [CashHandoverResponse](#m-CashHandoverResponse), [CashHandoverRequest](#m-CashHandoverRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionLifecycleController.java`, méthode `handover`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-sessions-sessionnumber-pause"></a>
### PATCH /sni/api/v1/cash-registers/sessions/{sessionNumber}/pause

- Accès : back-office : rôle d'administration et droit `CASH_SESSION:PAUSE`
- Réponse : `CashSessionStepResponse`
- Modèles : [CashSessionStepResponse](#m-CashSessionStepResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionLifecycleController.java`, méthode `pause`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-sessions-sessionnumber-recount"></a>
### PATCH /sni/api/v1/cash-registers/sessions/{sessionNumber}/recount

- Accès : back-office : rôle d'administration et droit `CASH_VARIANCE:DECIDE`
- Corps JSON (facultatif) : `CashReasonRequest`
- Réponse : `CashSessionStepResponse`
- Modèles : [CashSessionStepResponse](#m-CashSessionStepResponse), [CashReasonRequest](#m-CashReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionLifecycleController.java`, méthode `recount`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-sessions-sessionnumber-resume"></a>
### PATCH /sni/api/v1/cash-registers/sessions/{sessionNumber}/resume

- Accès : back-office : rôle d'administration et droit `CASH_SESSION:PAUSE`
- Corps JSON (facultatif) : `ResumeCashSessionRequest`
- Réponse : `CashSessionStepResponse`
- Modèles : [CashSessionStepResponse](#m-CashSessionStepResponse), [ResumeCashSessionRequest](#m-ResumeCashSessionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionLifecycleController.java`, méthode `resume`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-cash-registers-sessions-sessionnumber-spot-checks"></a>
### POST /sni/api/v1/cash-registers/sessions/{sessionNumber}/spot-checks

- Accès : back-office : rôle d'administration et droit `CASH_SESSION:SPOT_CHECK`
- Corps JSON (obligatoire) : `CashCountRequest`
- Réponse : `CashSessionStepResponse`
- Modèles : [CashSessionStepResponse](#m-CashSessionStepResponse), [CashCountRequest](#m-CashCountRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionLifecycleController.java`, méthode `spotCheck`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-sessions-sessionnumber-start-closing"></a>
### PATCH /sni/api/v1/cash-registers/sessions/{sessionNumber}/start-closing

- Accès : back-office : rôle d'administration et droit `CASH_SESSION:CLOSE`
- Réponse : `CashSessionStepResponse`
- Modèles : [CashSessionStepResponse](#m-CashSessionStepResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionLifecycleController.java`, méthode `startClosing`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-sessions-sessionnumber-suspend"></a>
### PATCH /sni/api/v1/cash-registers/sessions/{sessionNumber}/suspend

- Accès : back-office : rôle d'administration et droit `CASH_SESSION:SUSPEND`
- Corps JSON (facultatif) : `CashReasonRequest`
- Réponse : `CashSessionStepResponse`
- Modèles : [CashSessionStepResponse](#m-CashSessionStepResponse), [CashReasonRequest](#m-CashReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionLifecycleController.java`, méthode `suspend`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-sessions-sessionnumber-unsuspend"></a>
### PATCH /sni/api/v1/cash-registers/sessions/{sessionNumber}/unsuspend

- Accès : back-office : rôle d'administration et droit `CASH_SESSION:SUSPEND`
- Réponse : `CashSessionStepResponse`
- Modèles : [CashSessionStepResponse](#m-CashSessionStepResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionLifecycleController.java`, méthode `unsuspend`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-sessions-sessionnumber-variance-decision"></a>
### PATCH /sni/api/v1/cash-registers/sessions/{sessionNumber}/variance-decision

- Accès : back-office : rôle d'administration et droit `CASH_VARIANCE:DECIDE`
- Corps JSON (facultatif) : `CashVarianceDecisionRequest`
- Réponse : `CashSessionStepResponse`
- Modèles : [CashSessionStepResponse](#m-CashSessionStepResponse), [CashVarianceDecisionRequest](#m-CashVarianceDecisionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionLifecycleController.java`, méthode `decideSessionVariance`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-cash-registers-variance-reviews"></a>
### GET /sni/api/v1/cash-registers/variance-reviews

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `List<CashVarianceReviewResponse>`
- Modèles : [CashVarianceReviewResponse](#m-CashVarianceReviewResponse), [CashVarianceStatus](#m-CashVarianceStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionLifecycleController.java`, méthode `reviews`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `CashVarianceStatus` |  |  |
| `registerCode` | requête | `String` |  |  |
| `sessionNumber` | requête | `String` |  |  |

<a id="r-patch-sni-api-v1-cash-registers-variance-reviews-reviewnumber-decision"></a>
### PATCH /sni/api/v1/cash-registers/variance-reviews/{reviewNumber}/decision

- Accès : back-office : rôle d'administration et droit `CASH_VARIANCE:DECIDE`
- Corps JSON (facultatif) : `CashVarianceDecisionRequest`
- Réponse : `CashSessionStepResponse`
- Modèles : [CashSessionStepResponse](#m-CashSessionStepResponse), [CashVarianceDecisionRequest](#m-CashVarianceDecisionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionLifecycleController.java`, méthode `decideReview`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `reviewNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-cash-registers-registercode-expected-opening"></a>
### GET /sni/api/v1/cash-registers/{registerCode}/expected-opening

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `CashExpectedOpeningResponse`
- Modèles : [CashExpectedOpeningResponse](#m-CashExpectedOpeningResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionLifecycleController.java`, méthode `expectedOpening`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CashDenominationResponse"></a>
#### CashDenominationResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `currency` | texte |  |  |
| `value` | nombre décimal |  |  |
| `kind` | enum : `BILL`, `COIN` |  |  |
| `label` | texte |  |  |
| `sortOrder` | entier |  |  |
| `activeFrom` | date AAAA-MM-JJ |  |  |
| `activeTo` | date AAAA-MM-JJ |  |  |
| `active` | booléen |  |  |

<a id="m-CashDenominationRequest"></a>
#### CashDenominationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `value` | nombre décimal | oui |  |
| `kind` | enum : `BILL`, `COIN` | oui |  |
| `label` | texte | oui | longueur min `1` |
| `sortOrder` | entier |  |  |
| `activeFrom` | date AAAA-MM-JJ |  |  |
| `activeTo` | date AAAA-MM-JJ |  |  |

<a id="m-CashSessionStepResponse"></a>
#### CashSessionStepResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `session` | [CashSessionResponse](#m-CashSessionResponse) |  |  |
| `count` | [CashCountResponse](#m-CashCountResponse) |  |  |
| `review` | [CashVarianceReviewResponse](#m-CashVarianceReviewResponse) |  |  |
| `messages` | texte[] |  |  |

<a id="m-CashHandoverAnswerRequest"></a>
#### CashHandoverAnswerRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `count` | [CashCountRequest](#m-CashCountRequest) |  |  |
| `reason` | texte |  |  |
| `deviceCode` | texte |  |  |

<a id="m-CashHandoverResponse"></a>
#### CashHandoverResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `handoverNumber` | texte |  |  |
| `registerCode` | texte |  |  |
| `fromSession` | texte |  |  |
| `toSession` | texte |  |  |
| `outgoing` | texte |  |  |
| `incoming` | texte |  |  |
| `outgoingCount` | texte |  |  |
| `incomingCount` | texte |  |  |
| `outgoingAmount` | nombre décimal |  |  |
| `incomingAmount` | nombre décimal |  |  |
| `status` | enum : `PENDING`, `ACCEPTED`, `DISPUTED`, `CANCELLED` |  |  |
| `disputeReason` | texte |  |  |
| `outgoingSignedAt` | date-heure ISO 8601 UTC |  |  |
| `incomingSignedAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CashReasonRequest"></a>
#### CashReasonRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reasonCode` | texte |  |  |
| `reasonText` | texte |  |  |
| `note` | texte |  |  |

<a id="m-CashCountResponse"></a>
#### CashCountResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `countNumber` | texte |  |  |
| `registerCode` | texte |  |  |
| `sessionNumber` | texte |  |  |
| `moment` | enum : `OPENING`, `CLOSING`, `RECOUNT`, `HANDOVER`, `SPOT_CHECK`, `TRANSFER_RECEIPT`, `PERIODIC`, `RESPONSIBILITY_TRANSFER`, `FORCED_CLOSE`, `SEAL`, `UNSEAL` |  |  |
| `currency` | texte |  |  |
| `total` | nombre décimal |  |  |
| `damagedTotal` | nombre décimal |  |  |
| `withLines` | booléen |  |  |
| `expectedAmount` | nombre décimal |  |  |
| `variance` | nombre décimal |  |  |
| `countedBy` | texte |  |  |
| `secondCounter` | texte |  |  |
| `witness` | texte |  |  |
| `status` | texte |  |  |
| `supersededBy` | texte |  |  |
| `referenceNumber` | texte |  |  |
| `note` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `lines` | [CashCountResponse.Line](#m-CashCountResponse-Line)[] |  |  |

<a id="m-CashCountRequest"></a>
#### CashCountRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `currency` | texte |  |  |
| `lines` | [CashCountRequest.Line](#m-CashCountRequest-Line)[] |  |  |
| `totalAmount` | nombre décimal |  |  |
| `secondCounterEmail` | texte |  |  |
| `witnessEmail` | texte |  |  |
| `keptFloat` | nombre décimal |  |  |
| `varianceReasonCode` | texte |  |  |
| `varianceReasonText` | texte |  |  |
| `note` | texte |  |  |

<a id="m-CashSessionLifecycleController-ForceCloseRequest"></a>
#### CashSessionLifecycleController.ForceCloseRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `count` | [CashCountRequest](#m-CashCountRequest) |  |  |
| `reason` | [CashReasonRequest](#m-CashReasonRequest) |  |  |

<a id="m-CashHandoverRequest"></a>
#### CashHandoverRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `incomingEmail` | texte | oui | longueur min `1` |
| `count` | [CashCountRequest](#m-CashCountRequest) | oui |  |

<a id="m-ResumeCashSessionRequest"></a>
#### ResumeCashSessionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `password` | texte |  |  |

<a id="m-CashVarianceDecisionRequest"></a>
#### CashVarianceDecisionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `decision` | enum : `EXPENSE`, `INCOME`, `CASHIER`, `INVESTIGATION`, `TOLERATED` |  |  |
| `note` | texte |  |  |
| `reviewDueAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CashVarianceReviewResponse"></a>
#### CashVarianceReviewResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reviewNumber` | texte |  |  |
| `registerCode` | texte |  |  |
| `sessionNumber` | texte |  |  |
| `kind` | enum : `OPENING`, `CLOSING`, `SPOT_CHECK`, `HANDOVER`, `FORCED_CLOSE`, `UNSEAL`, `TRANSFER`, `RESPONSIBILITY_TRANSFER` |  |  |
| `amount` | nombre décimal |  |  |
| `countNumber` | texte |  |  |
| `cashier` | texte |  |  |
| `reason` | texte |  |  |
| `status` | enum : `PENDING`, `DECIDED`, `TOLERATED` |  |  |
| `decision` | enum : `EXPENSE`, `INCOME`, `CASHIER`, `INVESTIGATION`, `TOLERATED` |  |  |
| `proposedDecision` | enum : `EXPENSE`, `INCOME`, `CASHIER`, `INVESTIGATION`, `TOLERATED` |  |  |
| `decidedBy` | texte |  |  |
| `decidedAt` | date-heure ISO 8601 UTC |  |  |
| `decisionNote` | texte |  |  |
| `contestDeadline` | date-heure ISO 8601 UTC |  |  |
| `contestedAt` | date-heure ISO 8601 UTC |  |  |
| `contestReason` | texte |  |  |
| `reviewDueAt` | date-heure ISO 8601 UTC |  |  |
| `decidableNow` | booléen |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CashVarianceStatus"></a>
#### CashVarianceStatus

Énumération : `PENDING`, `DECIDED`, `TOLERATED`

<a id="m-CashExpectedOpeningResponse"></a>
#### CashExpectedOpeningResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `registerCode` | texte |  |  |
| `expectedAmount` | nombre décimal |  |  |
| `lastCountAt` | date-heure ISO 8601 UTC |  |  |
| `neverCounted` | booléen |  |  |
| `standardFloat` | nombre décimal |  |  |
| `countLinesRequired` | booléen |  |  |
| `denominations` | [CashDenominationResponse](#m-CashDenominationResponse)[] |  |  |

<a id="m-CashSessionResponse"></a>
#### CashSessionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `sessionNumber` | texte |  |  |
| `registerCode` | texte |  |  |
| `status` | enum : `OPENING`, `OPEN`, `PAUSED`, `SUSPENDED`, `COUNTING`, `CLOSING_REVIEW`, `RECOUNT`, `CLOSED`, `CANCELLED` |  |  |
| `openedBy` | texte |  |  |
| `closedBy` | texte |  |  |
| `reviewedBy` | texte |  |  |
| `openingAmount` | nombre décimal |  |  |
| `closingAmount` | nombre décimal |  |  |
| `expectedClosingAmount` | nombre décimal |  |  |
| `countedClosingAmount` | nombre décimal |  |  |
| `varianceAmount` | nombre décimal |  |  |
| `varianceReason` | texte |  |  |
| `openedAt` | date-heure ISO 8601 UTC |  |  |
| `closingRequestedAt` | date-heure ISO 8601 UTC |  |  |
| `closedAt` | date-heure ISO 8601 UTC |  |  |
| `expectedOpeningAmount` | nombre décimal |  |  |
| `openingVariance` | nombre décimal |  |  |
| `openingVarianceReason` | texte |  |  |
| `pausedAt` | date-heure ISO 8601 UTC |  |  |
| `pauseSeconds` | entier int64 ⚠ |  |  |
| `suspendedBy` | texte |  |  |
| `suspensionReason` | texte |  |  |
| `keptFloat` | nombre décimal |  |  |
| `amountToRemit` | nombre décimal |  |  |
| `closingCountNumber` | texte |  |  |
| `varianceDecision` | enum : `EXPENSE`, `INCOME`, `CASHIER`, `INVESTIGATION`, `TOLERATED` |  |  |
| `varianceDecidedBy` | texte |  |  |
| `forcedClose` | booléen |  |  |
| `handoverNumber` | texte |  |  |
| `outOfHoursReason` | texte |  |  |
| `expectedHidden` | booléen |  |  |

<a id="m-CashCountResponse-Line"></a>
#### CashCountResponse.Line

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `value` | nombre décimal |  |  |
| `kind` | enum : `BILL`, `COIN` |  |  |
| `quantity` | entier |  |  |
| `damagedQuantity` | entier |  |  |
| `amount` | nombre décimal |  |  |

<a id="m-CashCountRequest-Line"></a>
#### CashCountRequest.Line

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `value` | nombre décimal |  |  |
| `kind` | enum : `BILL`, `COIN` |  |  |
| `quantity` | entier |  |  |
| `damagedQuantity` | entier |  |  |
