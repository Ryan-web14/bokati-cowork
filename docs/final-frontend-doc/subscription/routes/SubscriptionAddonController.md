# SubscriptionAddonController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 3 route(s).

## Routes

<a id="r-get-sni-api-v1-subscriptions-addons"></a>
### GET /sni/api/v1/subscriptions/addons

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `PaginatedResponse<SubscriptionAddonResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [SubscriptionAddonResponse](#m-SubscriptionAddonResponse), [SubscriptionAddonStatus](#m-SubscriptionAddonStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/addon/controller/SubscriptionAddonController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | requête | `String` |  |  |
| `planCode` | requête | `String` |  |  |
| `status` | requête | `SubscriptionAddonStatus` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-patch-sni-api-v1-subscriptions-addons-addonid-cancel"></a>
### PATCH /sni/api/v1/subscriptions/addons/{addonId}/cancel

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CANCEL`
- Réponse : `SubscriptionAddonResponse`
- Modèles : [SubscriptionAddonResponse](#m-SubscriptionAddonResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/addon/controller/SubscriptionAddonController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `addonId` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-subscriptionnumber-addons"></a>
### POST /sni/api/v1/subscriptions/{subscriptionNumber}/addons

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE`
- Corps JSON (obligatoire) : `CreateSubscriptionAddonRequest`
- Réponse : `SubscriptionAddonResponse`
- Modèles : [SubscriptionAddonResponse](#m-SubscriptionAddonResponse), [CreateSubscriptionAddonRequest](#m-CreateSubscriptionAddonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/addon/controller/SubscriptionAddonController.java`, méthode `add`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-SubscriptionAddonResponse"></a>
#### SubscriptionAddonResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `subscriptionNumber` | texte |  |  |
| `planVersionId` | entier int64 ⚠ |  |  |
| `planCode` | texte |  |  |
| `planName` | texte |  |  |
| `quantity` | entier |  |  |
| `unitPrice` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `status` | enum : `ACTIVE`, `PENDING`, `CANCELLED`, `EXPIRED` |  |  |
| `startsAt` | date AAAA-MM-JJ |  |  |
| `endsAt` | date AAAA-MM-JJ |  |  |
| `contractCode` | texte |  |  |
| `metadataJson` | texte |  |  |

<a id="m-SubscriptionAddonStatus"></a>
#### SubscriptionAddonStatus

Énumération : `ACTIVE`, `PENDING`, `CANCELLED`, `EXPIRED`

<a id="m-CreateSubscriptionAddonRequest"></a>
#### CreateSubscriptionAddonRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `planVersionId` | entier int64 ⚠ | oui |  |
| `quantity` | entier |  | min `1` |
| `startsAt` | date AAAA-MM-JJ |  |  |
| `endsAt` | date AAAA-MM-JJ |  |  |
| `metadataJson` | texte |  |  |
