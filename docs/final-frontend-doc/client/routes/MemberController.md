# MemberController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [client](../README.md) · 22 route(s).

## Routes

<a id="r-get-sni-api-v1-members"></a>
### GET /sni/api/v1/members

- Accès : back-office : rôle d'administration et droit `CLIENT:READ`
- Réponse : `PaginatedResponse<Object>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/client/member/controller/MemberController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | requête | `String` |  |  |
| `summary` | requête | `boolean` |  | false |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-post-sni-api-v1-members"></a>
### POST /sni/api/v1/members

- Accès : back-office : rôle d'administration et droit `CLIENT:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `MEMBER_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `CreateMemberRequest`
- Réponse : `MemberResponse`
- Modèles : [MemberResponse](#m-MemberResponse), [CreateMemberRequest](#m-CreateMemberRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/client/member/controller/MemberController.java`, méthode `create`

<a id="r-post-sni-api-v1-members-admin-create"></a>
### POST /sni/api/v1/members/admin/create

- Accès : back-office : rôle d'administration et droit `CLIENT:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `MEMBER_ADMIN_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `CreateMemberRequest`
- Réponse : `MemberResponse`
- Modèles : [MemberResponse](#m-MemberResponse), [CreateMemberRequest](#m-CreateMemberRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/client/member/controller/MemberController.java`, méthode `createByAdmin`

<a id="r-get-sni-api-v1-members-by-email"></a>
### GET /sni/api/v1/members/by-email

- Accès : back-office : rôle d'administration et droit `CLIENT:READ`
- Réponse : `MemberResponse`
- Modèles : [MemberResponse](#m-MemberResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/client/member/controller/MemberController.java`, méthode `getByEmail`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `email` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-members-me-profile"></a>
### GET /sni/api/v1/members/me/profile

- Accès : back-office : rôle d'administration et droit `CLIENT:READ`
- Réponse : `MemberProfileResponse`
- Modèles : [MemberProfileResponse](#m-MemberProfileResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/client/member/controller/MemberController.java`, méthode `getMyProfile`

<a id="r-put-sni-api-v1-members-me-profile"></a>
### PUT /sni/api/v1/members/me/profile

- Accès : back-office : rôle d'administration et droit `CLIENT:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `MEMBER_MY_PROFILE_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `UpdateMemberProfileRequest`
- Réponse : `MemberProfileResponse`
- Modèles : [MemberProfileResponse](#m-MemberProfileResponse), [UpdateMemberProfileRequest](#m-UpdateMemberProfileRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/client/member/controller/MemberController.java`, méthode `updateMyProfile`

<a id="r-post-sni-api-v1-members-portal-complete"></a>
### POST /sni/api/v1/members/portal/complete

- Accès : back-office : rôle d'administration et droit `CLIENT:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `MEMBER_COMPLETE_PORTAL_REGISTRATION`, voir le README racine)
- Corps JSON (obligatoire) : `PortalMemberActivationRequest`
- Réponse : `MemberResponse`
- Modèles : [MemberResponse](#m-MemberResponse), [PortalMemberActivationRequest](#m-PortalMemberActivationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/client/member/controller/MemberController.java`, méthode `completePortalRegistration`

<a id="r-post-sni-api-v1-members-search"></a>
### POST /sni/api/v1/members/search

- Accès : back-office : rôle d'administration et droit `CLIENT:CREATE`
- Corps JSON (facultatif) : `MemberSearchCriteria`
- Réponse : `PaginatedResponse<MemberSummaryResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [MemberSummaryResponse](#m-MemberSummaryResponse), [MemberSearchCriteria](#m-MemberSearchCriteria)
- Code : `src/main/java/com/sni/bokaticowork/features/client/member/controller/MemberController.java`, méthode `search`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-get-sni-api-v1-members-search-basic"></a>
### GET /sni/api/v1/members/search/basic

- Accès : back-office : rôle d'administration et droit `CLIENT:READ`
- Réponse : `List<MemberSummaryResponse>`
- Modèles : [MemberSummaryResponse](#m-MemberSummaryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/client/member/controller/MemberController.java`, méthode `basicSearch`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-members-search-by-name"></a>
### GET /sni/api/v1/members/search/by-name

- Accès : back-office : rôle d'administration et droit `CLIENT:READ`
- Réponse : `List<MemberSummaryResponse>`
- Modèles : [MemberSummaryResponse](#m-MemberSummaryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/client/member/controller/MemberController.java`, méthode `basicSearch`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `query` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-members-summary"></a>
### GET /sni/api/v1/members/summary

- Accès : back-office : rôle d'administration et droit `CLIENT:READ`
- Réponse : `PaginatedResponse<MemberSummaryResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [MemberSummaryResponse](#m-MemberSummaryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/client/member/controller/MemberController.java`, méthode `listSummary`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-get-sni-api-v1-members-summary-all"></a>
### GET /sni/api/v1/members/summary/all

- Accès : back-office : rôle d'administration et droit `CLIENT:READ`
- Réponse : `List<MemberSummaryResponse>`
- Modèles : [MemberSummaryResponse](#m-MemberSummaryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/client/member/controller/MemberController.java`, méthode `listAllSummary`

<a id="r-delete-sni-api-v1-members-id"></a>
### DELETE /sni/api/v1/members/{id}

- Accès : back-office : rôle d'administration et droit `CLIENT:DELETE`
- Réponse : `non typé, voir le code`
- Code : `src/main/java/com/sni/bokaticowork/features/client/member/controller/MemberController.java`, méthode `archive`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `String` | oui |  |
| `purge` | requête | `boolean` |  | false |
| `reason` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-members-id"></a>
### GET /sni/api/v1/members/{id}

- Accès : back-office : rôle d'administration et droit `CLIENT:READ`
- Réponse : `MemberResponse`
- Modèles : [MemberResponse](#m-MemberResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/client/member/controller/MemberController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-members-id"></a>
### PATCH /sni/api/v1/members/{id}

- Accès : back-office : rôle d'administration et droit `CLIENT:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `MEMBER_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `UpdateMemberRequest`
- Réponse : `vide`
- Modèles : [UpdateMemberRequest](#m-UpdateMemberRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/client/member/controller/MemberController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-members-id-kyc-grace-period"></a>
### PATCH /sni/api/v1/members/{id}/kyc-grace-period

- Accès : back-office : rôle d'administration et droit `CLIENT:UPDATE`
- Corps JSON (obligatoire) : `KycGracePeriodRequest`
- Réponse : `vide`
- Modèles : [KycGracePeriodRequest](#m-KycGracePeriodRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/client/member/controller/MemberController.java`, méthode `setKycGracePeriod`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-members-id-portal-access-disable"></a>
### PATCH /sni/api/v1/members/{id}/portal-access/disable

- Accès : back-office : rôle d'administration et droit `CLIENT:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `MEMBER_DISABLE_PORTAL_ACCESS`, voir le README racine)
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/client/member/controller/MemberController.java`, méthode `disablePortalAccess`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-members-id-portal-access-enable"></a>
### PATCH /sni/api/v1/members/{id}/portal-access/enable

- Accès : back-office : rôle d'administration et droit `CLIENT:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `MEMBER_ENABLE_PORTAL_ACCESS`, voir le README racine)
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/client/member/controller/MemberController.java`, méthode `enablePortalAccess`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-members-id-profile"></a>
### GET /sni/api/v1/members/{id}/profile

- Accès : back-office : rôle d'administration et droit `CLIENT:READ`
- Réponse : `MemberProfileResponse`
- Modèles : [MemberProfileResponse](#m-MemberProfileResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/client/member/controller/MemberController.java`, méthode `getProfile`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-members-id-profile"></a>
### PUT /sni/api/v1/members/{id}/profile

- Accès : back-office : rôle d'administration et droit `CLIENT:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `MEMBER_PROFILE_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `UpdateMemberProfileRequest`
- Réponse : `MemberProfileResponse`
- Modèles : [MemberProfileResponse](#m-MemberProfileResponse), [UpdateMemberProfileRequest](#m-UpdateMemberProfileRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/client/member/controller/MemberController.java`, méthode `updateProfile`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-members-id-status"></a>
### PATCH /sni/api/v1/members/{id}/status

- Accès : back-office : rôle d'administration et droit `CLIENT:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `MEMBER_CHANGE_STATUS`, voir le README racine)
- Corps JSON (obligatoire) : `UpdateStatusRequest`
- Réponse : `vide`
- Modèles : [UpdateStatusRequest](#m-UpdateStatusRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/client/member/controller/MemberController.java`, méthode `changeStatus`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-members-id-transfer-customer"></a>
### PATCH /sni/api/v1/members/{id}/transfer-customer

- Accès : back-office : rôle d'administration et droit `CLIENT:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `MEMBER_TRANSFER_CUSTOMER`, voir le README racine)
- Corps JSON (obligatoire) : `TransferMemberCustomerRequest`
- Réponse : `MemberResponse`
- Modèles : [MemberResponse](#m-MemberResponse), [TransferMemberCustomerRequest](#m-TransferMemberCustomerRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/client/member/controller/MemberController.java`, méthode `transferCustomer`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-MemberResponse"></a>
#### MemberResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `memberId` | texte |  |  |
| `customerId` | texte |  |  |
| `userId` | texte |  |  |
| `firstname` | texte |  |  |
| `lastname` | texte |  |  |
| `fullname` | texte |  |  |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `whatsappPhone` | texte |  |  |
| `status` | texte |  |  |
| `portalAccess` | booléen |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CreateMemberRequest"></a>
#### CreateMemberRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `existingCustomerId` | texte |  | longueur min `0`, longueur max `300` |
| `customerType` | texte |  | longueur min `0`, longueur max `30` |
| `firstname` | texte |  | longueur min `0`, longueur max `350` |
| `lastname` | texte |  | longueur min `0`, longueur max `350` |
| `email` | string (email) | oui | longueur min `0`, longueur max `250` |
| `phone` | texte | oui | longueur min `0`, longueur max `30` |
| `phoneCountry` | texte |  | longueur min `0`, longueur max `10` |
| `whatsappPhone` | texte |  | longueur min `0`, longueur max `30` |
| `password` | texte |  | longueur min `0`, longueur max `45` |
| `companyName` | texte |  |  |
| `billingEmail` | string (email) |  | longueur min `0`, longueur max `250` |
| `address` | [AddressRequest](#m-AddressRequest) |  |  |
| `birthDate` | date AAAA-MM-JJ |  |  |
| `gender` | texte |  | longueur min `0`, longueur max `20` |
| `preferredCommunicationChannel` | texte |  | longueur min `0`, longueur max `30` |
| `generatePassword` | booléen |  |  |

<a id="m-MemberProfileResponse"></a>
#### MemberProfileResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `memberId` | texte |  |  |
| `firstName` | texte |  |  |
| `lastName` | texte |  |  |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `whatsappPhone` | texte |  |  |
| `birthDate` | date AAAA-MM-JJ |  |  |
| `jobTitle` | texte |  |  |
| `companyRole` | texte |  |  |
| `address` | texte |  |  |
| `city` | texte |  |  |
| `country` | texte |  |  |

<a id="m-UpdateMemberProfileRequest"></a>
#### UpdateMemberProfileRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `birthDate` | date AAAA-MM-JJ |  |  |
| `jobTitle` | texte |  | longueur min `0`, longueur max `120` |
| `companyRole` | texte |  | longueur min `0`, longueur max `120` |
| `address` | texte |  | longueur min `0`, longueur max `255` |
| `city` | texte |  | longueur min `0`, longueur max `120` |
| `country` | texte |  | longueur min `0`, longueur max `120` |

<a id="m-PortalMemberActivationRequest"></a>
#### PortalMemberActivationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `email` | string (email) | oui | longueur min `1` |

<a id="m-MemberSummaryResponse"></a>
#### MemberSummaryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `memberId` | texte |  |  |
| `customerId` | texte |  |  |
| `fullName` | texte |  |  |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `whatsapp_phone` | texte |  |  |
| `status` | texte |  |  |
| `portalAccess` | booléen |  |  |

<a id="m-MemberSearchCriteria"></a>
#### MemberSearchCriteria

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `query` | texte |  |  |
| `customerId` | texte |  |  |
| `memberId` | texte |  |  |
| `code` | texte |  |  |
| `firstname` | texte |  |  |
| `lastname` | texte |  |  |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `status` | enum : `ACTIVE`, `PENDING`, `UNDER_REVIEW`, `PENDING_CORRECTION`, `REJECTED`, `INACTIVE`, `SUSPENDED`, `ARCHIVED` |  |  |
| `portalAccess` | booléen |  |  |

<a id="m-UpdateMemberRequest"></a>
#### UpdateMemberRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `firstname` | texte |  | longueur min `0`, longueur max `350` |
| `lastname` | texte |  | longueur min `0`, longueur max `350` |
| `email` | string (email) |  | longueur min `0`, longueur max `250` |
| `phone` | texte |  | longueur min `0`, longueur max `30` |
| `phoneCountry` | texte |  |  |
| `whatsappPhone` | texte |  | longueur min `0`, longueur max `30` |

<a id="m-KycGracePeriodRequest"></a>
#### KycGracePeriodRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `gracePeriodDays` | entier | oui | min `1`, max `365` |

<a id="m-UpdateStatusRequest"></a>
#### UpdateStatusRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `status` | texte |  |  |

<a id="m-TransferMemberCustomerRequest"></a>
#### TransferMemberCustomerRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `customerId` | texte | oui | longueur min `1` |

<a id="m-AddressRequest"></a>
#### AddressRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `streetNumber` | texte |  |  |
| `streetName` | texte |  |  |
| `district` | texte |  |  |
| `city` | texte |  |  |
| `countryCode` | texte |  | longueur min `0`, longueur max `3` |
