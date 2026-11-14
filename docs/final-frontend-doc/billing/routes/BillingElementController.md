# BillingElementController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [billing](../README.md) · 9 route(s).

## Routes

<a id="r-post-sni-api-v1-billing-allocations-allocationid-cancel"></a>
### POST /sni/api/v1/billing/allocations/{allocationId}/cancel

- Accès : back-office : rôle d'administration et droit `BILLING:CANCEL`
- Corps JSON (obligatoire) : `AllocationReversalService.CancelRequest`
- Réponse : `AllocationReversalService.Result`
- Modèles : [AllocationReversalService.Result](#m-AllocationReversalService-Result), [AllocationReversalService.CancelRequest](#m-AllocationReversalService-CancelRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/element/controller/BillingElementController.java`, méthode `cancelAllocation`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `allocationId` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-billing-documents-documentnumber-elements"></a>
### GET /sni/api/v1/billing/documents/{documentNumber}/elements

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<ElementDtos.Settled>`
- Modèles : [ElementDtos.Settled](#m-ElementDtos-Settled)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/element/controller/BillingElementController.java`, méthode `elementsOfInvoice`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-billing-documents-documentnumber-elements"></a>
### POST /sni/api/v1/billing/documents/{documentNumber}/elements

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `ElementDtos.LinkRequest`
- Réponse : `List<ElementDtos.Settled>`
- Modèles : [ElementDtos.Settled](#m-ElementDtos-Settled), [ElementDtos.LinkRequest](#m-ElementDtos-LinkRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/element/controller/BillingElementController.java`, méthode `link`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-billing-documents-documentnumber-elements-type-code"></a>
### DELETE /sni/api/v1/billing/documents/{documentNumber}/elements/{type}/{code}

- Accès : back-office : rôle d'administration et droit `BILLING:CANCEL`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/billing/element/controller/BillingElementController.java`, méthode `unlink`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |
| `type` | chemin | `String` | oui |  |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-billing-elements"></a>
### GET /sni/api/v1/billing/elements

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<ElementDtos.Element>`
- Modèles : [ElementDtos.Element](#m-ElementDtos-Element)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/element/controller/BillingElementController.java`, méthode `elementsOf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `partyType` | requête | `String` | oui |  |
| `partyCode` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-billing-elements-search"></a>
### GET /sni/api/v1/billing/elements/search

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<ElementDtos.Party>`
- Modèles : [ElementDtos.Party](#m-ElementDtos-Party)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/element/controller/BillingElementController.java`, méthode `search`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` | oui |  |
| `limit` | requête | `int` |  | 10 |

<a id="r-get-sni-api-v1-billing-elements-type-code"></a>
### GET /sni/api/v1/billing/elements/{type}/{code}

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `ElementDtos.Element`
- Modèles : [ElementDtos.Element](#m-ElementDtos-Element)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/element/controller/BillingElementController.java`, méthode `element`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `type` | chemin | `String` | oui |  |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-billing-elements-type-code-invoices"></a>
### GET /sni/api/v1/billing/elements/{type}/{code}/invoices

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<ElementDtos.Invoice>`
- Modèles : [ElementDtos.Invoice](#m-ElementDtos-Invoice)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/element/controller/BillingElementController.java`, méthode `invoices`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `type` | chemin | `String` | oui |  |
| `code` | chemin | `String` | oui |  |
| `openOnly` | requête | `boolean` |  | false |

<a id="r-get-sni-api-v1-billing-transactions-transactionnumber-allocations"></a>
### GET /sni/api/v1/billing/transactions/{transactionNumber}/allocations

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `ElementDtos.Allocations`
- Modèles : [ElementDtos.Allocations](#m-ElementDtos-Allocations)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/element/controller/BillingElementController.java`, méthode `allocations`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `transactionNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-AllocationReversalService-Result"></a>
#### AllocationReversalService.Result

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `allocationId` | entier int64 ⚠ |  |  |
| `transactionNumber` | texte |  |  |
| `documentNumber` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `invoiceOutcome` | texte |  |  |
| `elementsReverted` | texte[] |  |  |
| `accountingRequired` | booléen |  |  |

<a id="m-AllocationReversalService-CancelRequest"></a>
#### AllocationReversalService.CancelRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `invoiceOutcome` | texte |  |  |
| `reason` | texte |  |  |

<a id="m-ElementDtos-Settled"></a>
#### ElementDtos.Settled

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | texte |  |  |
| `code` | texte |  |  |
| `origin` | texte |  |  |
| `activationMode` | texte |  |  |

<a id="m-ElementDtos-LinkRequest"></a>
#### ElementDtos.LinkRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | texte |  |  |
| `code` | texte |  |  |
| `activationMode` | texte |  |  |
| `replaceOwnInvoice` | booléen |  |  |

<a id="m-ElementDtos-Element"></a>
#### ElementDtos.Element

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | texte |  |  |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `status` | texte |  |  |
| `holderType` | texte |  |  |
| `holderCode` | texte |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `endDate` | date AAAA-MM-JJ |  |  |
| `totalAmount` | nombre décimal |  |  |

<a id="m-ElementDtos-Party"></a>
#### ElementDtos.Party

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | texte |  |  |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `external` | booléen |  |  |
| `elements` | [ElementDtos.Element](#m-ElementDtos-Element)[] |  |  |

<a id="m-ElementDtos-Invoice"></a>
#### ElementDtos.Invoice

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte |  |  |
| `documentType` | texte |  |  |
| `status` | texte |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `currency` | texte |  |  |
| `totalAmount` | nombre décimal |  |  |
| `balanceDue` | nombre décimal |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `origin` | texte |  |  |
| `activationMode` | texte |  |  |

<a id="m-ElementDtos-Allocations"></a>
#### ElementDtos.Allocations

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `transactionNumber` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `pending` | nombre décimal |  |  |
| `current` | [ElementDtos.Allocation](#m-ElementDtos-Allocation)[] |  |  |
| `reversed` | [ElementDtos.Reversal](#m-ElementDtos-Reversal)[] |  |  |

<a id="m-ElementDtos-Allocation"></a>
#### ElementDtos.Allocation

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `documentNumber` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `mode` | texte |  |  |
| `allocatedAt` | date-heure ISO 8601 UTC |  |  |
| `allocatedBy` | texte |  |  |

<a id="m-ElementDtos-Reversal"></a>
#### ElementDtos.Reversal

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `allocationId` | entier int64 ⚠ |  |  |
| `documentNumber` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `invoiceOutcome` | texte |  |  |
| `reason` | texte |  |  |
| `reversedAt` | date-heure ISO 8601 UTC |  |  |
| `reversedBy` | texte |  |  |
| `elementsReverted` | texte |  |  |
