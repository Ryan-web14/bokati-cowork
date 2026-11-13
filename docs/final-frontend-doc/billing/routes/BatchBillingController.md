# BatchBillingController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [billing](../README.md) · 6 route(s).

## Routes

<a id="r-get-sni-api-v1-billing-batch-runs"></a>
### GET /sni/api/v1/billing/batch-runs

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<BatchBillingDtos.RunSummary>`
- Modèles : [BatchBillingDtos.RunSummary](#m-BatchBillingDtos-RunSummary)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BatchBillingController.java`, méthode `list`

<a id="r-post-sni-api-v1-billing-batch-runs"></a>
### POST /sni/api/v1/billing/batch-runs

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `BatchBillingDtos.PreviewRequest`
- Réponse : `BatchBillingDtos.RunView`
- Modèles : [BatchBillingDtos.RunView](#m-BatchBillingDtos-RunView), [BatchBillingDtos.PreviewRequest](#m-BatchBillingDtos-PreviewRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BatchBillingController.java`, méthode `preview`

<a id="r-get-sni-api-v1-billing-batch-runs-runnumber"></a>
### GET /sni/api/v1/billing/batch-runs/{runNumber}

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `BatchBillingDtos.RunView`
- Modèles : [BatchBillingDtos.RunView](#m-BatchBillingDtos-RunView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BatchBillingController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `runNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-billing-batch-runs-runnumber-cancel"></a>
### POST /sni/api/v1/billing/batch-runs/{runNumber}/cancel

- Accès : back-office : rôle d'administration et droit `BILLING:CANCEL`
- Réponse : `BatchBillingDtos.RunView`
- Modèles : [BatchBillingDtos.RunView](#m-BatchBillingDtos-RunView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BatchBillingController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `runNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-batch-runs-runnumber-exclude"></a>
### PATCH /sni/api/v1/billing/batch-runs/{runNumber}/exclude

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Corps JSON (obligatoire) : `BatchBillingDtos.ExcludeRequest`
- Réponse : `BatchBillingDtos.RunView`
- Modèles : [BatchBillingDtos.RunView](#m-BatchBillingDtos-RunView), [BatchBillingDtos.ExcludeRequest](#m-BatchBillingDtos-ExcludeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BatchBillingController.java`, méthode `exclude`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `runNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-billing-batch-runs-runnumber-issue"></a>
### POST /sni/api/v1/billing/batch-runs/{runNumber}/issue

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Réponse : `BatchBillingDtos.RunView`
- Modèles : [BatchBillingDtos.RunView](#m-BatchBillingDtos-RunView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/BatchBillingController.java`, méthode `issue`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `runNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-BatchBillingDtos-RunSummary"></a>
#### BatchBillingDtos.RunSummary

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `runNumber` | texte |  |  |
| `periodFrom` | date AAAA-MM-JJ |  |  |
| `periodTo` | date AAAA-MM-JJ |  |  |
| `title` | texte |  |  |
| `status` | texte |  |  |
| `customers` | entier |  |  |
| `invoices` | entier int64 ⚠ |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-BatchBillingDtos-RunView"></a>
#### BatchBillingDtos.RunView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `runNumber` | texte |  |  |
| `periodFrom` | date AAAA-MM-JJ |  |  |
| `periodTo` | date AAAA-MM-JJ |  |  |
| `title` | texte |  |  |
| `status` | texte |  |  |
| `createdBy` | texte |  |  |
| `issuedBy` | texte |  |  |
| `issuedAt` | date-heure ISO 8601 UTC |  |  |
| `totalToInvoice` | nombre décimal |  |  |
| `customers` | [BatchBillingDtos.RunItem](#m-BatchBillingDtos-RunItem)[] |  |  |

<a id="m-BatchBillingDtos-PreviewRequest"></a>
#### BatchBillingDtos.PreviewRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `periodFrom` | date AAAA-MM-JJ | oui |  |
| `periodTo` | date AAAA-MM-JJ | oui |  |
| `sourceTypes` | texte[] |  |  |
| `title` | texte |  | longueur min `0`, longueur max `255` |

<a id="m-BatchBillingDtos-ExcludeRequest"></a>
#### BatchBillingDtos.ExcludeRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemId` | entier int64 ⚠ | oui |  |
| `excluded` | booléen | oui |  |

<a id="m-BatchBillingDtos-RunItem"></a>
#### BatchBillingDtos.RunItem

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemId` | entier int64 ⚠ |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `customerName` | texte |  |  |
| `billingCompany` | texte |  |  |
| `itemCount` | entier |  |  |
| `totalAmount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `excluded` | booléen |  |  |
| `invoiceNumber` | texte |  |  |
| `error` | texte |  |  |
