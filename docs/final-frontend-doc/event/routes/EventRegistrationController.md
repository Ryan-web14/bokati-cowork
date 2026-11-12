# EventRegistrationController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [event](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-event-registrations"></a>
### GET /sni/api/v1/event-registrations

- Accès : back-office : rôle d'administration et droit `CRM:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:READ','CRM_READ')`
- Réponse : `PaginatedResponse<EventRegistrationDtos.EventRegistrationResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [EventRegistrationDtos.EventRegistrationResponse](#m-EventRegistrationDtos-EventRegistrationResponse), [RegistrationStatus](#m-RegistrationStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/event/controller/EventRegistrationController.java`, méthode `search`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `eventCode` | requête | `String` |  |  |
| `status` | requête | `RegistrationStatus` |  |  |
| `searchText` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-get-sni-api-v1-event-registrations-id"></a>
### GET /sni/api/v1/event-registrations/{id}

- Accès : back-office : rôle d'administration et droit `CRM:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:READ','CRM_READ')`
- Réponse : `EventRegistrationDtos.EventRegistrationResponse`
- Modèles : [EventRegistrationDtos.EventRegistrationResponse](#m-EventRegistrationDtos-EventRegistrationResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/event/controller/EventRegistrationController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-event-registrations-id-reject"></a>
### PATCH /sni/api/v1/event-registrations/{id}/reject

- Accès : back-office : rôle d'administration et droit `CRM:CONVERT` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:CONVERT','CRM_CONVERT')`
- Corps JSON (facultatif) : `EventRegistrationDtos.RejectEventRegistrationRequest`
- Réponse : `EventRegistrationDtos.EventRegistrationResponse`
- Modèles : [EventRegistrationDtos.EventRegistrationResponse](#m-EventRegistrationDtos-EventRegistrationResponse), [EventRegistrationDtos.RejectEventRegistrationRequest](#m-EventRegistrationDtos-RejectEventRegistrationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/event/controller/EventRegistrationController.java`, méthode `reject`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-event-registrations-id-validate"></a>
### PATCH /sni/api/v1/event-registrations/{id}/validate

- Accès : back-office : rôle d'administration et droit `CRM:CONVERT` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('CRM:CONVERT','CRM_CONVERT')`
- Réponse : `EventRegistrationDtos.EventRegistrationResponse`
- Modèles : [EventRegistrationDtos.EventRegistrationResponse](#m-EventRegistrationDtos-EventRegistrationResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/event/controller/EventRegistrationController.java`, méthode `validate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-EventRegistrationDtos-EventRegistrationResponse"></a>
#### EventRegistrationDtos.EventRegistrationResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `registrationNumber` | texte |  |  |
| `eventCode` | texte |  |  |
| `eventName` | texte |  |  |
| `firstname` | texte |  |  |
| `lastname` | texte |  |  |
| `fullName` | texte |  |  |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `whatsappPhone` | texte |  |  |
| `status` | enum : `PENDING_VALIDATION`, `VALIDATED`, `REJECTED` |  |  |
| `rejectionReason` | texte |  |  |
| `memberId` | texte |  |  |
| `customerId` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `validatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-RegistrationStatus"></a>
#### RegistrationStatus

Énumération : `PENDING_VALIDATION`, `VALIDATED`, `REJECTED`

<a id="m-EventRegistrationDtos-RejectEventRegistrationRequest"></a>
#### EventRegistrationDtos.RejectEventRegistrationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  | longueur min `0`, longueur max `255` |
