# CompanyAccountController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [company](../README.md) · 9 route(s).

## Routes

<a id="r-get-sni-api-v1-companies"></a>
### GET /sni/api/v1/companies

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `PaginatedResponse<CompanyDtos.CompanySummary>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [CompanyDtos.CompanySummary](#m-CompanyDtos-CompanySummary)
- Code : `src/main/java/com/sni/bokaticowork/features/company/account/controller/CompanyAccountController.java`, méthode `search`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `q` | requête | `String` |  |  |
| `status` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-post-sni-api-v1-companies"></a>
### POST /sni/api/v1/companies

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Corps JSON (obligatoire) : `CompanyDtos.CreateCompanyRequest`
- Réponse : `CompanyDtos.CompanyResponse`
- Modèles : [CompanyDtos.CompanyResponse](#m-CompanyDtos-CompanyResponse), [CompanyDtos.CreateCompanyRequest](#m-CompanyDtos-CreateCompanyRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/company/account/controller/CompanyAccountController.java`, méthode `create`

<a id="r-get-sni-api-v1-companies-customerid"></a>
### GET /sni/api/v1/companies/{customerId}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `CompanyDtos.CompanyResponse`
- Modèles : [CompanyDtos.CompanyResponse](#m-CompanyDtos-CompanyResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/company/account/controller/CompanyAccountController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-companies-customerid-billing"></a>
### PATCH /sni/api/v1/companies/{customerId}/billing

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Corps JSON (obligatoire) : `CompanyDtos.UpdateBillingRequest`
- Réponse : `CompanyDtos.CompanyResponse`
- Modèles : [CompanyDtos.CompanyResponse](#m-CompanyDtos-CompanyResponse), [CompanyDtos.UpdateBillingRequest](#m-CompanyDtos-UpdateBillingRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/company/account/controller/CompanyAccountController.java`, méthode `updateBilling`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-companies-customerid-block"></a>
### POST /sni/api/v1/companies/{customerId}/block

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Corps JSON (obligatoire) : `CompanyDtos.BlockRequest`
- Réponse : `CompanyDtos.CompanyResponse`
- Modèles : [CompanyDtos.CompanyResponse](#m-CompanyDtos-CompanyResponse), [CompanyDtos.BlockRequest](#m-CompanyDtos-BlockRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/company/account/controller/CompanyAccountController.java`, méthode `block`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-companies-customerid-contact"></a>
### PATCH /sni/api/v1/companies/{customerId}/contact

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Corps JSON (obligatoire) : `CompanyDtos.UpdateContactRequest`
- Réponse : `CompanyDtos.CompanyResponse`
- Modèles : [CompanyDtos.CompanyResponse](#m-CompanyDtos-CompanyResponse), [CompanyDtos.UpdateContactRequest](#m-CompanyDtos-UpdateContactRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/company/account/controller/CompanyAccountController.java`, méthode `updateContact`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-companies-customerid-legal"></a>
### PATCH /sni/api/v1/companies/{customerId}/legal

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Corps JSON (obligatoire) : `CompanyDtos.UpdateLegalRequest`
- Réponse : `CompanyDtos.CompanyResponse`
- Modèles : [CompanyDtos.CompanyResponse](#m-CompanyDtos-CompanyResponse), [CompanyDtos.UpdateLegalRequest](#m-CompanyDtos-UpdateLegalRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/company/account/controller/CompanyAccountController.java`, méthode `updateLegal`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-companies-customerid-legal-profile"></a>
### POST /sni/api/v1/companies/{customerId}/legal-profile

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Corps JSON (obligatoire) : `CompanyDtos.LinkLegalProfileRequest`
- Réponse : `CompanyDtos.CompanyResponse`
- Modèles : [CompanyDtos.CompanyResponse](#m-CompanyDtos-CompanyResponse), [CompanyDtos.LinkLegalProfileRequest](#m-CompanyDtos-LinkLegalProfileRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/company/account/controller/CompanyAccountController.java`, méthode `linkLegalProfile`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-companies-customerid-unblock"></a>
### POST /sni/api/v1/companies/{customerId}/unblock

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `CompanyDtos.CompanyResponse`
- Modèles : [CompanyDtos.CompanyResponse](#m-CompanyDtos-CompanyResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/company/account/controller/CompanyAccountController.java`, méthode `unblock`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CompanyDtos-CompanySummary"></a>
#### CompanyDtos.CompanySummary

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `customerId` | texte |  |  |
| `name` | texte |  |  |
| `organizationType` | enum : `SOCIETE`, `ETABLISSEMENT`, `ENTREPRENEUR_INDIVIDUEL`, `PROFESSION_LIBERALE`, `ASSOCIATION`, `ONG`, `INSTITUTION_PUBLIQUE` |  |  |
| `niu` | texte |  |  |
| `rccm` | texte |  |  |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `status` | texte |  |  |
| `outstanding` | nombre décimal |  |  |

<a id="m-CompanyDtos-CompanyResponse"></a>
#### CompanyDtos.CompanyResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `customerId` | texte |  |  |
| `name` | texte |  |  |
| `organizationType` | enum : `SOCIETE`, `ETABLISSEMENT`, `ENTREPRENEUR_INDIVIDUEL`, `PROFESSION_LIBERALE`, `ASSOCIATION`, `ONG`, `INSTITUTION_PUBLIQUE` |  |  |
| `organizationTypeLabel` | texte |  |  |
| `legalForm` | texte |  |  |
| `activity` | texte |  |  |
| `identifiers` | [CompanyDtos.IdentifierResponse](#m-CompanyDtos-IdentifierResponse)[] |  |  |
| `suggestedIdentifiers` | enum : `NIU`, `RCCM`, `ORDRE_PROFESSIONNEL`, `RECEPISSE`, `AGREMENT`, `ACTE_CREATION`, `CODE_ADMINISTRATIF`, `AUTRE`[] |  |  |
| `legalRepresentativeName` | texte |  |  |
| `legalRepresentativeTitle` | texte |  |  |
| `vatRegime` | enum : `SUBJECT`, `EXEMPT` |  |  |
| `vatExemptionNote` | texte |  |  |
| `email` | texte |  |  |
| `billingEmail` | texte |  |  |
| `phone` | texte |  |  |
| `website` | texte |  |  |
| `address` | [CompanyDtos.AddressResponse](#m-CompanyDtos-AddressResponse) |  |  |
| `billingAddress` | [CompanyDtos.AddressResponse](#m-CompanyDtos-AddressResponse) |  |  |
| `paymentTermsDays` | entier |  |  |
| `creditLimit` | nombre décimal |  |  |
| `outstanding` | nombre décimal |  |  |
| `status` | texte |  |  |
| `blockedReason` | texte |  |  |
| `blockedBy` | texte |  |  |
| `blockedAt` | date-heure ISO 8601 UTC |  |  |
| `businessCode` | texte |  |  |
| `currencyCode` | texte |  | devise de facturation de l'organisation (code ISO à 3 lettres) |
| `membersCount` | entier int64 ⚠ |  |  |
| `activeSubscriptions` | entier |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CompanyDtos-CreateCompanyRequest"></a>
#### CompanyDtos.CreateCompanyRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `organizationType` | enum : `SOCIETE`, `ETABLISSEMENT`, `ENTREPRENEUR_INDIVIDUEL`, `PROFESSION_LIBERALE`, `ASSOCIATION`, `ONG`, `INSTITUTION_PUBLIQUE` | oui |  |
| `name` | texte | oui | longueur min `0`, longueur max `255` |
| `legalForm` | texte |  |  |
| `activity` | texte |  |  |
| `identifiers` | [CompanyDtos.IdentifierRequest](#m-CompanyDtos-IdentifierRequest)[] |  |  |
| `legalRepresentativeName` | texte |  |  |
| `legalRepresentativeTitle` | texte |  |  |
| `vatRegime` | enum : `SUBJECT`, `EXEMPT` |  |  |
| `vatExemptionNote` | texte |  | longueur min `0`, longueur max `500` |
| `email` | string (email) |  |  |
| `billingEmail` | string (email) |  |  |
| `phone` | texte | oui | longueur min `1` |
| `website` | texte |  |  |
| `address` | [AddressRequest](#m-AddressRequest) |  |  |
| `billingAddress` | [AddressRequest](#m-AddressRequest) |  |  |
| `paymentTermsDays` | entier |  |  |
| `creditLimit` | nombre décimal |  |  |
| `currencyCode` | texte |  | longueur min `3`, longueur max `3` |

<a id="m-CompanyDtos-UpdateBillingRequest"></a>
#### CompanyDtos.UpdateBillingRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `billingEmail` | string (email) |  |  |
| `billingAddress` | [AddressRequest](#m-AddressRequest) |  |  |
| `paymentTermsDays` | entier |  |  |
| `creditLimit` | nombre décimal |  |  |
| `removeCreditLimit` | booléen |  |  |
| `currencyCode` | texte |  | longueur `3` ; code ISO d'une devise connue (`XAF`, `EUR`, `USD`...) ; absent : devise inchangée ; inconnue : `400` « Devise inconnue » |

<a id="m-CompanyDtos-BlockRequest"></a>
#### CompanyDtos.BlockRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte | oui | longueur min `0`, longueur max `500` |

<a id="m-CompanyDtos-UpdateContactRequest"></a>
#### CompanyDtos.UpdateContactRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `email` | string (email) |  |  |
| `phone` | texte |  |  |
| `website` | texte |  |  |
| `address` | [AddressRequest](#m-AddressRequest) |  |  |

<a id="m-CompanyDtos-UpdateLegalRequest"></a>
#### CompanyDtos.UpdateLegalRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `organizationType` | enum : `SOCIETE`, `ETABLISSEMENT`, `ENTREPRENEUR_INDIVIDUEL`, `PROFESSION_LIBERALE`, `ASSOCIATION`, `ONG`, `INSTITUTION_PUBLIQUE` |  |  |
| `name` | texte |  | longueur min `0`, longueur max `255` |
| `legalForm` | texte |  |  |
| `activity` | texte |  |  |
| `identifiers` | [CompanyDtos.IdentifierRequest](#m-CompanyDtos-IdentifierRequest)[] |  |  |
| `legalRepresentativeName` | texte |  |  |
| `legalRepresentativeTitle` | texte |  |  |
| `vatRegime` | enum : `SUBJECT`, `EXEMPT` |  |  |
| `vatExemptionNote` | texte |  | longueur min `0`, longueur max `500` |

<a id="m-CompanyDtos-LinkLegalProfileRequest"></a>
#### CompanyDtos.LinkLegalProfileRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `businessCode` | texte | oui | longueur min `1` |

<a id="m-CompanyDtos-IdentifierResponse"></a>
#### CompanyDtos.IdentifierResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | enum : `NIU`, `RCCM`, `ORDRE_PROFESSIONNEL`, `RECEPISSE`, `AGREMENT`, `ACTE_CREATION`, `CODE_ADMINISTRATIF`, `AUTRE` |  |  |
| `label` | texte |  |  |
| `value` | texte |  |  |
| `format` | texte |  |  |

<a id="m-CompanyDtos-AddressResponse"></a>
#### CompanyDtos.AddressResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `streetNumber` | texte |  |  |
| `streetName` | texte |  |  |
| `district` | texte |  |  |
| `city` | texte |  |  |
| `country` | texte |  |  |

<a id="m-CompanyDtos-IdentifierRequest"></a>
#### CompanyDtos.IdentifierRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | enum : `NIU`, `RCCM`, `ORDRE_PROFESSIONNEL`, `RECEPISSE`, `AGREMENT`, `ACTE_CREATION`, `CODE_ADMINISTRATIF`, `AUTRE` | oui |  |
| `label` | texte |  | longueur min `0`, longueur max `120` |
| `value` | texte | oui | longueur min `0`, longueur max `150` |

<a id="m-AddressRequest"></a>
#### AddressRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `streetNumber` | texte |  |  |
| `streetName` | texte |  |  |
| `district` | texte |  |  |
| `city` | texte |  |  |
| `countryCode` | texte |  | longueur min `0`, longueur max `3` |
