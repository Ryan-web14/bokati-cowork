# SupportRoutingRuleController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [support](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-support-routing-rules"></a>
### GET /sni/api/v1/support/routing-rules

- Accès : back-office : rôle d'administration et droit `SUPPORT:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')`
- Réponse : `List<SupportDtos.RoutingRuleResponse>`
- Modèles : [SupportDtos.RoutingRuleResponse](#m-SupportDtos-RoutingRuleResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportRoutingRuleController.java`, méthode `list`

<a id="r-post-sni-api-v1-support-routing-rules"></a>
### POST /sni/api/v1/support/routing-rules

- Accès : back-office : rôle d'administration et droit `SUPPORT:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')`
- Corps JSON (obligatoire) : `SupportDtos.RoutingRuleRequest`
- Réponse : `SupportDtos.RoutingRuleResponse`
- Modèles : [SupportDtos.RoutingRuleResponse](#m-SupportDtos-RoutingRuleResponse), [SupportDtos.RoutingRuleRequest](#m-SupportDtos-RoutingRuleRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportRoutingRuleController.java`, méthode `create`

<a id="r-delete-sni-api-v1-support-routing-rules-id"></a>
### DELETE /sni/api/v1/support/routing-rules/{id}

- Accès : back-office : rôle d'administration et droit `SUPPORT:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportRoutingRuleController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-put-sni-api-v1-support-routing-rules-id"></a>
### PUT /sni/api/v1/support/routing-rules/{id}

- Accès : back-office : rôle d'administration et droit `SUPPORT:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')`
- Corps JSON (obligatoire) : `SupportDtos.RoutingRuleRequest`
- Réponse : `SupportDtos.RoutingRuleResponse`
- Modèles : [SupportDtos.RoutingRuleResponse](#m-SupportDtos-RoutingRuleResponse), [SupportDtos.RoutingRuleRequest](#m-SupportDtos-RoutingRuleRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportRoutingRuleController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-SupportDtos-RoutingRuleResponse"></a>
#### SupportDtos.RoutingRuleResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `name` | texte |  |  |
| `active` | booléen |  |  |
| `category` | enum : `BILLING`, `BOOKING`, `ACCESS`, `TECHNICAL`, `OTHER` |  |  |
| `priority` | enum : `LOW`, `MEDIUM`, `HIGH`, `URGENT` |  |  |
| `ownerType` | texte |  |  |
| `relatedType` | texte |  |  |
| `assignedTo` | entier int64 ⚠ |  |  |
| `teamCode` | texte |  |  |
| `sortOrder` | entier |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-SupportDtos-RoutingRuleRequest"></a>
#### SupportDtos.RoutingRuleRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `1` |
| `active` | booléen |  |  |
| `category` | enum : `BILLING`, `BOOKING`, `ACCESS`, `TECHNICAL`, `OTHER` |  |  |
| `priority` | enum : `LOW`, `MEDIUM`, `HIGH`, `URGENT` |  |  |
| `ownerType` | texte |  |  |
| `relatedType` | texte |  |  |
| `assignedTo` | entier int64 ⚠ |  |  |
| `teamCode` | texte |  |  |
| `sortOrder` | entier |  |  |
