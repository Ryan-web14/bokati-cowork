# SubscriptionSeatController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 3 route(s).

## Routes

<a id="r-get-sni-api-v1-subscriptions-seats"></a>
### GET /sni/api/v1/subscriptions/seats

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `PaginatedResponse<SubscriptionSeatResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [SubscriptionSeatResponse](#m-SubscriptionSeatResponse), [SeatStatus](#m-SeatStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/seat/controller/SubscriptionSeatController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | requête | `String` |  |  |
| `memberCode` | requête | `String` |  |  |
| `status` | requête | `SeatStatus` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-post-sni-api-v1-subscriptions-subscriptionnumber-seats"></a>
### POST /sni/api/v1/subscriptions/{subscriptionNumber}/seats

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE`
- Corps JSON (obligatoire) : `CreateSubscriptionSeatRequest`
- Réponse : `SubscriptionSeatResponse`
- Modèles : [SubscriptionSeatResponse](#m-SubscriptionSeatResponse), [CreateSubscriptionSeatRequest](#m-CreateSubscriptionSeatRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/seat/controller/SubscriptionSeatController.java`, méthode `add`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-subscriptions-subscriptionnumber-seats-membercode"></a>
### DELETE /sni/api/v1/subscriptions/{subscriptionNumber}/seats/{memberCode}

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:UPDATE`
- Réponse : `SubscriptionSeatResponse`
- Modèles : [SubscriptionSeatResponse](#m-SubscriptionSeatResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/seat/controller/SubscriptionSeatController.java`, méthode `remove`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |
| `memberCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-SubscriptionSeatResponse"></a>
#### SubscriptionSeatResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `subscriptionNumber` | texte |  |  |
| `memberCode` | texte |  |  |
| `memberName` | texte |  |  |
| `role` | enum : `OWNER`, `ADMIN`, `MEMBER`, `GUEST` |  |  |
| `status` | enum : `INVITED`, `ACTIVE`, `SUSPENDED`, `REMOVED` |  |  |
| `invitedAt` | date-heure ISO 8601 UTC |  |  |
| `activatedAt` | date-heure ISO 8601 UTC |  |  |
| `removedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-SeatStatus"></a>
#### SeatStatus

Énumération : `INVITED`, `ACTIVE`, `SUSPENDED`, `REMOVED`

<a id="m-CreateSubscriptionSeatRequest"></a>
#### CreateSubscriptionSeatRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `memberCode` | texte | oui | longueur min `1` |
| `role` | enum : `OWNER`, `ADMIN`, `MEMBER`, `GUEST` | oui |  |
