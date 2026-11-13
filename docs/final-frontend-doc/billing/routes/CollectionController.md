# CollectionController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [billing](../README.md) · 4 route(s).

## Routes

<a id="r-post-sni-api-v1-billing-documents-documentnumber-payment-plan"></a>
### POST /sni/api/v1/billing/documents/{documentNumber}/payment-plan

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Corps JSON (obligatoire) : `CollectionDtos.PlanRequest`
- Réponse : `PaymentScheduleResponse`
- Modèles : [PaymentScheduleResponse](#m-PaymentScheduleResponse), [CollectionDtos.PlanRequest](#m-CollectionDtos-PlanRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CollectionController.java`, méthode `plan`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-billing-documents-documentnumber-payment-promises"></a>
### POST /sni/api/v1/billing/documents/{documentNumber}/payment-promises

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Corps JSON (obligatoire) : `CollectionDtos.PromiseRequest`
- Réponse : `CollectionDtos.PromiseView`
- Modèles : [CollectionDtos.PromiseView](#m-CollectionDtos-PromiseView), [CollectionDtos.PromiseRequest](#m-CollectionDtos-PromiseRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CollectionController.java`, méthode `promise`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-billing-payment-promises"></a>
### GET /sni/api/v1/billing/payment-promises

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Réponse : `List<CollectionDtos.PromiseView>`
- Modèles : [CollectionDtos.PromiseView](#m-CollectionDtos-PromiseView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CollectionController.java`, méthode `promises`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | requête | `String` |  |  |
| `openOnly` | requête | `boolean` |  | false |

<a id="r-patch-sni-api-v1-billing-payment-promises-promisenumber-cancel"></a>
### PATCH /sni/api/v1/billing/payment-promises/{promiseNumber}/cancel

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Réponse : `CollectionDtos.PromiseView`
- Modèles : [CollectionDtos.PromiseView](#m-CollectionDtos-PromiseView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CollectionController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `promiseNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-PaymentScheduleResponse"></a>
#### PaymentScheduleResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `scheduleNumber` | texte |  |  |
| `billingDocumentNumber` | texte |  |  |
| `status` | enum : `ACTIVE`, `COMPLETED`, `CANCELLED` |  |  |
| `totalAmount` | nombre décimal |  |  |
| `paidAmount` | nombre décimal |  |  |
| `balanceDue` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `notes` | texte |  |  |
| `installments` | [PaymentScheduleInstallmentResponse](#m-PaymentScheduleInstallmentResponse)[] |  |  |

<a id="m-CollectionDtos-PlanRequest"></a>
#### CollectionDtos.PlanRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `installments` | entier | oui | min `2`, max `36` |
| `firstDueDate` | date AAAA-MM-JJ | oui |  |
| `intervalMonths` | entier |  | min `1`, max `12` |
| `notes` | texte |  | longueur min `0`, longueur max `1000` |

<a id="m-CollectionDtos-PromiseView"></a>
#### CollectionDtos.PromiseView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `promiseNumber` | texte |  |  |
| `documentNumber` | texte |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `promisedDate` | date AAAA-MM-JJ |  |  |
| `amount` | nombre décimal |  |  |
| `balanceAtPromise` | nombre décimal |  |  |
| `channel` | texte |  |  |
| `note` | texte |  |  |
| `status` | texte |  |  |
| `createdBy` | texte |  |  |
| `closedAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CollectionDtos-PromiseRequest"></a>
#### CollectionDtos.PromiseRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `promisedDate` | date AAAA-MM-JJ | oui |  |
| `amount` | nombre décimal |  | min `0.01` |
| `channel` | texte |  |  |
| `note` | texte |  | longueur min `0`, longueur max `1000` |

<a id="m-PaymentScheduleInstallmentResponse"></a>
#### PaymentScheduleInstallmentResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `installmentNumber` | texte |  |  |
| `installmentOrder` | entier |  |  |
| `label` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `paidAmount` | nombre décimal |  |  |
| `balanceDue` | nombre décimal |  |  |
| `status` | enum : `PENDING`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`, `CANCELLED` |  |  |
| `paidAt` | date-heure ISO 8601 UTC |  |  |
