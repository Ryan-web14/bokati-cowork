# PlatformSettingsController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [parametres](../README.md) · 5 route(s).

## Routes

<a id="r-get-sni-api-v1-admin-settings"></a>
### GET /sni/api/v1/admin/settings

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasRole('SUPER_ADMIN') or hasRole('ADMIN')`
- Réponse : `List<PlatformSettingsAdminService.SettingView>`
- Modèles : [PlatformSettingsAdminService.SettingView](#m-PlatformSettingsAdminService-SettingView), [SettingDomain](#m-SettingDomain)
- Code : `src/main/java/com/sni/bokaticowork/core/settings/platform/PlatformSettingsController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `domain` | requête | `SettingDomain` |  |  |

<a id="r-get-sni-api-v1-admin-settings-domains"></a>
### GET /sni/api/v1/admin/settings/domains

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasRole('SUPER_ADMIN') or hasRole('ADMIN')`
- Réponse : `List<PlatformSettingsAdminService.DomainView>`
- Modèles : [PlatformSettingsAdminService.DomainView](#m-PlatformSettingsAdminService-DomainView)
- Code : `src/main/java/com/sni/bokaticowork/core/settings/platform/PlatformSettingsController.java`, méthode `domains`

<a id="r-delete-sni-api-v1-admin-settings-key"></a>
### DELETE /sni/api/v1/admin/settings/{key}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasRole('SUPER_ADMIN') or hasRole('ADMIN')`
- Réponse : `PlatformSettingsAdminService.SettingView`
- Modèles : [PlatformSettingsAdminService.SettingView](#m-PlatformSettingsAdminService-SettingView)
- Code : `src/main/java/com/sni/bokaticowork/core/settings/platform/PlatformSettingsController.java`, méthode `reset`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `key` | chemin | `String` | oui |  |
| `reason` | requête | `String` |  |  |

<a id="r-put-sni-api-v1-admin-settings-key"></a>
### PUT /sni/api/v1/admin/settings/{key}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasRole('SUPER_ADMIN') or hasRole('ADMIN')`
- Corps JSON (obligatoire) : `PlatformSettingsController.UpdateRequest`
- Réponse : `PlatformSettingsAdminService.SettingView`
- Modèles : [PlatformSettingsAdminService.SettingView](#m-PlatformSettingsAdminService-SettingView), [PlatformSettingsController.UpdateRequest](#m-PlatformSettingsController-UpdateRequest)
- Code : `src/main/java/com/sni/bokaticowork/core/settings/platform/PlatformSettingsController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `key` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-admin-settings-key-history"></a>
### GET /sni/api/v1/admin/settings/{key}/history

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasRole('SUPER_ADMIN') or hasRole('ADMIN')`
- Réponse : `List<PlatformSettingsAdminService.ChangeView>`
- Modèles : [PlatformSettingsAdminService.ChangeView](#m-PlatformSettingsAdminService-ChangeView)
- Code : `src/main/java/com/sni/bokaticowork/core/settings/platform/PlatformSettingsController.java`, méthode `history`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `key` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-PlatformSettingsAdminService-SettingView"></a>
#### PlatformSettingsAdminService.SettingView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `key` | texte |  |  |
| `label` | texte |  |  |
| `domain` | enum : `BOOKING`, `RESOURCES`, `SUBSCRIPTIONS`, `PASSES`, `MARKETPLACE`, `PROMOTIONS`, `DOMICILIATION`, `CONTRACTS`, `SUPPORT`, `TASKS`, `CRM`, `EVENTS`, `PORTAL`, `COMPANIES`, `WALLET`, `PAYMENT`, `BILLING`, `COMPLIANCE`, `DOCUMENTS`, `LOCALE`, `SECURITY`, `NOTIFICATIONS`, `WEBHOOKS`, `OUTBOX`, `GRAPH_API`, `WEBSOCKET`, `ENVIRONMENT` |  |  |
| `type` | enum : `TEXT`, `EMAIL`, `INTEGER`, `DECIMAL`, `BOOLEAN`, `CRON`, `TIME`, `TIME_RANGE`, `ENUM`, `SECRET` |  |  |
| `effect` | enum : `LIVE`, `RESTART` |  |  |
| `value` | texte |  |  |
| `defaultValue` | texte |  |  |
| `modified` | booléen |  |  |
| `editable` | booléen |  |  |
| `configured` | booléen |  |  |
| `restartPending` | booléen |  |  |
| `updatedBy` | texte |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `options` | texte[] |  |  |
| `sensitive` | booléen |  | un changement prend effet aussitôt et alerte les super-administrateurs |
| `risk` | texte |  | ce que risque un changement, à montrer avant d'enregistrer ; `null` si non sensible |

<a id="m-SettingDomain"></a>
#### SettingDomain

Énumération : `BOOKING`, `RESOURCES`, `SUBSCRIPTIONS`, `PASSES`, `MARKETPLACE`, `PROMOTIONS`, `DOMICILIATION`, `CONTRACTS`, `SUPPORT`, `TASKS`, `CRM`, `EVENTS`, `PORTAL`, `COMPANIES`, `WALLET`, `PAYMENT`, `BILLING`, `COMPLIANCE`, `DOCUMENTS`, `LOCALE`, `SECURITY`, `NOTIFICATIONS`, `WEBHOOKS`, `OUTBOX`, `GRAPH_API`, `WEBSOCKET`, `ENVIRONMENT`

<a id="m-PlatformSettingsAdminService-DomainView"></a>
#### PlatformSettingsAdminService.DomainView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `domain` | enum : `BOOKING`, `RESOURCES`, `SUBSCRIPTIONS`, `PASSES`, `MARKETPLACE`, `PROMOTIONS`, `DOMICILIATION`, `CONTRACTS`, `SUPPORT`, `TASKS`, `CRM`, `EVENTS`, `PORTAL`, `COMPANIES`, `WALLET`, `PAYMENT`, `BILLING`, `COMPLIANCE`, `DOCUMENTS`, `LOCALE`, `SECURITY`, `NOTIFICATIONS`, `WEBHOOKS`, `OUTBOX`, `GRAPH_API`, `WEBSOCKET`, `ENVIRONMENT` |  |  |
| `label` | texte |  |  |
| `adminEditable` | booléen |  |  |
| `settings` | entier |  |  |

<a id="m-PlatformSettingsController-UpdateRequest"></a>
#### PlatformSettingsController.UpdateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `value` | texte |  |  |
| `reason` | texte |  | longueur min `0`, longueur max `500` |

<a id="m-PlatformSettingsAdminService-ChangeView"></a>
#### PlatformSettingsAdminService.ChangeView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `oldValue` | texte |  |  |
| `newValue` | texte |  |  |
| `reason` | texte |  |  |
| `changedAt` | date-heure ISO 8601 UTC |  |  |
