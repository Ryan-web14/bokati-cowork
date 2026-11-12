# SupportQuickReplyController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [support](../README.md) · 3 route(s).

## Routes

<a id="r-get-sni-api-v1-support-quick-replies"></a>
### GET /sni/api/v1/support/quick-replies

- Accès : back-office : rôle d'administration et droit `SUPPORT:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')`
- Réponse : `List<SupportDtos.QuickReplyResponse>`
- Modèles : [SupportDtos.QuickReplyResponse](#m-SupportDtos-QuickReplyResponse), [TicketCategory](#m-TicketCategory)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportQuickReplyController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `category` | requête | `TicketCategory` |  |  |

<a id="r-post-sni-api-v1-support-quick-replies"></a>
### POST /sni/api/v1/support/quick-replies

- Accès : back-office : rôle d'administration et droit `SUPPORT:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')`
- Corps JSON (obligatoire) : `SupportDtos.QuickReplyRequest`
- Réponse : `SupportDtos.QuickReplyResponse`
- Modèles : [SupportDtos.QuickReplyResponse](#m-SupportDtos-QuickReplyResponse), [SupportDtos.QuickReplyRequest](#m-SupportDtos-QuickReplyRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportQuickReplyController.java`, méthode `create`

<a id="r-delete-sni-api-v1-support-quick-replies-id"></a>
### DELETE /sni/api/v1/support/quick-replies/{id}

- Accès : back-office : rôle d'administration et droit `SUPPORT:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportQuickReplyController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-SupportDtos-QuickReplyResponse"></a>
#### SupportDtos.QuickReplyResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `category` | enum : `BILLING`, `BOOKING`, `ACCESS`, `TECHNICAL`, `OTHER` |  |  |
| `title` | texte |  |  |
| `body` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-TicketCategory"></a>
#### TicketCategory

Énumération : `BILLING`, `BOOKING`, `ACCESS`, `TECHNICAL`, `OTHER`

<a id="m-SupportDtos-QuickReplyRequest"></a>
#### SupportDtos.QuickReplyRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `category` | enum : `BILLING`, `BOOKING`, `ACCESS`, `TECHNICAL`, `OTHER` | oui |  |
| `title` | texte | oui | longueur min `1` |
| `body` | texte | oui | longueur min `1` |
