# parametres : routes

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

9 routes. Le fonctionnement et les parcours sont dans [README.md](README.md).

| Verbe | Chemin | Accès | Détail |
| --- | --- | --- | --- |
| GET | `/sni/api/v1/members/{memberId}/settings` | CLIENT:READ | [MemberSettingsController](routes/MemberSettingsController.md#r-get-sni-api-v1-members-memberid-settings) |
| PATCH | `/sni/api/v1/members/{memberId}/settings` | CLIENT:UPDATE | [MemberSettingsController](routes/MemberSettingsController.md#r-patch-sni-api-v1-members-memberid-settings) |
| GET | `/sni/api/v1/members/{memberId}/settings/notifications` | CLIENT:READ | [MemberSettingsController](routes/MemberSettingsController.md#r-get-sni-api-v1-members-memberid-settings-notifications) |
| PATCH | `/sni/api/v1/members/{memberId}/settings/notifications` | CLIENT:UPDATE | [MemberSettingsController](routes/MemberSettingsController.md#r-patch-sni-api-v1-members-memberid-settings-notifications) |
| GET | `/sni/api/v1/admin/settings` | SYSTEM:SETTINGS | [PlatformSettingsController](routes/PlatformSettingsController.md#r-get-sni-api-v1-admin-settings) |
| GET | `/sni/api/v1/admin/settings/domains` | SYSTEM:SETTINGS | [PlatformSettingsController](routes/PlatformSettingsController.md#r-get-sni-api-v1-admin-settings-domains) |
| DELETE | `/sni/api/v1/admin/settings/{key}` | SYSTEM:SETTINGS | [PlatformSettingsController](routes/PlatformSettingsController.md#r-delete-sni-api-v1-admin-settings-key) |
| PUT | `/sni/api/v1/admin/settings/{key}` | SYSTEM:SETTINGS | [PlatformSettingsController](routes/PlatformSettingsController.md#r-put-sni-api-v1-admin-settings-key) |
| GET | `/sni/api/v1/admin/settings/{key}/history` | SYSTEM:SETTINGS | [PlatformSettingsController](routes/PlatformSettingsController.md#r-get-sni-api-v1-admin-settings-key-history) |
