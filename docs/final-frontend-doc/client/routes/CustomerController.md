# CustomerController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [client](../README.md) · 10 route(s).

## Routes

<a id="r-get-sni-api-v1-customers"></a>
### GET /sni/api/v1/customers

- Accès : back-office : rôle d'administration et droit `CLIENT:READ`
- Réponse : `non typé, voir le code`
- Code : `src/main/java/com/sni/bokaticowork/features/client/customer/controller/CustomerController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `summary` | requête | `boolean` |  | false |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-post-sni-api-v1-customers"></a>
### POST /sni/api/v1/customers

- Accès : back-office : rôle d'administration et droit `CLIENT:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `CUSTOMER_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `CustomerRequest`
- Réponse : `CustomerResponse`
- Modèles : [CustomerResponse](#m-CustomerResponse), [CustomerRequest](#m-CustomerRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/client/customer/controller/CustomerController.java`, méthode `create`

<a id="r-get-sni-api-v1-customers-by-email"></a>
### GET /sni/api/v1/customers/by-email

- Accès : back-office : rôle d'administration et droit `CLIENT:READ`
- Réponse : `CustomerResponse`
- Modèles : [CustomerResponse](#m-CustomerResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/client/customer/controller/CustomerController.java`, méthode `getByEmail`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `email` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-customers-search-basic"></a>
### GET /sni/api/v1/customers/search/basic

- Accès : back-office : rôle d'administration et droit `CLIENT:READ`
- Réponse : `List<CustomerSummaryresponse>`
- Modèles : [CustomerSummaryresponse](#m-CustomerSummaryresponse)
- Code : `src/main/java/com/sni/bokaticowork/features/client/customer/controller/CustomerController.java`, méthode `basicSearch`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` | oui |  |
| `type` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-customers-summary"></a>
### GET /sni/api/v1/customers/summary

- Accès : back-office : rôle d'administration et droit `CLIENT:READ`
- Réponse : `PaginatedResponse<CustomerSummaryresponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [CustomerSummaryresponse](#m-CustomerSummaryresponse)
- Code : `src/main/java/com/sni/bokaticowork/features/client/customer/controller/CustomerController.java`, méthode `listSummary`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-get-sni-api-v1-customers-summary-all"></a>
### GET /sni/api/v1/customers/summary/all

- Accès : back-office : rôle d'administration et droit `CLIENT:READ`
- Réponse : `List<CustomerSummaryresponse>`
- Modèles : [CustomerSummaryresponse](#m-CustomerSummaryresponse)
- Code : `src/main/java/com/sni/bokaticowork/features/client/customer/controller/CustomerController.java`, méthode `listAllSummary`

<a id="r-delete-sni-api-v1-customers-customerid"></a>
### DELETE /sni/api/v1/customers/{customerId}

- Accès : back-office : rôle d'administration et droit `CLIENT:DELETE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/client/customer/controller/CustomerController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-customers-customerid"></a>
### GET /sni/api/v1/customers/{customerId}

- Accès : back-office : rôle d'administration et droit `CLIENT:READ`
- Réponse : `CustomerResponse`
- Modèles : [CustomerResponse](#m-CustomerResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/client/customer/controller/CustomerController.java`, méthode `getByCustomerId`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-customers-customerid"></a>
### PATCH /sni/api/v1/customers/{customerId}

- Accès : back-office : rôle d'administration et droit `CLIENT:UPDATE`
- Corps JSON (obligatoire) : `CustomerRequest`
- Réponse : `vide`
- Modèles : [CustomerRequest](#m-CustomerRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/client/customer/controller/CustomerController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-customers-customerid-status"></a>
### PATCH /sni/api/v1/customers/{customerId}/status

- Accès : back-office : rôle d'administration et droit `CLIENT:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `CUSTOMER_CHANGE_STATUS`, voir le README racine)
- Corps JSON (obligatoire) : `ChangeCustomerStatusRequest`
- Réponse : `vide`
- Modèles : [ChangeCustomerStatusRequest](#m-ChangeCustomerStatusRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/client/customer/controller/CustomerController.java`, méthode `changeStatus`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CustomerResponse"></a>
#### CustomerResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `customerId` | texte |  |  |
| `type` | texte |  |  |
| `firstname` | texte |  |  |
| `lastname` | texte |  |  |
| `companyName` | texte |  |  |
| `email` | texte |  |  |
| `billingEmail` | texte |  |  |
| `phone` | texte |  |  |
| `whatsappPhone` | texte |  |  |
| `address` | [AddressResponse](#m-AddressResponse) |  |  |
| `status` | texte |  |  |
| `createdAt` | texte |  |  |

<a id="m-CustomerRequest"></a>
#### CustomerRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | texte | oui |  |
| `firstname` | texte |  |  |
| `lastname` | texte |  |  |
| `companyName` | texte |  |  |
| `email` | string (email) |  |  |
| `billingEmail` | texte |  |  |
| `address` | [AddressRequest](#m-AddressRequest) |  |  |
| `phone` | texte | oui |  |
| `whatsappPhone` | texte |  |  |
| `phoneCountry` | texte |  |  |

<a id="m-CustomerSummaryresponse"></a>
#### CustomerSummaryresponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `customerId` | texte |  |  |
| `displayName` | texte |  |  |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `status` | texte |  |  |

<a id="m-ChangeCustomerStatusRequest"></a>
#### ChangeCustomerStatusRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `status` | enum : `ACTIVE`, `PENDING`, `INACTIVE`, `SUSPENDED`, `ARCHIVED` | oui |  |

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

<a id="m-AddressRequest"></a>
#### AddressRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `streetNumber` | texte |  |  |
| `streetName` | texte |  |  |
| `district` | texte |  |  |
| `city` | texte |  |  |
| `countryCode` | texte |  | longueur min `0`, longueur max `3` |
