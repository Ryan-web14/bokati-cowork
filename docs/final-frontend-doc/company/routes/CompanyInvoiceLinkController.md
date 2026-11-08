# CompanyInvoiceLinkController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [company](../README.md) · 3 route(s).

## Routes

<a id="r-post-sni-api-v1-billing-documents-company-links"></a>
### POST /sni/api/v1/billing/documents/company-links

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Corps JSON (obligatoire) : `CompanyInvoiceLinkService.LinkRequest`
- Réponse : `CompanyInvoiceLinkService.Result`
- Modèles : [CompanyInvoiceLinkService.Result](#m-CompanyInvoiceLinkService-Result), [CompanyInvoiceLinkService.LinkRequest](#m-CompanyInvoiceLinkService-LinkRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/company/membership/controller/CompanyInvoiceLinkController.java`, méthode `link`

<a id="r-post-sni-api-v1-billing-documents-company-links-preview"></a>
### POST /sni/api/v1/billing/documents/company-links/preview

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Corps JSON (obligatoire) : `CompanyInvoiceLinkService.LinkRequest`
- Réponse : `CompanyInvoiceLinkService.Result`
- Modèles : [CompanyInvoiceLinkService.Result](#m-CompanyInvoiceLinkService-Result), [CompanyInvoiceLinkService.LinkRequest](#m-CompanyInvoiceLinkService-LinkRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/company/membership/controller/CompanyInvoiceLinkController.java`, méthode `preview`

<a id="r-delete-sni-api-v1-billing-documents-documentnumber-company-link"></a>
### DELETE /sni/api/v1/billing/documents/{documentNumber}/company-link

- Accès : back-office : rôle d'administration et droit `BILLING:CANCEL` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/company/membership/controller/CompanyInvoiceLinkController.java`, méthode `unlink`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CompanyInvoiceLinkService-Result"></a>
#### CompanyInvoiceLinkService.Result

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `companyCustomerCode` | texte |  |  |
| `companyName` | texte |  |  |
| `eligible` | entier |  |  |
| `linked` | entier |  |  |
| `eligibleBalance` | nombre décimal |  |  |
| `lines` | [CompanyInvoiceLinkService.Line](#m-CompanyInvoiceLinkService-Line)[] |  |  |

<a id="m-CompanyInvoiceLinkService-LinkRequest"></a>
#### CompanyInvoiceLinkService.LinkRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `companyCustomerCode` | texte | oui | longueur min `1` |
| `documentNumbers` | texte[] |  |  |
| `memberId` | texte |  |  |
| `from` | date AAAA-MM-JJ |  |  |
| `to` | date AAAA-MM-JJ |  |  |

<a id="m-CompanyInvoiceLinkService-Line"></a>
#### CompanyInvoiceLinkService.Line

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte |  |  |
| `memberId` | texte |  |  |
| `memberName` | texte |  |  |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `totalAmount` | nombre décimal |  |  |
| `balanceDue` | nombre décimal |  |  |
| `status` | texte |  |  |
| `eligible` | booléen |  |  |
| `reason` | texte |  |  |
