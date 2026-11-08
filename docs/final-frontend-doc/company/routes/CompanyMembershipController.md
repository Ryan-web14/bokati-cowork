# CompanyMembershipController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [company](../README.md) · 6 route(s).

## Routes

<a id="r-patch-sni-api-v1-companies-customerid-invoice-link-mode"></a>
### PATCH /sni/api/v1/companies/{customerId}/invoice-link-mode

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Corps JSON (obligatoire) : `MembershipDtos.CompanyModeRequest`
- Réponse : `InvoiceLinkMode`
- Modèles : [InvoiceLinkMode](#m-InvoiceLinkMode), [MembershipDtos.CompanyModeRequest](#m-MembershipDtos-CompanyModeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/company/membership/controller/CompanyMembershipController.java`, méthode `setDefaultMode`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-customers-customerid-members"></a>
### GET /sni/api/v1/customers/{customerId}/members

- Accès : back-office : rôle d'administration et droit `CLIENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `List<MembershipDtos.LinkResponse>`
- Modèles : [MembershipDtos.LinkResponse](#m-MembershipDtos-LinkResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/company/membership/controller/CompanyMembershipController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `status` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-customers-customerid-members"></a>
### POST /sni/api/v1/customers/{customerId}/members

- Accès : back-office : rôle d'administration et droit `CLIENT:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Corps JSON (obligatoire) : `MembershipDtos.LinkMemberRequest`
- Réponse : `MembershipDtos.LinkResponse`
- Modèles : [MembershipDtos.LinkResponse](#m-MembershipDtos-LinkResponse), [MembershipDtos.LinkMemberRequest](#m-MembershipDtos-LinkMemberRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/company/membership/controller/CompanyMembershipController.java`, méthode `link`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-customers-customerid-members-linkid"></a>
### PATCH /sni/api/v1/customers/{customerId}/members/{linkId}

- Accès : back-office : rôle d'administration et droit `CLIENT:UPDATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Corps JSON (obligatoire) : `MembershipDtos.UpdateLinkRequest`
- Réponse : `MembershipDtos.LinkResponse`
- Modèles : [MembershipDtos.LinkResponse](#m-MembershipDtos-LinkResponse), [MembershipDtos.UpdateLinkRequest](#m-MembershipDtos-UpdateLinkRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/company/membership/controller/CompanyMembershipController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `linkId` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-customers-customerid-members-linkid-end"></a>
### POST /sni/api/v1/customers/{customerId}/members/{linkId}/end

- Accès : back-office : rôle d'administration et droit `CLIENT:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Corps JSON (obligatoire) : `MembershipDtos.EndLinkRequest`
- Réponse : `MembershipDtos.LinkResponse`
- Modèles : [MembershipDtos.LinkResponse](#m-MembershipDtos-LinkResponse), [MembershipDtos.EndLinkRequest](#m-MembershipDtos-EndLinkRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/company/membership/controller/CompanyMembershipController.java`, méthode `end`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `linkId` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-members-memberid-companies"></a>
### GET /sni/api/v1/members/{memberId}/companies

- Accès : back-office : rôle d'administration et droit `CLIENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `List<MembershipDtos.LinkResponse>`
- Modèles : [MembershipDtos.LinkResponse](#m-MembershipDtos-LinkResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/company/membership/controller/CompanyMembershipController.java`, méthode `companiesOf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `memberId` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-InvoiceLinkMode"></a>
#### InvoiceLinkMode

Énumération : `AUTOMATIC`, `ON_CHOICE`

<a id="m-MembershipDtos-CompanyModeRequest"></a>
#### MembershipDtos.CompanyModeRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `mode` | enum : `AUTOMATIC`, `ON_CHOICE` | oui |  |

<a id="m-MembershipDtos-LinkResponse"></a>
#### MembershipDtos.LinkResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `customerId` | texte |  |  |
| `companyName` | texte |  |  |
| `memberId` | texte |  |  |
| `memberName` | texte |  |  |
| `memberEmail` | texte |  |  |
| `role` | enum : `OWNER`, `EMPLOYEE` |  |  |
| `status` | enum : `ACTIVE`, `ENDED` |  |  |
| `startedAt` | date-heure ISO 8601 UTC |  |  |
| `endedAt` | date-heure ISO 8601 UTC |  |  |
| `endedReason` | texte |  |  |
| `invoiceLinkMode` | enum : `AUTOMATIC`, `ON_CHOICE` |  |  |
| `effectiveInvoiceLinkMode` | enum : `AUTOMATIC`, `ON_CHOICE` |  |  |
| `canSign` | booléen |  |  |
| `canCollectMail` | booléen |  |  |
| `payerChanged` | booléen |  |  |
| `warning` | texte |  |  |

<a id="m-MembershipDtos-LinkMemberRequest"></a>
#### MembershipDtos.LinkMemberRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `memberId` | texte | oui | longueur min `1` |
| `role` | enum : `OWNER`, `EMPLOYEE` | oui |  |
| `startedAt` | date-heure ISO 8601 UTC |  |  |
| `invoiceLinkMode` | enum : `AUTOMATIC`, `ON_CHOICE` |  |  |
| `canSign` | booléen |  |  |
| `canCollectMail` | booléen |  |  |

<a id="m-MembershipDtos-UpdateLinkRequest"></a>
#### MembershipDtos.UpdateLinkRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `role` | enum : `OWNER`, `EMPLOYEE` |  |  |
| `invoiceLinkMode` | enum : `AUTOMATIC`, `ON_CHOICE` |  |  |
| `useCompanyDefaultMode` | booléen |  |  |
| `canSign` | booléen |  |  |
| `canCollectMail` | booléen |  |  |

<a id="m-MembershipDtos-EndLinkRequest"></a>
#### MembershipDtos.EndLinkRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte | oui | longueur min `0`, longueur max `500` |
