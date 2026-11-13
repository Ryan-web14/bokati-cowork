# WhatsAppController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [socle](../README.md) · 9 route(s).

## Routes

<a id="r-get-sni-api-v1-admin-whatsapp-messages"></a>
### GET /sni/api/v1/admin/whatsapp/messages

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('WHATSAPP:MANAGE','WHATSAPP_MANAGE')`
- Réponse : `List<WhatsAppMessage>`
- Modèles : [WhatsAppMessage](#m-WhatsAppMessage)
- Code : `src/main/java/com/sni/bokaticowork/core/communication/whatsapp/WhatsAppController.java`, méthode `messages`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `reference` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-admin-whatsapp-messages-refresh"></a>
### POST /sni/api/v1/admin/whatsapp/messages/refresh

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('WHATSAPP:MANAGE','WHATSAPP_MANAGE')`
- Réponse : `Map<String, Integer>`
- Code : `src/main/java/com/sni/bokaticowork/core/communication/whatsapp/WhatsAppController.java`, méthode `refresh`

<a id="r-get-sni-api-v1-admin-whatsapp-subscription"></a>
### GET /sni/api/v1/admin/whatsapp/subscription

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('WHATSAPP:MANAGE','WHATSAPP_MANAGE')`
- Réponse : `Map<String, Object>`
- Code : `src/main/java/com/sni/bokaticowork/core/communication/whatsapp/WhatsAppController.java`, méthode `subscription`

<a id="r-post-sni-api-v1-admin-whatsapp-subscription"></a>
### POST /sni/api/v1/admin/whatsapp/subscription

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('WHATSAPP:MANAGE','WHATSAPP_MANAGE')`
- Réponse : `Map<String, Object>`
- Code : `src/main/java/com/sni/bokaticowork/core/communication/whatsapp/WhatsAppController.java`, méthode `subscribe`

<a id="r-get-sni-api-v1-admin-whatsapp-templates"></a>
### GET /sni/api/v1/admin/whatsapp/templates

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('WHATSAPP:MANAGE','WHATSAPP_MANAGE')`
- Réponse : `List<WhatsAppTemplate>`
- Modèles : [WhatsAppTemplate](#m-WhatsAppTemplate)
- Code : `src/main/java/com/sni/bokaticowork/core/communication/whatsapp/WhatsAppController.java`, méthode `templates`

<a id="r-post-sni-api-v1-admin-whatsapp-templates"></a>
### POST /sni/api/v1/admin/whatsapp/templates

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('WHATSAPP:MANAGE','WHATSAPP_MANAGE')`
- Corps JSON (obligatoire) : `WhatsAppService.TemplateRequest`
- Réponse : `WhatsAppTemplate`
- Modèles : [WhatsAppTemplate](#m-WhatsAppTemplate), [WhatsAppService.TemplateRequest](#m-WhatsAppService-TemplateRequest)
- Code : `src/main/java/com/sni/bokaticowork/core/communication/whatsapp/WhatsAppController.java`, méthode `create`

<a id="r-post-sni-api-v1-admin-whatsapp-templates-defaults"></a>
### POST /sni/api/v1/admin/whatsapp/templates/defaults

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('WHATSAPP:MANAGE','WHATSAPP_MANAGE')`
- Réponse : `List<WhatsAppTemplate>`
- Modèles : [WhatsAppTemplate](#m-WhatsAppTemplate)
- Code : `src/main/java/com/sni/bokaticowork/core/communication/whatsapp/WhatsAppController.java`, méthode `defaults`

<a id="r-post-sni-api-v1-admin-whatsapp-templates-sync"></a>
### POST /sni/api/v1/admin/whatsapp/templates/sync

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('WHATSAPP:MANAGE','WHATSAPP_MANAGE')`
- Réponse : `List<WhatsAppTemplate>`
- Modèles : [WhatsAppTemplate](#m-WhatsAppTemplate)
- Code : `src/main/java/com/sni/bokaticowork/core/communication/whatsapp/WhatsAppController.java`, méthode `sync`

<a id="r-post-sni-api-v1-admin-whatsapp-test"></a>
### POST /sni/api/v1/admin/whatsapp/test

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('WHATSAPP:MANAGE','WHATSAPP_MANAGE')`
- Corps JSON (obligatoire) : `WhatsAppController.TestRequest`
- Réponse : `WhatsAppMessageSender.Result`
- Modèles : [WhatsAppMessageSender.Result](#m-WhatsAppMessageSender-Result), [WhatsAppController.TestRequest](#m-WhatsAppController-TestRequest)
- Code : `src/main/java/com/sni/bokaticowork/core/communication/whatsapp/WhatsAppController.java`, méthode `test`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-WhatsAppMessage"></a>
#### WhatsAppMessage

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `messageId` | texte |  |  |
| `toPhone` | texte |  |  |
| `kind` | texte |  |  |
| `purpose` | texte |  |  |
| `templateName` | texte |  |  |
| `content` | texte |  |  |
| `reference` | texte |  |  |
| `statusGroup` | texte |  |  |
| `statusName` | texte |  |  |
| `error` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `notificationNumber` | texte |  |  |
| `statusDescription` | texte |  |  |
| `sentAt` | date-heure ISO 8601 UTC |  |  |
| `doneAt` | date-heure ISO 8601 UTC |  |  |
| `seenAt` | date-heure ISO 8601 UTC |  |  |
| `checkedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-WhatsAppTemplate"></a>
#### WhatsAppTemplate

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `purpose` | texte |  |  |
| `name` | texte |  |  |
| `language` | texte |  |  |
| `category` | texte |  |  |
| `bodyText` | texte |  |  |
| `examples` | texte |  |  |
| `placeholders` | entier |  |  |
| `status` | texte |  |  |
| `statusDetail` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `syncedAt` | date-heure ISO 8601 UTC |  |  |
| `footerText` | texte |  |  |
| `buttonType` | texte |  |  |
| `buttonText` | texte |  |  |
| `buttonUrl` | texte |  |  |
| `buttonExample` | texte |  |  |
| `codeExpirationMinutes` | entier |  |  |

<a id="m-WhatsAppService-TemplateRequest"></a>
#### WhatsAppService.TemplateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `purpose` | texte |  |  |
| `name` | texte |  |  |
| `language` | texte |  |  |
| `category` | texte |  |  |
| `text` | texte |  |  |
| `examples` | texte[] |  |  |
| `footer` | texte |  |  |
| `button` | [WhatsAppService.Button](#m-WhatsAppService-Button) |  |  |
| `codeExpirationMinutes` | entier |  |  |

<a id="m-WhatsAppMessageSender-Result"></a>
#### WhatsAppMessageSender.Result

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `messageId` | texte |  |  |
| `kind` | texte |  |  |
| `templateName` | texte |  |  |
| `statusGroup` | texte |  |  |
| `statusName` | texte |  |  |

<a id="m-WhatsAppController-TestRequest"></a>
#### WhatsAppController.TestRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `phone` | texte |  |  |
| `purpose` | texte |  |  |
| `placeholders` | texte[] |  |  |
| `text` | texte |  |  |

<a id="m-WhatsAppService-Button"></a>
#### WhatsAppService.Button

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | texte |  |  |
| `text` | texte |  |  |
| `url` | texte |  |  |
| `example` | texte |  |  |
