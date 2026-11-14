# UnallocatedPaymentController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [billing](../README.md) · 2 route(s).

## Routes

<a id="r-get-sni-api-v1-billing-unallocated-payments-suggestions"></a>
### GET /sni/api/v1/billing/unallocated-payments/suggestions

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<UnallocatedPaymentDtos.Suggestion>`
- Modèles : [UnallocatedPaymentDtos.Suggestion](#m-UnallocatedPaymentDtos-Suggestion)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/UnallocatedPaymentController.java`, méthode `suggestions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerType` | requête | `String` |  |  |
| `customerCode` | requête | `String` |  |  |
| `limit` | requête | `int` |  | 100 |

<a id="r-post-sni-api-v1-billing-unallocated-payments-transactionnumber-allocate"></a>
### POST /sni/api/v1/billing/unallocated-payments/{transactionNumber}/allocate

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `UnallocatedPaymentDtos.AllocateRequest`
- Réponse : `UnallocatedPaymentDtos.AllocationResult`
- Modèles : [UnallocatedPaymentDtos.AllocationResult](#m-UnallocatedPaymentDtos-AllocationResult), [UnallocatedPaymentDtos.AllocateRequest](#m-UnallocatedPaymentDtos-AllocateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/UnallocatedPaymentController.java`, méthode `allocate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `transactionNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-UnallocatedPaymentDtos-Suggestion"></a>
#### UnallocatedPaymentDtos.Suggestion

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `transactionNumber` | texte |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `pending` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `paidAt` | date-heure ISO 8601 UTC |  |  |
| `providerReference` | texte |  |  |
| `payerName` | texte |  |  |
| `candidates` | [UnallocatedPaymentDtos.Candidate](#m-UnallocatedPaymentDtos-Candidate)[] |  |  |

<a id="m-UnallocatedPaymentDtos-AllocationResult"></a>
#### UnallocatedPaymentDtos.AllocationResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `transactionNumber` | texte |  |  |
| `allocated` | nombre décimal |  |  |
| `stillPending` | nombre décimal |  |  |
| `documentNumbers` | texte[] |  |  |

<a id="m-UnallocatedPaymentDtos-AllocateRequest"></a>
#### UnallocatedPaymentDtos.AllocateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `allocations` | [UnallocatedPaymentDtos.Allocation](#m-UnallocatedPaymentDtos-Allocation)[] |  |  |
| `elements` | [UnallocatedPaymentDtos.ElementAllocation](#m-UnallocatedPaymentDtos-ElementAllocation)[] |  |  |

<a id="m-UnallocatedPaymentDtos-Candidate"></a>
#### UnallocatedPaymentDtos.Candidate

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte |  |  |
| `title` | texte |  |  |
| `balanceDue` | nombre décimal |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `score` | entier |  |  |
| `reasons` | texte[] |  |  |
| `proposedAmount` | nombre décimal |  |  |

<a id="m-UnallocatedPaymentDtos-Allocation"></a>
#### UnallocatedPaymentDtos.Allocation

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte | oui | longueur min `1` |
| `amount` | nombre décimal | oui |  |

<a id="m-UnallocatedPaymentDtos-ElementAllocation"></a>
#### UnallocatedPaymentDtos.ElementAllocation

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | texte | oui | longueur min `1` |
| `code` | texte | oui | longueur min `1` |
| `amount` | nombre décimal | oui |  |
| `activationMode` | texte |  |  |
