# KnowledgeArticleController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [support](../README.md) · 7 route(s).

## Routes

<a id="r-get-sni-api-v1-support-knowledge-base"></a>
### GET /sni/api/v1/support/knowledge-base

- Accès : back-office : rôle d'administration et droit `SUPPORT:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')`
- Réponse : `PaginatedResponse<KnowledgeDtos.KnowledgeArticleResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [KnowledgeDtos.KnowledgeArticleResponse](#m-KnowledgeDtos-KnowledgeArticleResponse), [TicketCategory](#m-TicketCategory)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/KnowledgeArticleController.java`, méthode `search`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `category` | requête | `TicketCategory` |  |  |
| `searchText` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-post-sni-api-v1-support-knowledge-base"></a>
### POST /sni/api/v1/support/knowledge-base

- Accès : back-office : rôle d'administration et droit `SUPPORT:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')`
- Corps JSON (obligatoire) : `KnowledgeDtos.KnowledgeArticleRequest`
- Réponse : `KnowledgeDtos.KnowledgeArticleResponse`
- Modèles : [KnowledgeDtos.KnowledgeArticleResponse](#m-KnowledgeDtos-KnowledgeArticleResponse), [KnowledgeDtos.KnowledgeArticleRequest](#m-KnowledgeDtos-KnowledgeArticleRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/KnowledgeArticleController.java`, méthode `create`

<a id="r-get-sni-api-v1-support-knowledge-base-analytics"></a>
### GET /sni/api/v1/support/knowledge-base/analytics

- Accès : back-office : rôle d'administration et droit `SUPPORT:METRICS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:METRICS','SUPPORT_METRICS')`
- Réponse : `KnowledgeDtos.KnowledgeAnalyticsResponse`
- Modèles : [KnowledgeDtos.KnowledgeAnalyticsResponse](#m-KnowledgeDtos-KnowledgeAnalyticsResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/KnowledgeArticleController.java`, méthode `analytics`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `Instant` |  |  |
| `to` | requête | `Instant` |  |  |

<a id="r-get-sni-api-v1-support-knowledge-base-suggestions"></a>
### GET /sni/api/v1/support/knowledge-base/suggestions

- Accès : back-office : rôle d'administration et droit `SUPPORT:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')`
- Réponse : `List<KnowledgeDtos.ArticleSuggestionResponse>`
- Modèles : [KnowledgeDtos.ArticleSuggestionResponse](#m-KnowledgeDtos-ArticleSuggestionResponse), [TicketCategory](#m-TicketCategory)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/KnowledgeArticleController.java`, méthode `suggestions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `category` | requête | `TicketCategory` |  |  |
| `searchText` | requête | `String` |  |  |
| `limit` | requête | `int` |  | 5 |

<a id="r-delete-sni-api-v1-support-knowledge-base-articlecode"></a>
### DELETE /sni/api/v1/support/knowledge-base/{articleCode}

- Accès : back-office : rôle d'administration et droit `SUPPORT:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/KnowledgeArticleController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `articleCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-support-knowledge-base-articlecode"></a>
### GET /sni/api/v1/support/knowledge-base/{articleCode}

- Accès : back-office : rôle d'administration et droit `SUPPORT:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')`
- Réponse : `KnowledgeDtos.KnowledgeArticleResponse`
- Modèles : [KnowledgeDtos.KnowledgeArticleResponse](#m-KnowledgeDtos-KnowledgeArticleResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/KnowledgeArticleController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `articleCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-support-knowledge-base-articlecode"></a>
### PUT /sni/api/v1/support/knowledge-base/{articleCode}

- Accès : back-office : rôle d'administration et droit `SUPPORT:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')`
- Corps JSON (obligatoire) : `KnowledgeDtos.KnowledgeArticleRequest`
- Réponse : `KnowledgeDtos.KnowledgeArticleResponse`
- Modèles : [KnowledgeDtos.KnowledgeArticleResponse](#m-KnowledgeDtos-KnowledgeArticleResponse), [KnowledgeDtos.KnowledgeArticleRequest](#m-KnowledgeDtos-KnowledgeArticleRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/KnowledgeArticleController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `articleCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-KnowledgeDtos-KnowledgeArticleResponse"></a>
#### KnowledgeDtos.KnowledgeArticleResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `articleCode` | texte |  |  |
| `title` | texte |  |  |
| `slug` | texte |  |  |
| `body` | texte |  |  |
| `category` | enum : `BILLING`, `BOOKING`, `ACCESS`, `TECHNICAL`, `OTHER` |  |  |
| `tags` | texte[] |  |  |
| `publicVisible` | booléen |  |  |
| `internalOnly` | booléen |  |  |
| `active` | booléen |  |  |
| `viewCount` | entier int64 ⚠ |  |  |
| `createdBy` | entier int64 ⚠ |  |  |
| `updatedBy` | entier int64 ⚠ |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-TicketCategory"></a>
#### TicketCategory

Énumération : `BILLING`, `BOOKING`, `ACCESS`, `TECHNICAL`, `OTHER`

<a id="m-KnowledgeDtos-KnowledgeArticleRequest"></a>
#### KnowledgeDtos.KnowledgeArticleRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `title` | texte | oui | longueur min `1` |
| `slug` | texte |  |  |
| `body` | texte | oui | longueur min `1` |
| `category` | enum : `BILLING`, `BOOKING`, `ACCESS`, `TECHNICAL`, `OTHER` |  |  |
| `tags` | texte[] |  |  |
| `publicVisible` | booléen |  |  |
| `internalOnly` | booléen |  |  |
| `active` | booléen |  |  |

<a id="m-KnowledgeDtos-KnowledgeAnalyticsResponse"></a>
#### KnowledgeDtos.KnowledgeAnalyticsResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `from` | date-heure ISO 8601 UTC |  |  |
| `to` | date-heure ISO 8601 UTC |  |  |
| `totalArticles` | entier int64 ⚠ |  |  |
| `activeArticles` | entier int64 ⚠ |  |  |
| `totalViews` | entier int64 ⚠ |  |  |
| `ticketsCreatedInPeriod` | entier int64 ⚠ |  |  |
| `topArticles` | [KnowledgeDtos.ArticleViewEntry](#m-KnowledgeDtos-ArticleViewEntry)[] |  |  |

<a id="m-KnowledgeDtos-ArticleSuggestionResponse"></a>
#### KnowledgeDtos.ArticleSuggestionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `articleCode` | texte |  |  |
| `title` | texte |  |  |
| `slug` | texte |  |  |
| `excerpt` | texte |  |  |

<a id="m-KnowledgeDtos-ArticleViewEntry"></a>
#### KnowledgeDtos.ArticleViewEntry

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `articleCode` | texte |  |  |
| `title` | texte |  |  |
| `viewCount` | entier int64 ⚠ |  |  |
