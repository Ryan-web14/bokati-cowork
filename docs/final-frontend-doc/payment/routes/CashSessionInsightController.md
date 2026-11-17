# CashSessionInsightController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 5 route(s).

## Routes

<a id="r-get-sni-api-v1-cash-registers-sessions-sessionnumber-indicators"></a>
### GET /sni/api/v1/cash-registers/sessions/{sessionNumber}/indicators

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `CashSessionInsightService.Indicators`
- Modèles : [CashSessionInsightService.Indicators](#m-CashSessionInsightService-Indicators)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionInsightController.java`, méthode `indicators`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-cash-registers-sessions-sessionnumber-payments"></a>
### GET /sni/api/v1/cash-registers/sessions/{sessionNumber}/payments

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `CashSessionInsightService.SessionPayments`
- Modèles : [CashSessionInsightService.SessionPayments](#m-CashSessionInsightService-SessionPayments)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionInsightController.java`, méthode `payments`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |
| `paymentMethod` | requête | `String` |  |  |
| `receivedBy` | requête | `String` |  |  |

<a id="r-patch-sni-api-v1-cash-registers-sessions-sessionnumber-payments-transactionnumber-reassign"></a>
### PATCH /sni/api/v1/cash-registers/sessions/{sessionNumber}/payments/{transactionNumber}/reassign

- Accès : back-office : rôle d'administration et droit `CASH_VARIANCE:DECIDE`
- Corps JSON (obligatoire) : `CashSessionInsightController.ReassignRequest`
- Réponse : `CashRegisterService.ReassignedPayment`
- Modèles : [CashRegisterService.ReassignedPayment](#m-CashRegisterService-ReassignedPayment), [CashSessionInsightController.ReassignRequest](#m-CashSessionInsightController-ReassignRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionInsightController.java`, méthode `reassign`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |
| `transactionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-cash-registers-sessions-sessionnumber-report-send"></a>
### POST /sni/api/v1/cash-registers/sessions/{sessionNumber}/report/send

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Corps JSON (facultatif) : `CashSessionInsightController.SendReportRequest`
- Réponse : `CashSessionInsightController.SentReport`
- Modèles : [CashSessionInsightController.SentReport](#m-CashSessionInsightController-SentReport), [CashSessionInsightController.SendReportRequest](#m-CashSessionInsightController-SendReportRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionInsightController.java`, méthode `sendReport`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-cash-registers-sessions-sessionnumber-timeline"></a>
### GET /sni/api/v1/cash-registers/sessions/{sessionNumber}/timeline

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `List<CashSessionInsightService.TimelineEvent>`
- Modèles : [CashSessionInsightService.TimelineEvent](#m-CashSessionInsightService-TimelineEvent)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashSessionInsightController.java`, méthode `timeline`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CashSessionInsightService-Indicators"></a>
#### CashSessionInsightService.Indicators

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `sessionNumber` | texte |  |  |
| `status` | texte |  |  |
| `expectedBalance` | nombre décimal |  |  |
| `expectedHidden` | booléen |  |  |
| `paymentCount` | entier int64 ⚠ |  |  |
| `customersServed` | entier int64 ⚠ |  |  |
| `paymentTotal` | nombre décimal |  |  |
| `averageTicket` | nombre décimal |  |  |
| `refundCount` | entier int64 ⚠ |  |  |
| `refundTotal` | nombre décimal |  |  |
| `movementCount` | entier int64 ⚠ |  |  |
| `lastMovementAt` | date-heure ISO 8601 UTC |  |  |
| `activeMinutes` | entier int64 ⚠ |  |  |
| `pauseMinutes` | entier int64 ⚠ |  |  |
| `detentionCap` | nombre décimal |  |  |
| `detentionRemaining` | nombre décimal |  |  |
| `openAnomalies` | entier int64 ⚠ |  |  |

<a id="m-CashSessionInsightService-SessionPayments"></a>
#### CashSessionInsightService.SessionPayments

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `sessionNumber` | texte |  |  |
| `lines` | [CashSessionInsightService.PaymentLine](#m-CashSessionInsightService-PaymentLine)[] |  |  |
| `byMethod` | [CashSessionInsightService.MethodTotal](#m-CashSessionInsightService-MethodTotal)[] |  |  |
| `byHour` | [CashSessionInsightService.HourTotal](#m-CashSessionInsightService-HourTotal)[] |  |  |
| `paymentCount` | entier int64 ⚠ |  |  |
| `paymentTotal` | nombre décimal |  |  |
| `refundCount` | entier int64 ⚠ |  |  |
| `refundTotal` | nombre décimal |  |  |

<a id="m-CashRegisterService-ReassignedPayment"></a>
#### CashRegisterService.ReassignedPayment

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `transactionNumber` | texte |  |  |
| `fromSession` | texte |  |  |
| `toSession` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `exitMovementNumber` | texte |  |  |
| `entryMovementNumber` | texte |  |  |

<a id="m-CashSessionInsightController-ReassignRequest"></a>
#### CashSessionInsightController.ReassignRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `targetSessionNumber` | texte | oui | longueur min `1` |
| `reason` | texte | oui | longueur min `1` |

<a id="m-CashSessionInsightController-SentReport"></a>
#### CashSessionInsightController.SentReport

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `sessionNumber` | texte |  |  |
| `recipients` | texte[] |  |  |

<a id="m-CashSessionInsightController-SendReportRequest"></a>
#### CashSessionInsightController.SendReportRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `extraRecipients` | texte |  |  |

<a id="m-CashSessionInsightService-TimelineEvent"></a>
#### CashSessionInsightService.TimelineEvent

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `at` | date-heure ISO 8601 UTC |  |  |
| `kind` | texte |  |  |
| `label` | texte |  |  |
| `actor` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `reference` | texte |  |  |

<a id="m-CashSessionInsightService-PaymentLine"></a>
#### CashSessionInsightService.PaymentLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `transactionNumber` | texte |  |  |
| `paymentMethod` | texte |  |  |
| `status` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `tenderedAmount` | nombre décimal |  |  |
| `changeAmount` | nombre décimal |  |  |
| `receivedBy` | texte |  |  |
| `at` | date-heure ISO 8601 UTC |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `customerName` | texte |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `sourceLabel` | texte |  |  |
| `receiptNumber` | texte |  |  |
| `refund` | booléen |  |  |

<a id="m-CashSessionInsightService-MethodTotal"></a>
#### CashSessionInsightService.MethodTotal

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `paymentMethod` | texte |  |  |
| `count` | entier int64 ⚠ |  |  |
| `amount` | nombre décimal |  |  |

<a id="m-CashSessionInsightService-HourTotal"></a>
#### CashSessionInsightService.HourTotal

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `hour` | entier |  |  |
| `count` | entier int64 ⚠ |  |  |
| `amount` | nombre décimal |  |  |
