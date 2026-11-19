# BookingCompanyController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [booking](../README.md) · 6 route(s).

## Routes

<a id="r-post-sni-api-v1-bookings-companies-statements-run"></a>
### POST /sni/api/v1/bookings/companies/statements/run

- Accès : back-office : rôle d'administration et droit `BOOKING:CREATE`
- Réponse : `List<BookingCompanyAccounts.Statement>`
- Modèles : [BookingCompanyAccounts.Statement](#m-BookingCompanyAccounts-Statement)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingCompanyController.java`, méthode `runStatements`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `month` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-bookings-companies-businesscode-budgets"></a>
### GET /sni/api/v1/bookings/companies/{businessCode}/budgets

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `List<BookingCompanyAccounts.MemberBudget>`
- Modèles : [BookingCompanyAccounts.MemberBudget](#m-BookingCompanyAccounts-MemberBudget)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingCompanyController.java`, méthode `budgets`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `businessCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-bookings-companies-businesscode-budgets-memberid"></a>
### PUT /sni/api/v1/bookings/companies/{businessCode}/budgets/{memberId}

- Accès : back-office : rôle d'administration et droit `BOOKING:UPDATE`
- Corps JSON (obligatoire) : `BookingCompanyController.BudgetRequest`
- Réponse : `List<BookingCompanyAccounts.MemberBudget>`
- Modèles : [BookingCompanyAccounts.MemberBudget](#m-BookingCompanyAccounts-MemberBudget), [BookingCompanyController.BudgetRequest](#m-BookingCompanyController-BudgetRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingCompanyController.java`, méthode `setBudget`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `businessCode` | chemin | `String` | oui |  |
| `memberId` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-bookings-companies-businesscode-terms"></a>
### GET /sni/api/v1/bookings/companies/{businessCode}/terms

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `BookingCompanyAccounts.Terms`
- Modèles : [BookingCompanyAccounts.Terms](#m-BookingCompanyAccounts-Terms)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingCompanyController.java`, méthode `terms`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `businessCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-bookings-companies-businesscode-terms"></a>
### PUT /sni/api/v1/bookings/companies/{businessCode}/terms

- Accès : back-office : rôle d'administration et droit `BOOKING:UPDATE`
- Corps JSON (obligatoire) : `BookingCompanyController.TermsRequest`
- Réponse : `BookingCompanyAccounts.Terms`
- Modèles : [BookingCompanyAccounts.Terms](#m-BookingCompanyAccounts-Terms), [BookingCompanyController.TermsRequest](#m-BookingCompanyController-TermsRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingCompanyController.java`, méthode `saveTerms`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `businessCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-bookings-companies-businesscode-usage"></a>
### GET /sni/api/v1/bookings/companies/{businessCode}/usage

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `BookingCompanyAccounts.Usage`
- Modèles : [BookingCompanyAccounts.Usage](#m-BookingCompanyAccounts-Usage)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingCompanyController.java`, méthode `usage`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `businessCode` | chemin | `String` | oui |  |
| `month` | requête | `String` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-BookingCompanyAccounts-Statement"></a>
#### BookingCompanyAccounts.Statement

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `businessCode` | texte |  |  |
| `month` | objet libre |  |  |
| `items` | entier |  |  |
| `documentNumber` | texte |  |  |

<a id="m-BookingCompanyAccounts-MemberBudget"></a>
#### BookingCompanyAccounts.MemberBudget

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `businessCode` | texte |  |  |
| `memberId` | texte |  |  |
| `monthlyBudget` | nombre décimal |  |  |

<a id="m-BookingCompanyController-BudgetRequest"></a>
#### BookingCompanyController.BudgetRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `monthlyBudget` | nombre décimal |  |  |

<a id="m-BookingCompanyAccounts-Terms"></a>
#### BookingCompanyAccounts.Terms

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `businessCode` | texte |  |  |
| `monthlyStatement` | booléen |  |  |
| `monthlyBudget` | nombre décimal |  |  |
| `monthlyHours` | entier |  |  |
| `updatedBy` | texte |  |  |

<a id="m-BookingCompanyController-TermsRequest"></a>
#### BookingCompanyController.TermsRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `monthlyStatement` | booléen |  |  |
| `monthlyBudget` | nombre décimal |  |  |
| `monthlyHours` | entier |  |  |

<a id="m-BookingCompanyAccounts-Usage"></a>
#### BookingCompanyAccounts.Usage

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `businessCode` | texte |  |  |
| `month` | objet libre |  |  |
| `spent` | nombre décimal |  |  |
| `monthlyBudget` | nombre décimal |  |  |
| `coveredMinutes` | entier int64 (texte au-delà de 2^53) |  |  |
| `monthlyHours` | entier |  |  |
| `remainingMinutes` | entier int64 (texte au-delà de 2^53) |  |  |
| `members` | [BookingCompanyAccounts.MemberUsage](#m-BookingCompanyAccounts-MemberUsage)[] |  |  |

<a id="m-BookingCompanyAccounts-MemberUsage"></a>
#### BookingCompanyAccounts.MemberUsage

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `memberId` | texte |  |  |
| `spent` | nombre décimal |  |  |
| `monthlyBudget` | nombre décimal |  |  |
