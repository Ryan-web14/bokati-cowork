# ClientKnowledgeArticleController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [support](../README.md) · 3 route(s).

## Routes

<a id="r-get-sni-api-v1-client-support-knowledge-base"></a>
### GET /sni/api/v1/client/support/knowledge-base

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `PaginatedResponse<KnowledgeDtos.KnowledgeArticleResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [KnowledgeDtos.KnowledgeArticleResponse](#m-KnowledgeDtos-KnowledgeArticleResponse), [TicketCategory](#m-TicketCategory)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/ClientKnowledgeArticleController.java`, méthode `search`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `category` | requête | `TicketCategory` |  |  |
| `searchText` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-get-sni-api-v1-client-support-knowledge-base-suggestions"></a>
### GET /sni/api/v1/client/support/knowledge-base/suggestions

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<KnowledgeDtos.ArticleSuggestionResponse>`
- Modèles : [KnowledgeDtos.ArticleSuggestionResponse](#m-KnowledgeDtos-ArticleSuggestionResponse), [TicketCategory](#m-TicketCategory)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/ClientKnowledgeArticleController.java`, méthode `suggestions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `category` | requête | `TicketCategory` |  |  |
| `searchText` | requête | `String` |  |  |
| `limit` | requête | `int` |  | 5 |

<a id="r-get-sni-api-v1-client-support-knowledge-base-slug"></a>
### GET /sni/api/v1/client/support/knowledge-base/{slug}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `KnowledgeDtos.KnowledgeArticleResponse`
- Modèles : [KnowledgeDtos.KnowledgeArticleResponse](#m-KnowledgeDtos-KnowledgeArticleResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/ClientKnowledgeArticleController.java`, méthode `read`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `slug` | chemin | `String` | oui |  |

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

<a id="m-KnowledgeDtos-ArticleSuggestionResponse"></a>
#### KnowledgeDtos.ArticleSuggestionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `articleCode` | texte |  |  |
| `title` | texte |  |  |
| `slug` | texte |  |  |
| `excerpt` | texte |  |  |
