# BusinessController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [company](../README.md) · 12 route(s).

## Routes

<a id="r-get-sni-api-v1-businesses"></a>
### GET /sni/api/v1/businesses

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `List<BusinessEntityResponse>`
- Modèles : [BusinessEntityResponse](#m-BusinessEntityResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/company/controller/BusinessController.java`, méthode `listBusinesses`

<a id="r-post-sni-api-v1-businesses"></a>
### POST /sni/api/v1/businesses

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- En-tête `Idempotency-Key` : conseillé (opération `BUSINESS_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `BusinessEntityRequest`
- Réponse : `BusinessEntityResponse`
- Modèles : [BusinessEntityResponse](#m-BusinessEntityResponse), [BusinessEntityRequest](#m-BusinessEntityRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/company/controller/BusinessController.java`, méthode `createBusiness`

<a id="r-get-sni-api-v1-businesses-by-name"></a>
### GET /sni/api/v1/businesses/by-name

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `BusinessEntityResponse`
- Modèles : [BusinessEntityResponse](#m-BusinessEntityResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/company/controller/BusinessController.java`, méthode `getBusinessByName`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `name` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-businesses-paged"></a>
### GET /sni/api/v1/businesses/paged

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `PaginatedResponse<BusinessEntityResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [BusinessEntityResponse](#m-BusinessEntityResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/company/controller/BusinessController.java`, méthode `listBusinessesPaged`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-post-sni-api-v1-businesses-search"></a>
### POST /sni/api/v1/businesses/search

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Corps JSON (facultatif) : `BusinessSearchCriteria`
- Réponse : `PaginatedResponse<BusinessEntityResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [BusinessEntityResponse](#m-BusinessEntityResponse), [BusinessSearchCriteria](#m-BusinessSearchCriteria)
- Code : `src/main/java/com/sni/bokaticowork/features/company/controller/BusinessController.java`, méthode `search`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-get-sni-api-v1-businesses-search-basic"></a>
### GET /sni/api/v1/businesses/search/basic

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `List<BusinessEntityResponse>`
- Modèles : [BusinessEntityResponse](#m-BusinessEntityResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/company/controller/BusinessController.java`, méthode `basicSearch`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` | oui |  |

<a id="r-delete-sni-api-v1-businesses-code"></a>
### DELETE /sni/api/v1/businesses/{code}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/company/controller/BusinessController.java`, méthode `deleteBusiness`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-businesses-code"></a>
### GET /sni/api/v1/businesses/{code}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Réponse : `BusinessEntityResponse`
- Modèles : [BusinessEntityResponse](#m-BusinessEntityResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/company/controller/BusinessController.java`, méthode `getBusiness`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-businesses-code"></a>
### PUT /sni/api/v1/businesses/{code}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- En-tête `Idempotency-Key` : conseillé (opération `BUSINESS_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `BusinessEntityRequest`
- Réponse : `BusinessEntityResponse`
- Modèles : [BusinessEntityResponse](#m-BusinessEntityResponse), [BusinessEntityRequest](#m-BusinessEntityRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/company/controller/BusinessController.java`, méthode `updateBusiness`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-businesses-code-address"></a>
### PATCH /sni/api/v1/businesses/{code}/address

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- En-tête `Idempotency-Key` : conseillé (opération `BUSINESS_UPDATE_ADDRESS`, voir le README racine)
- Corps JSON (obligatoire) : `AddressRequest`
- Réponse : `vide`
- Modèles : [AddressRequest](#m-AddressRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/company/controller/BusinessController.java`, méthode `updateBusinessAddress`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-businesses-code-operator"></a>
### PATCH /sni/api/v1/businesses/{code}/operator

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- En-tête `Idempotency-Key` : conseillé (opération `BUSINESS_DESIGNATE_OPERATOR`, voir le README racine)
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/company/controller/BusinessController.java`, méthode `designateOperatingBusiness`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-businesses-code-status"></a>
### PATCH /sni/api/v1/businesses/{code}/status

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- En-tête `Idempotency-Key` : conseillé (opération `BUSINESS_UPDATE_STATUS`, voir le README racine)
- Corps JSON (obligatoire) : `UpdateBusinessStatusRequest`
- Réponse : `vide`
- Modèles : [UpdateBusinessStatusRequest](#m-UpdateBusinessStatusRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/company/controller/BusinessController.java`, méthode `updateBusinessStatus`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-BusinessEntityResponse"></a>
#### BusinessEntityResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `status` | texte |  |  |
| `legalForm` | texte |  |  |
| `niuNumber` | texte |  |  |
| `rccmNumber` | texte |  |  |
| `taxId` | texte |  |  |
| `activity` | texte |  |  |
| `baseCurrencyCode` | texte |  |  |
| `address` | [AddressResponse](#m-AddressResponse) |  |  |
| `phone` | texte |  |  |
| `email` | texte |  |  |

<a id="m-BusinessEntityRequest"></a>
#### BusinessEntityRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `1` |
| `legalForm` | texte | oui | longueur min `1` |
| `niuNumber` | texte |  |  |
| `rccmNumber` | texte | oui | longueur min `1` |
| `taxId` | texte |  |  |
| `activity` | texte |  |  |
| `address` | [AddressRequest](#m-AddressRequest) |  |  |
| `phone` | texte | oui | longueur min `1` |
| `email` | string (email) |  |  |
| `baseCurrencyCode` | texte | oui | longueur min `0`, longueur max `3` |

<a id="m-BusinessSearchCriteria"></a>
#### BusinessSearchCriteria

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `legalForm` | texte |  |  |
| `niuNumber` | texte |  |  |
| `rccmNumber` | texte |  |  |
| `taxId` | texte |  |  |
| `activity` | texte |  |  |
| `phone` | texte |  |  |
| `email` | texte |  |  |
| `baseCurrencyCode` | texte |  |  |
| `status` | texte |  |  |

<a id="m-AddressRequest"></a>
#### AddressRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `streetNumber` | texte |  |  |
| `streetName` | texte |  |  |
| `district` | texte |  |  |
| `city` | texte |  |  |
| `countryCode` | texte |  | longueur min `0`, longueur max `3` |

<a id="m-UpdateBusinessStatusRequest"></a>
#### UpdateBusinessStatusRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `status` | texte | oui | longueur min `1` |

<a id="m-AddressResponse"></a>
#### AddressResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `streetNumber` | texte |  |  |
| `streetName` | texte |  |  |
| `district` | texte |  |  |
| `city` | texte |  |  |
| `country` | texte |  |  |
| `fullAddress` | texte |  |  |
